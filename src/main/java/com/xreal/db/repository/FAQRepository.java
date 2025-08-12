package com.xreal.db.repository;

import com.xreal.db.entity.FAQ;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface FAQRepository extends JpaRepository<FAQ, Long> {
    
    Page<FAQ> findByActive(Boolean active, Pageable pageable);
    
    @Query("SELECT DISTINCT f FROM FAQ f JOIN f.faqTags ft WHERE ft.tag.name IN :tags")
    Page<FAQ> findByTagNamesIn(@Param("tags") Set<String> tags, Pageable pageable);
    
    @Query("SELECT DISTINCT f FROM FAQ f JOIN f.faqTags ft WHERE ft.tag.name IN :tags AND f.active = :active")
    Page<FAQ> findByTagNamesInAndActive(@Param("tags") Set<String> tags, @Param("active") Boolean active, Pageable pageable);
}