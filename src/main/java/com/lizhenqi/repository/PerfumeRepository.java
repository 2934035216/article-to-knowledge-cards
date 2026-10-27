package com.lizhenqi.repository;

import com.lizhenqi.entity.Perfume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PerfumeRepository extends JpaRepository<Perfume, Long> {
    Page<Perfume> findByNameContaining(String keyword, Pageable pageable);
    Page<Perfume> findByBrandId(Long brandId, Pageable pageable);
    Page<Perfume> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Perfume> findByMerchantId(Long merchantId, Pageable pageable);
    List<Perfume> findTop10ByOrderBySalesDesc();
    List<Perfume> findTop8ByOrderByCreateTimeDesc();

    @Query("SELECT p FROM Perfume p WHERE (:brandId IS NULL OR p.brandId = :brandId) " +
           "AND (:categoryId IS NULL OR p.categoryId = :categoryId) " +
           "AND (:keyword IS NULL OR p.name LIKE %:keyword%)")
    Page<Perfume> search(@Param("brandId") Long brandId,
                         @Param("categoryId") Long categoryId,
                         @Param("keyword") String keyword,
                         Pageable pageable);

    @Query(value = "SELECT p.* FROM perfume p WHERE p.brand_id IN (:brandIds) " +
           "OR p.category_id IN (:catIds) ORDER BY p.sales DESC LIMIT 10", nativeQuery = true)
    List<Perfume> findRecommendations(@Param("brandIds") List<Long> brandIds,
                                      @Param("catIds") List<Long> catIds);
}
