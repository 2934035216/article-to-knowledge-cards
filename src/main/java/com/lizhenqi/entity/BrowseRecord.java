package com.lizhenqi.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "browse_record")
public class BrowseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "perfume_id", nullable = false)
    private Long perfumeId;

    @Column(name = "browse_time")
    private LocalDateTime browseTime;

    @Transient
    private Perfume perfume;

    @PrePersist
    public void prePersist() {
        if (browseTime == null) browseTime = LocalDateTime.now();
    }
}
