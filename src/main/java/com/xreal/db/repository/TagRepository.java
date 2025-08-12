package com.xreal.db.repository;

import com.xreal.db.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, String> {
    
    List<Tag> findByActive(Boolean active);
    
    boolean existsByNameAndActive(String name, Boolean active);
}