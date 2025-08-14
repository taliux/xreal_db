package com.xreal.db.service;

import com.xreal.db.document.FaqDocument;
import com.xreal.db.dto.ExcelUploadResponse;
import com.xreal.db.entity.FAQ;
import com.xreal.db.entity.Tag;
import com.xreal.db.repository.FAQRepository;
import com.xreal.db.repository.FaqDocumentRepository;
import com.xreal.db.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelUploadService {
    
    private final FAQRepository faqRepository;
    private final TagRepository tagRepository;
    private final FaqService faqService;
    private final TagService tagService;
    @Autowired(required = false)
    private FaqDocumentRepository faqDocumentRepository;
    @Autowired(required = false)
    private EmbeddingService embeddingService;
    @Autowired(required = false)
    private AsyncElasticsearchService asyncElasticsearchService;
    
    private final List<FAQ> faqsToSync = new ArrayList<>();
    
    @Transactional
    public ExcelUploadResponse processExcelFile(MultipartFile file) throws Exception {
        long startTime = System.currentTimeMillis();
        
        ExcelUploadResponse.ExcelUploadResponseBuilder responseBuilder = ExcelUploadResponse.builder();
        List<String> unrecognizedTags = new ArrayList<>();
        
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            // Process tag sheet first
            Sheet tagSheet = workbook.getSheet("tag");
            if (tagSheet == null) {
                throw new IllegalArgumentException("'tag' sheet not found in Excel file");
            }
            
            Map<String, Tag> validTags = processTagSheet(tagSheet, responseBuilder);
            
            // Process xreal_tech_faq sheet
            Sheet faqSheet = workbook.getSheet("xreal_tech_faq");
            if (faqSheet == null) {
                throw new IllegalArgumentException("'xreal_tech_faq' sheet not found in Excel file");
            }
            
            processFaqSheet(faqSheet, validTags, unrecognizedTags, responseBuilder);
            
        } catch (Exception e) {
            log.error("Error processing Excel file", e);
            throw e;
        }
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        return responseBuilder
                .unrecognizedTags(unrecognizedTags)
                .processingTimeMs(processingTime)
                .message("Excel file processed successfully")
                .build();
    }
    
    private Map<String, Tag> processTagSheet(Sheet sheet, ExcelUploadResponse.ExcelUploadResponseBuilder responseBuilder) {
        Map<String, Tag> tagMap = new HashMap<>();
        int totalTags = 0;
        int importedTags = 0;
        int updatedTags = 0;
        
        // Get header row to identify column positions
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new IllegalArgumentException("Tag sheet missing header row");
        }
        
        int nameCol = -1;
        int descriptionCol = -1;
        int activeCol = -1;
        
        for (Cell cell : headerRow) {
            String cellValue = getCellStringValue(cell).toLowerCase().trim();
            if (cellValue.equals("name")) {
                nameCol = cell.getColumnIndex();
            } else if (cellValue.equals("description")) {
                descriptionCol = cell.getColumnIndex();
            } else if (cellValue.equals("active")) {
                activeCol = cell.getColumnIndex();
            }
        }
        
        if (nameCol == -1) {
            throw new IllegalArgumentException("Tag sheet missing 'Name' column");
        }
        
        // Process data rows
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            String tagName = getCellStringValue(row.getCell(nameCol)).trim();
            if (tagName.isEmpty()) continue;
            
            totalTags++;
            
            String description = descriptionCol != -1 ? 
                getCellStringValue(row.getCell(descriptionCol)).trim() : "";
            
            // Parse active field (0/1 or boolean)
            boolean active = true;
            if (activeCol != -1) {
                String activeValue = getCellStringValue(row.getCell(activeCol)).trim();
                active = "1".equals(activeValue) || "true".equalsIgnoreCase(activeValue) || "yes".equalsIgnoreCase(activeValue);
            }
            
            Tag existingTag = tagRepository.findById(tagName).orElse(null);
            
            if (existingTag == null) {
                Tag newTag = new Tag();
                newTag.setName(tagName);
                newTag.setDescription(description.isEmpty() ? null : description);
                newTag.setActive(active);
                tagRepository.save(newTag);
                if (active) {
                    tagMap.put(tagName, newTag);
                }
                importedTags++;
                log.info("Imported new tag: {} (active: {})", tagName, active);
            } else {
                boolean needUpdate = false;
                if (!description.isEmpty() && !description.equals(existingTag.getDescription())) {
                    existingTag.setDescription(description);
                    needUpdate = true;
                }
                if (existingTag.getActive() != active) {
                    existingTag.setActive(active);
                    needUpdate = true;
                }
                if (needUpdate) {
                    tagRepository.save(existingTag);
                    updatedTags++;
                    log.info("Updated tag: {} (active: {})", tagName, active);
                }
                if (active) {
                    tagMap.put(tagName, existingTag);
                }
            }
        }
        
        responseBuilder
                .totalTagsProcessed(totalTags)
                .tagsImported(importedTags)
                .tagsUpdated(updatedTags);
        
        return tagMap;
    }
    
    private void processFaqSheet(Sheet sheet, Map<String, Tag> validTags, 
                                 List<String> unrecognizedTags,
                                 ExcelUploadResponse.ExcelUploadResponseBuilder responseBuilder) {
        
        int totalFaqs = 0;
        int importedFaqs = 0;
        int updatedFaqs = 0;
        int skippedFaqs = 0;
        
        // Get header row to identify column positions
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new IllegalArgumentException("FAQ sheet missing header row");
        }
        
        Map<String, Integer> columnMap = new HashMap<>();
        for (Cell cell : headerRow) {
            String cellValue = getCellStringValue(cell).trim();
            columnMap.put(cellValue, cell.getColumnIndex());
        }
        
        // Required columns
        Integer questionCol = columnMap.get("Question");
        Integer tagsCol = columnMap.get("Tags");
        Integer commentCol = columnMap.get("Comment");
        Integer instructionCol = columnMap.get("Instruction");
        Integer answerCol = columnMap.get("Answer");
        Integer urlCol = columnMap.get("Url");
        
        if (questionCol == null || answerCol == null) {
            throw new IllegalArgumentException("FAQ sheet must contain 'Question' and 'Answer' columns");
        }
        
        // Process data rows
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            String question = getCellStringValue(row.getCell(questionCol)).trim();
            String answer = getCellStringValue(row.getCell(answerCol)).trim();
            
            if (question.isEmpty() || answer.isEmpty()) {
                skippedFaqs++;
                continue;
            }
            
            totalFaqs++;
            
            // Check if FAQ already exists (case-insensitive)
            FAQ existingFaq = faqRepository.findByQuestionIgnoreCase(question).orElse(null);
            FAQ faq = existingFaq != null ? existingFaq : new FAQ();
            
            faq.setQuestion(question);
            faq.setAnswer(answer);
            
            if (commentCol != null) {
                faq.setComment(getCellStringValue(row.getCell(commentCol)).trim());
            }
            
            if (instructionCol != null) {
                faq.setInstruction(getCellStringValue(row.getCell(instructionCol)).trim());
            }
            
            if (urlCol != null) {
                faq.setUrl(getCellStringValue(row.getCell(urlCol)).trim());
            }
            
            faq.setActive(true);
            
            // Process tags
            if (tagsCol != null) {
                String tagsString = getCellStringValue(row.getCell(tagsCol)).trim();
                if (!tagsString.isEmpty()) {
                    Set<String> tagNames = Arrays.stream(tagsString.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toSet());
                    
                    faq.clearTags();
                    
                    for (String tagName : tagNames) {
                        if (validTags.containsKey(tagName)) {
                            faq.addTag(validTags.get(tagName));
                        } else {
                            if (!unrecognizedTags.contains(tagName)) {
                                unrecognizedTags.add(tagName);
                                log.warn("Unrecognized tag: {}", tagName);
                            }
                        }
                    }
                }
            }
            
            faq = faqRepository.save(faq);
            
            // Collect FAQs for batch ES sync
            faqsToSync.add(faq);
            
            if (existingFaq != null) {
                updatedFaqs++;
                log.info("Updated FAQ: {}", question);
            } else {
                importedFaqs++;
                log.info("Imported new FAQ: {}", question);
            }
        }
        
        responseBuilder
                .totalFaqsProcessed(totalFaqs)
                .faqsImported(importedFaqs)
                .faqsUpdated(updatedFaqs)
                .faqsSkipped(skippedFaqs);
        
        // Sync to Elasticsearch - use async for large datasets
        int syncSize = faqsToSync.size();
        if (syncSize > 50 && asyncElasticsearchService != null) {
            // For large datasets, use async processing
            asyncElasticsearchService.syncFaqsToElasticsearch(new ArrayList<>(faqsToSync));
            log.info("Started async Elasticsearch sync for {} FAQs", syncSize);
            faqsToSync.clear();
        } else if (syncSize > 0) {
            // For smaller datasets, use sync processing but with timeout protection
            try {
                syncAllToElasticsearchWithTimeout();
            } catch (Exception e) {
                log.warn("Elasticsearch sync failed or timed out, but data was saved to database: {}", e.getMessage());
            }
        }
    }
    
    private void syncAllToElasticsearchWithTimeout() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Void> future = executor.submit(() -> {
            syncAllToElasticsearch();
            return null;
        });
        
        try {
            // Wait maximum 30 seconds for ES sync
            future.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("Elasticsearch sync timed out after 30 seconds");
        } catch (Exception e) {
            log.warn("Error during Elasticsearch sync: {}", e.getMessage());
        } finally {
            executor.shutdown();
            faqsToSync.clear();
        }
    }
    
    private void syncAllToElasticsearch() {
        if (faqDocumentRepository == null || embeddingService == null) {
            log.debug("Elasticsearch or EmbeddingService not available, skipping sync");
            faqsToSync.clear();
            return;
        }
        
        if (faqsToSync.isEmpty()) {
            return;
        }
        
        log.info("Starting batch sync of {} FAQs to Elasticsearch", faqsToSync.size());
        
        // Process in batches to avoid timeout
        List<FaqDocument> documents = new ArrayList<>();
        int batchSize = 10;
        
        try {
            for (int i = 0; i < faqsToSync.size(); i += batchSize) {
                int end = Math.min(i + batchSize, faqsToSync.size());
                List<FAQ> batch = faqsToSync.subList(i, end);
                
                for (FAQ faq : batch) {
                    try {
                        String content = generateContent(faq);
                        float[] embedding = embeddingService.generateEmbedding(content);
                        
                        FaqDocument document = FaqDocument.builder()
                            .id(faq.getId().toString())
                            .content(content)
                            .embedding(embedding)
                            .metadata(FaqDocument.FaqMetadata.builder()
                                .question(faq.getQuestion())
                                .answer(faq.getAnswer())
                                .tags(new ArrayList<>(faq.getTagNames()))
                                .instruction(faq.getInstruction())
                                .url(faq.getUrl())
                                .active(faq.getActive())
                                .timestamp(faq.getTimestamp())
                                .build())
                            .build();
                        
                        documents.add(document);
                    } catch (Exception e) {
                        log.warn("Failed to prepare FAQ {} for Elasticsearch: {}", faq.getId(), e.getMessage());
                    }
                }
                
                // Save batch to Elasticsearch
                if (!documents.isEmpty()) {
                    try {
                        faqDocumentRepository.saveAll(documents);
                        log.debug("Synced batch of {} FAQs to Elasticsearch", documents.size());
                        documents.clear();
                    } catch (Exception e) {
                        log.warn("Failed to sync batch to Elasticsearch: {}", e.getMessage());
                    }
                }
            }
        } finally {
            faqsToSync.clear();
        }
        
        log.info("Completed Elasticsearch sync");
    }
    
    private String generateContent(FAQ faq) {
        StringBuilder content = new StringBuilder();
        content.append("Question: ").append(faq.getQuestion()).append("\n");
        content.append("Answer: ").append(faq.getAnswer()).append("\n");
        
        if (faq.getInstruction() != null && !faq.getInstruction().isEmpty()) {
            content.append("Instruction: ").append(faq.getInstruction()).append("\n");
        }
        
        if (!faq.getTagNames().isEmpty()) {
            content.append("Tags: ").append(String.join(", ", faq.getTagNames())).append("\n");
        }
        
        return content.toString();
    }
    
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}