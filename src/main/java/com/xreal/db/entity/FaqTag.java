package com.xreal.db.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Entity
@Table(name = "faq_tag")
@Data
@IdClass(FaqTag.FaqTagId.class)
public class FaqTag {
    
    @Id
    @ManyToOne
    @JoinColumn(name = "faq_id")
    private FAQ faq;
    
    @Id
    @ManyToOne
    @JoinColumn(name = "tag")
    private Tag tag;
    
    @Data
    @EqualsAndHashCode
    public static class FaqTagId implements Serializable {
        private Long faq;
        private String tag;
    }
}