package com.xreal.db.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchDimensionsConfig {
    
    @Value("${spring.ai.vectorstore.elasticsearch.dimensions}")
    private int dimensions;
    
    public int getDimensions() {
        return dimensions;
    }
}