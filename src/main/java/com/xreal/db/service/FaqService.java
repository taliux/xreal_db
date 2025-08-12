package com.xreal.db.service;

import com.xreal.db.document.FaqDocument;
import com.xreal.db.dto.FaqRequest;
import com.xreal.db.dto.FaqResponse;
import com.xreal.db.entity.FAQ;
import com.xreal.db.entity.Tag;
import com.xreal.db.exception.ResourceNotFoundException;
import com.xreal.db.exception.DataSyncException;
import com.xreal.db.repository.FAQRepository;
import com.xreal.db.repository.FaqDocumentRepository;
import com.xreal.db.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaqService {
    
    private final FAQRepository faqRepository;
    @Autowired(required = false)
    private FaqDocumentRepository faqDocumentRepository;
    private final TagRepository tagRepository;
    @Autowired(required = false)
    private EmbeddingService embeddingService;
    
    @Transactional
    public void deleteAll() {
        log.info("Deleting all FAQ records");
        faqRepository.deleteAll();
        if (faqDocumentRepository != null) {
            try {
                faqDocumentRepository.deleteAll();
            } catch (Exception e) {
                log.warn("Failed to delete from Elasticsearch: {}", e.getMessage());
            }
        }
    }
    
    @Transactional
    public FaqResponse createFaq(FaqRequest request) {
        log.info("Creating new FAQ: {}", request.getQuestion());
        
        FAQ faq = new FAQ();
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setInstruction(request.getInstruction());
        faq.setUrl(request.getUrl());
        faq.setActive(request.getActive());
        faq.setComment(request.getComment());
        
        if (request.getTags() != null) {
            for (String tagName : request.getTags()) {
                Tag tag = tagRepository.findById(tagName)
                    .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagName));
                if (!tag.getActive()) {
                    throw new IllegalArgumentException("Tag is not active: " + tagName);
                }
                faq.addTag(tag);
            }
        }
        
        FAQ savedFaq = faqRepository.save(faq);
        
        try {
            syncToElasticsearch(savedFaq);
        } catch (Exception e) {
            log.error("Failed to sync to Elasticsearch, but continuing", e);
            // 不再抛出异常，允许操作继续
        }
        
        return toResponse(savedFaq);
    }
    
    @Transactional
    public FaqResponse updateFaq(Long id, FaqRequest request) {
        log.info("Updating FAQ with id: {}", id);
        
        FAQ faq = faqRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FAQ not found with id: " + id));
        
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setInstruction(request.getInstruction());
        faq.setUrl(request.getUrl());
        faq.setActive(request.getActive());
        faq.setComment(request.getComment());
        
        faq.clearTags();
        if (request.getTags() != null) {
            for (String tagName : request.getTags()) {
                Tag tag = tagRepository.findById(tagName)
                    .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagName));
                if (!tag.getActive()) {
                    throw new IllegalArgumentException("Tag is not active: " + tagName);
                }
                faq.addTag(tag);
            }
        }
        
        FAQ savedFaq = faqRepository.save(faq);
        
        try {
            syncToElasticsearch(savedFaq);
        } catch (Exception e) {
            log.error("Failed to sync to Elasticsearch, but continuing", e);
            // 不再抛出异常，允许操作继续
        }
        
        return toResponse(savedFaq);
    }
    
    @Transactional
    public void deleteFaq(Long id) {
        log.info("Deleting FAQ with id: {}", id);
        
        if (!faqRepository.existsById(id)) {
            throw new ResourceNotFoundException("FAQ not found with id: " + id);
        }
        
        faqRepository.deleteById(id);
        
        if (faqDocumentRepository != null) {
            try {
                faqDocumentRepository.deleteById(id.toString());
            } catch (Exception e) {
                log.error("Failed to delete from Elasticsearch, continuing", e);
            }
        }
    }
    
    @Transactional(readOnly = true)
    public FaqResponse getFaq(Long id) {
        FAQ faq = faqRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FAQ not found with id: " + id));
        return toResponse(faq);
    }
    
    @Transactional(readOnly = true)
    public Page<FaqResponse> getAllFaqs(Boolean active, Pageable pageable) {
        Page<FAQ> faqs;
        if (active != null) {
            faqs = faqRepository.findByActive(active, pageable);
        } else {
            faqs = faqRepository.findAll(pageable);
        }
        return faqs.map(this::toResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<FaqResponse> searchByTags(Set<String> tags, Boolean active, Pageable pageable) {
        Page<FAQ> faqs;
        if (active != null) {
            faqs = faqRepository.findByTagNamesInAndActive(tags, active, pageable);
        } else {
            faqs = faqRepository.findByTagNamesIn(tags, pageable);
        }
        return faqs.map(this::toResponse);
    }
    
    private void syncToElasticsearch(FAQ faq) {
        if (faqDocumentRepository == null || embeddingService == null) {
            log.warn("Elasticsearch or EmbeddingService not available, skipping sync");
            return;
        }
        
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
            
            faqDocumentRepository.save(document);
        } catch (Exception e) {
            log.error("Failed to sync to Elasticsearch: {}", e.getMessage());
        }
    }
    
    private String generateContent(FAQ faq) {
        StringBuilder content = new StringBuilder();
        content.append(faq.getQuestion());
        
        if (!faq.getTagNames().isEmpty()) {
            content.append(" [Applicable to ");
            content.append(String.join(", ", faq.getTagNames()));
            content.append("]");
        }
        
        return content.toString();
    }
    
    private FaqResponse toResponse(FAQ faq) {
        return FaqResponse.builder()
            .id(faq.getId())
            .question(faq.getQuestion())
            .answer(faq.getAnswer())
            .instruction(faq.getInstruction())
            .url(faq.getUrl())
            .active(faq.getActive())
            .comment(faq.getComment())
            .timestamp(faq.getTimestamp())
            .tags(faq.getTagNames())
            .build();
    }
}