package com.lizhenqi.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_preference")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "preferred_brands", length = 500)
    private String preferredBrands;

    @Column(name = "preferred_categories", length = 500)
    private String preferredCategories;

    @Column(name = "preferred_notes", length = 500)
    private String preferredNotes;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        if (updateTime == null) updateTime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}
