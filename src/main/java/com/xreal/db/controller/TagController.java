package com.xreal.db.controller;

import com.xreal.db.dto.TagRequest;
import com.xreal.db.dto.TagResponse;
import com.xreal.db.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {
    
    private final TagService tagService;
    
    @PostMapping
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody TagRequest request) {
        TagResponse response = tagService.createTag(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{name}")
    public ResponseEntity<TagResponse> updateTag(
            @PathVariable String name,
            @Valid @RequestBody TagRequest request) {
        TagResponse response = tagService.updateTag(name, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteTag(@PathVariable String name) {
        tagService.deleteTag(name);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping
    public ResponseEntity<List<TagResponse>> getAllTags() {
        List<TagResponse> response = tagService.getAllTags();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<TagResponse>> getActiveTags() {
        List<TagResponse> response = tagService.getActiveTags();
        return ResponseEntity.ok(response);
    }
}