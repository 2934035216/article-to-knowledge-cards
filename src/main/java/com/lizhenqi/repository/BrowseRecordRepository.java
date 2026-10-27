package com.lizhenqi.repository;

import com.lizhenqi.entity.BrowseRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrowseRecordRepository extends JpaRepository<BrowseRecord, Long> {
    List<BrowseRecord> findByUserIdOrderByBrowseTimeDesc(Long userId);
    void deleteByUserId(Long userId);
}
