package com.xreal.db.config;

import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import java.util.Collections;

public class CustomElasticsearchConversions extends ElasticsearchCustomConversions {
    
    private final int dimensions;
    
    public CustomElasticsearchConversions(int dimensions) {
        super(Collections.emptyList());
        this.dimensions = dimensions;
    }
    
    public int getDimensions() {
        return dimensions;
    }
}