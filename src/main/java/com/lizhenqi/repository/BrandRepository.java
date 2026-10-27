package com.lizhenqi.repository;

import com.lizhenqi.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findByNameContaining(String keyword);
}
