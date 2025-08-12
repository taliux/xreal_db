package com.xreal.db.repository;

import com.xreal.db.entity.FaqTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaqTagRepository extends JpaRepository<FaqTag, FaqTag.FaqTagId> {
    
    void deleteByFaqId(Long faqId);
    
    boolean existsByTagName(String tagName);
}