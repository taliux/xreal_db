package com.xreal.db.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelUploadResponse {
    
    private Integer totalFaqsProcessed;
    
    private Integer faqsImported;
    
    private Integer faqsUpdated;
    
    private Integer faqsSkipped;
    
    private Integer totalTagsProcessed;
    
    private Integer tagsImported;
    
    private Integer tagsUpdated;
    
    private List<String> unrecognizedTags;
    
    private String message;
    
    private Long processingTimeMs;
}