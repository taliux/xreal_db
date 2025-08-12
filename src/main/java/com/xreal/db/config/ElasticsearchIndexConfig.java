package com.xreal.db.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexConfig {
    
    private final ElasticsearchClient elasticsearchClient;
    
    @Value("${spring.ai.vectorstore.elasticsearch.dimensions}")
    private int dimensions;
    
    @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
    private String indexName;
    
    @PostConstruct
    public void createIndexIfNotExists() {
        try {
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(indexName));
            boolean exists = elasticsearchClient.indices().exists(existsRequest).value();
            
            if (!exists) {
                log.info("Creating index '{}' with dimensions: {}", indexName, dimensions);
                
                CreateIndexRequest createIndexRequest = CreateIndexRequest.of(c -> c
                    .index(indexName)
                    .mappings(m -> m
                        .properties("id", p -> p.keyword(k -> k))
                        .properties("content", p -> p.text(t -> t))
                        .properties("embedding", p -> p.denseVector(d -> d
                            .dims(dimensions)
                            .index(true)
                            .similarity(DenseVectorSimilarity.Cosine)
                        ))
                        .properties("metadata", p -> p.object(o -> o
                            .properties("question", mp -> mp.text(t -> t))
                            .properties("answer", mp -> mp.text(t -> t))
                            .properties("tags", mp -> mp.keyword(k -> k))
                            .properties("instruction", mp -> mp.text(t -> t))
                            .properties("url", mp -> mp.text(t -> t))
                            .properties("active", mp -> mp.boolean_(b -> b))
                            .properties("timestamp", mp -> mp.date(d -> d))
                        ))
                    )
                );
                
                elasticsearchClient.indices().create(createIndexRequest);
                log.info("Index '{}' created successfully", indexName);
            } else {
                log.info("Index '{}' already exists", indexName);
            }
        } catch (Exception e) {
            log.error("Failed to create index '{}': {}", indexName, e.getMessage(), e);
        }
    }
}