package com.lizhenqi.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "perfume")
public class Perfume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "brand_id")
    private Long brandId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 50)
    private String volume;

    @Column(name = "main_notes", length = 255)
    private String mainNotes;

    @Column(name = "heart_notes", length = 255)
    private String heartNotes;

    @Column(name = "base_notes", length = 255)
    private String baseNotes;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column
    private Integer stock = 0;

    @Column
    private Integer sales = 0;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Transient
    private String brandName;

    @Transient
    private String categoryName;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createTime == null) createTime = now;
        if (updateTime == null) updateTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}
