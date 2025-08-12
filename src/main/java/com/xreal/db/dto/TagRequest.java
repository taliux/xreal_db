package com.xreal.db.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagRequest {
    
    @NotBlank(message = "Tag name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Active status is required")
    private Boolean active = true;
}