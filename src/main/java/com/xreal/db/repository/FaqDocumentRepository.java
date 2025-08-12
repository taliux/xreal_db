package com.xreal.db.repository;

import com.xreal.db.document.FaqDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqDocumentRepository extends ElasticsearchRepository<FaqDocument, String> {
    
    @Query("{\"bool\": {\"must\": [{\"terms\": {\"metadata.tags\": ?0}}]}}")
    Page<FaqDocument> findByTags(List<String> tags, Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"terms\": {\"metadata.tags\": ?0}}, {\"term\": {\"metadata.active\": ?1}}]}}")
    Page<FaqDocument> findByTagsAndActive(List<String> tags, Boolean active, Pageable pageable);
    
    Page<FaqDocument> findByMetadataActive(Boolean active, Pageable pageable);
}