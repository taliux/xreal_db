package com.xreal.db.service;

import com.xreal.db.document.FaqDocument;
import com.xreal.db.entity.FAQ;
import com.xreal.db.repository.FaqDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncElasticsearchService {
    
    @Autowired(required = false)
    private FaqDocumentRepository faqDocumentRepository;
    
    @Autowired(required = false)
    private EmbeddingService embeddingService;
    
    @Async("elasticsearchSyncExecutor")
    public CompletableFuture<Void> syncFaqsToElasticsearch(List<FAQ> faqs) {
        if (faqDocumentRepository == null || embeddingService == null) {
            log.debug("Elasticsearch or EmbeddingService not available, skipping async sync");
            return CompletableFuture.completedFuture(null);
        }
        
        log.info("Starting async sync of {} FAQs to Elasticsearch", faqs.size());
        
        try {
            List<FaqDocument> documents = new ArrayList<>();
            
            for (FAQ faq : faqs) {
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
                    
                    // Save in batches of 10
                    if (documents.size() >= 10) {
                        faqDocumentRepository.saveAll(documents);
                        log.debug("Async synced batch of {} FAQs to Elasticsearch", documents.size());
                        documents.clear();
                    }
                } catch (Exception e) {
                    log.warn("Failed to process FAQ {} for async Elasticsearch sync: {}", faq.getId(), e.getMessage());
                }
            }
            
            // Save remaining documents
            if (!documents.isEmpty()) {
                faqDocumentRepository.saveAll(documents);
                log.debug("Async synced final batch of {} FAQs to Elasticsearch", documents.size());
            }
            
            log.info("Completed async Elasticsearch sync of {} FAQs", faqs.size());
        } catch (Exception e) {
            log.error("Error during async Elasticsearch sync", e);
        }
        
        return CompletableFuture.completedFuture(null);
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
}