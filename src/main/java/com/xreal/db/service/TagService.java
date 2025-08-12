package com.xreal.db.service;

import com.xreal.db.dto.TagRequest;
import com.xreal.db.dto.TagResponse;
import com.xreal.db.entity.Tag;
import com.xreal.db.exception.ResourceNotFoundException;
import com.xreal.db.repository.FaqTagRepository;
import com.xreal.db.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagService {
    
    private final TagRepository tagRepository;
    private final FaqTagRepository faqTagRepository;
    
    @Transactional
    public TagResponse createTag(TagRequest request) {
        log.info("Creating new tag: {}", request.getName());
        
        if (tagRepository.existsById(request.getName())) {
            throw new IllegalArgumentException("Tag already exists: " + request.getName());
        }
        
        Tag tag = new Tag();
        tag.setName(request.getName());
        tag.setDescription(request.getDescription());
        tag.setActive(request.getActive());
        
        Tag savedTag = tagRepository.save(tag);
        return toResponse(savedTag);
    }
    
    @Transactional
    public TagResponse updateTag(String name, TagRequest request) {
        log.info("Updating tag: {}", name);
        
        Tag tag = tagRepository.findById(name)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + name));
        
        if (!name.equals(request.getName())) {
            throw new IllegalArgumentException("Tag name cannot be changed");
        }
        
        tag.setDescription(request.getDescription());
        tag.setActive(request.getActive());
        
        Tag savedTag = tagRepository.save(tag);
        return toResponse(savedTag);
    }
    
    @Transactional
    public void deleteTag(String name) {
        log.info("Deleting tag: {}", name);
        
        if (!tagRepository.existsById(name)) {
            throw new ResourceNotFoundException("Tag not found: " + name);
        }
        
        if (faqTagRepository.existsByTagName(name)) {
            throw new IllegalArgumentException("Cannot delete tag that is associated with FAQs: " + name);
        }
        
        tagRepository.deleteById(name);
    }
    
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<TagResponse> getActiveTags() {
        return tagRepository.findByActive(true).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    private TagResponse toResponse(Tag tag) {
        return TagResponse.builder()
            .name(tag.getName())
            .description(tag.getDescription())
            .active(tag.getActive())
            .build();
    }
}