package com.xreal.db.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaqRequest {
    
    @NotBlank(message = "Question is required")
    private String question;
    
    @NotBlank(message = "Answer is required")
    private String answer;
    
    private String instruction;
    
    private String url;
    
    @NotNull(message = "Active status is required")
    private Boolean active = true;
    
    private String comment;
    
    private Set<String> tags;
}