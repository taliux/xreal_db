package com.xreal.db.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {
    
    private final EmbeddingModel embeddingModel;
    
    @Value("${spring.ai.vectorstore.elasticsearch.dimensions}")
    private int dimensions;
    
    public float[] generateEmbedding(String text) {
        try {
            // 创建嵌入请求 - 使用构造函数而不是 builder
            EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
            
            // 调用模型获取响应
            EmbeddingResponse response = embeddingModel.call(request);
            
            // 提取嵌入向量
            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                float[] embedding = response.getResults().get(0).getOutput();
                return embedding;
            }
            
            log.warn("No embedding generated for text: {}", text);
            return new float[dimensions];
        } catch (Exception e) {
            log.error("Failed to generate embedding for text: {}", text, e);
            return new float[dimensions];
        }
    }
}