package com.xreal.db.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tag")
@Data
@EqualsAndHashCode(exclude = {"faqTags"})
@ToString(exclude = {"faqTags"})
public class Tag {
    
    @Id
    @Column(length = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FaqTag> faqTags = new HashSet<>();
}