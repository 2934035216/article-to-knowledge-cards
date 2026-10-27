package com.lizhenqi.repository;

import com.lizhenqi.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByPerfumeIdOrderByCreateTimeDesc(Long perfumeId);
    List<Review> findByUserIdOrderByCreateTimeDesc(Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.perfumeId = :perfumeId")
    Double avgRatingByPerfumeId(Long perfumeId);

    int countByPerfumeId(Long perfumeId);
}
