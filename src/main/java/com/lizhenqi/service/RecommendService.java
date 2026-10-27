package com.lizhenqi.service;

import com.lizhenqi.entity.*;
import com.lizhenqi.repository.*;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 推荐模块：个人偏好管理 + 香水推荐
 */
@Service
public class RecommendService {

    private final UserPreferenceRepository preferenceRepository;
    private final PerfumeRepository perfumeRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public RecommendService(UserPreferenceRepository preferenceRepository,
                            PerfumeRepository perfumeRepository,
                            BrandRepository brandRepository,
                            CategoryRepository categoryRepository) {
        this.preferenceRepository = preferenceRepository;
        this.perfumeRepository = perfumeRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    public UserPreference getPreference(Long userId) {
        return preferenceRepository.findByUserId(userId).orElse(null);
    }

    public UserPreference savePreference(Long userId, String brandIds, String categoryIds, String notes) {
        UserPreference p = preferenceRepository.findByUserId(userId).orElseGet(() -> {
            UserPreference up = new UserPreference();
            up.setUserId(userId);
            return up;
        });
        p.setPreferredBrands(brandIds);
        p.setPreferredCategories(categoryIds);
        p.setPreferredNotes(notes);
        return preferenceRepository.save(p);
    }

    /**
     * 基于用户偏好 + 热门销量 + 新品 的简单推荐算法
     */
    public List<Perfume> recommend(Long userId, int limit) {
        UserPreference pref = preferenceRepository.findByUserId(userId).orElse(null);

        // 1. 有偏好 -> 按偏好匹配
        if (pref != null && (pref.getPreferredBrands() != null || pref.getPreferredCategories() != null)) {
            List<Long> brandIds = parseIds(pref.getPreferredBrands());
            List<Long> catIds   = parseIds(pref.getPreferredCategories());
            List<Perfume> matched = Collections.emptyList();
            if (!brandIds.isEmpty() || !catIds.isEmpty()) {
                matched = perfumeRepository.findRecommendations(brandIds, catIds);
            }
            if (!matched.isEmpty()) {
                matched.forEach(p -> {
                    if (p.getBrandId() != null)
                        brandRepository.findById(p.getBrandId()).ifPresent(b -> p.setBrandName(b.getName()));
                    if (p.getCategoryId() != null)
                        categoryRepository.findById(p.getCategoryId()).ifPresent(c -> p.setCategoryName(c.getName()));
                });
                if (matched.size() >= limit) return matched.subList(0, limit);
            }
        }

        // 2. 无偏好或匹配不足 -> 热门 + 新品混合
        List<Perfume> hot = perfumeRepository.findTop10ByOrderBySalesDesc();
        hot.forEach(p -> {
            if (p.getBrandId() != null)
                brandRepository.findById(p.getBrandId()).ifPresent(b -> p.setBrandName(b.getName()));
            if (p.getCategoryId() != null)
                categoryRepository.findById(p.getCategoryId()).ifPresent(c -> p.setCategoryName(c.getName()));
        });
        return hot.subList(0, Math.min(hot.size(), limit));
    }

    private List<Long> parseIds(String raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
}
