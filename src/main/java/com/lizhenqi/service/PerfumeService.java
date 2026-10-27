package com.lizhenqi.service;

import com.lizhenqi.entity.Brand;
import com.lizhenqi.entity.Category;
import com.lizhenqi.entity.Perfume;
import com.lizhenqi.repository.BrandRepository;
import com.lizhenqi.repository.CategoryRepository;
import com.lizhenqi.repository.PerfumeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 香水信息管理模块：香水/品牌/分类 CRUD、查询、详情查看
 */
@Service
public class PerfumeService {

    private final PerfumeRepository perfumeRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public PerfumeService(PerfumeRepository perfumeRepository,
                          BrandRepository brandRepository,
                          CategoryRepository categoryRepository) {
        this.perfumeRepository = perfumeRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    // ---- Brand ----
    public List<Brand> allBrands() { return brandRepository.findAll(); }
    public Brand saveBrand(Brand b) { return brandRepository.save(b); }
    public Brand findBrand(Long id) { return brandRepository.findById(id).orElse(null); }
    public void deleteBrand(Long id) { brandRepository.deleteById(id); }

    // ---- Category ----
    public List<Category> allCategories() { return categoryRepository.findAll(); }
    public Category saveCategory(Category c) { return categoryRepository.save(c); }
    public Category findCategory(Long id) { return categoryRepository.findById(id).orElse(null); }
    public void deleteCategory(Long id) { categoryRepository.deleteById(id); }

    // ---- Perfume ----
    public Perfume save(Perfume p) {
        if (p.getBrandId() != null) {
            brandRepository.findById(p.getBrandId())
                    .ifPresent(b -> p.setBrandName(b.getName()));
        }
        if (p.getCategoryId() != null) {
            categoryRepository.findById(p.getCategoryId())
                    .ifPresent(c -> p.setCategoryName(c.getName()));
        }
        return perfumeRepository.save(p);
    }

    public Perfume findById(Long id) {
        Perfume p = perfumeRepository.findById(id).orElse(null);
        if (p != null) enrich(p);
        return p;
    }

    private void enrich(Perfume p) {
        if (p.getBrandId() != null)
            brandRepository.findById(p.getBrandId()).ifPresent(b -> p.setBrandName(b.getName()));
        if (p.getCategoryId() != null)
            categoryRepository.findById(p.getCategoryId()).ifPresent(c -> p.setCategoryName(c.getName()));
    }

    public Page<Perfume> search(Long brandId, Long categoryId, String keyword, int page, int size) {
        Page<Perfume> result = perfumeRepository.search(brandId, categoryId, keyword, PageRequest.of(page, size));
        result.forEach(this::enrich);
        return result;
    }

    public Page<Perfume> findByMerchant(Long merchantId, int page, int size) {
        Page<Perfume> result = perfumeRepository.findByMerchantId(merchantId, PageRequest.of(page, size));
        result.forEach(this::enrich);
        return result;
    }

    public List<Perfume> topSales() {
        List<Perfume> list = perfumeRepository.findTop10ByOrderBySalesDesc();
        list.forEach(this::enrich);
        return list;
    }

    public List<Perfume> newest() {
        List<Perfume> list = perfumeRepository.findTop8ByOrderByCreateTimeDesc();
        list.forEach(this::enrich);
        return list;
    }

    public void delete(Long id) { perfumeRepository.deleteById(id); }

    public void incrementSales(Long perfumeId, int qty) {
        perfumeRepository.findById(perfumeId).ifPresent(p -> {
            p.setSales((p.getSales() == null ? 0 : p.getSales()) + qty);
            if (p.getStock() != null) p.setStock(Math.max(0, p.getStock() - qty));
            perfumeRepository.save(p);
        });
    }
}
