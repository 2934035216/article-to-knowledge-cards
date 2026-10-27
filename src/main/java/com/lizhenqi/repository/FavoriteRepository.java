package com.lizhenqi.repository;

import com.lizhenqi.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserIdOrderByCreateTimeDesc(Long userId);
    Optional<Favorite> findByUserIdAndPerfumeId(Long userId, Long perfumeId);
    boolean existsByUserIdAndPerfumeId(Long userId, Long perfumeId);
    void deleteByUserIdAndPerfumeId(Long userId, Long perfumeId);
    int countByPerfumeId(Long perfumeId);
}
