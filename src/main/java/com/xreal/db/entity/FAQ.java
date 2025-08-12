package com.xreal.db.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "faq")
@Data
@EqualsAndHashCode(exclude = {"faqTags"})
@ToString(exclude = {"faqTags"})
public class FAQ {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;
    
    @Column(columnDefinition = "TEXT")
    private String instruction;
    
    @Column(length = 500)
    private String url;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    @UpdateTimestamp
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @OneToMany(mappedBy = "faq", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<FaqTag> faqTags = new HashSet<>();
    
    public void addTag(Tag tag) {
        FaqTag faqTag = new FaqTag();
        faqTag.setFaq(this);
        faqTag.setTag(tag);
        this.faqTags.add(faqTag);
    }
    
    public void removeTag(Tag tag) {
        this.faqTags.removeIf(ft -> ft.getTag().equals(tag));
    }
    
    public void clearTags() {
        this.faqTags.clear();
    }
    
    public Set<String> getTagNames() {
        Set<String> tagNames = new HashSet<>();
        for (FaqTag faqTag : faqTags) {
            tagNames.add(faqTag.getTag().getName());
        }
        return tagNames;
    }
}