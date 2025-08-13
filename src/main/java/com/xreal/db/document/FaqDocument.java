package com.xreal.db.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(indexName = "#{@environment.getProperty('spring.ai.vectorstore.elasticsearch.index-name')}", createIndex = false)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqDocument {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Text)
    private String content;
    
    // 注意: 维度在ElasticsearchIndexConfig中动态配置
    @Field(type = FieldType.Dense_Vector)
    private float[] embedding;
    
    @Field(type = FieldType.Object)
    private FaqMetadata metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaqMetadata {
        
        @Field(type = FieldType.Text)
        private String question;
        
        @Field(type = FieldType.Text)
        private String answer;
        
        @Field(type = FieldType.Keyword)
        private List<String> tags;
        
        @Field(type = FieldType.Text)
        private String instruction;
        
        @Field(type = FieldType.Text)
        private String url;
        
        @Field(type = FieldType.Boolean)
        private Boolean active;
        
        @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
        private LocalDateTime timestamp;
    }
}