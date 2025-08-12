package com.xreal.db.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqResponse {
    
    private Long id;
    private String question;
    private String answer;
    private String instruction;
    private String url;
    private Boolean active;
    private String comment;
    private LocalDateTime timestamp;
    private Set<String> tags;
}