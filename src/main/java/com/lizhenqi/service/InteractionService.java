package com.lizhenqi.service;

import com.lizhenqi.entity.*;
import com.lizhenqi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户互动模块：收藏、评价、浏览记录、公告
 */
@Service
public class InteractionService {

    private final FavoriteRepository favoriteRepository;
    private final ReviewRepository reviewRepository;
    private final BrowseRecordRepository browseRecordRepository;
    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final PerfumeRepository perfumeRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public InteractionService(FavoriteRepository favoriteRepository,
                              ReviewRepository reviewRepository,
                              BrowseRecordRepository browseRecordRepository,
                              NoticeRepository noticeRepository,
                              UserRepository userRepository,
                              PerfumeRepository perfumeRepository,
                              BrandRepository brandRepository,
                              CategoryRepository categoryRepository) {
        this.favoriteRepository = favoriteRepository;
        this.reviewRepository = reviewRepository;
        this.browseRecordRepository = browseRecordRepository;
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
        this.perfumeRepository = perfumeRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    // ===== 收藏 =====
    @Transactional
    public Favorite toggleFavorite(Long userId, Long perfumeId) {
        Favorite f = favoriteRepository.findByUserIdAndPerfumeId(userId, perfumeId).orElse(null);
        if (f != null) {
            favoriteRepository.delete(f);
            return null;
        }
        Favorite nf = new Favorite();
        nf.setUserId(userId);
        nf.setPerfumeId(perfumeId);
        return favoriteRepository.save(nf);
    }

    public boolean isFavorite(Long userId, Long perfumeId) {
        return favoriteRepository.existsByUserIdAndPerfumeId(userId, perfumeId);
    }

    public List<Favorite> myFavorites(Long userId) {
        List<Favorite> favs = favoriteRepository.findByUserIdOrderByCreateTimeDesc(userId);
        favs.forEach(f -> perfumeRepository.findById(f.getPerfumeId()).ifPresent(f::setPerfume));
        return favs;
    }

    // ===== 评价 =====
    public Review addReview(Review r) { return reviewRepository.save(r); }

    public List<Review> reviewsByPerfume(Long perfumeId) {
        List<Review> list = reviewRepository.findByPerfumeIdOrderByCreateTimeDesc(perfumeId);
        list.forEach(this::enrichReview);
        return list;
    }

    public List<Review> myReviews(Long userId) {
        List<Review> list = reviewRepository.findByUserIdOrderByCreateTimeDesc(userId);
        list.forEach(this::enrichReview);
        return list;
    }

    public Double avgRating(Long perfumeId) { return reviewRepository.avgRatingByPerfumeId(perfumeId); }
    public int reviewCount(Long perfumeId) { return reviewRepository.countByPerfumeId(perfumeId); }

    @Transactional
    public void deleteReview(Long id) { reviewRepository.deleteById(id); }

    private void enrichReview(Review r) {
        userRepository.findById(r.getUserId()).ifPresent(u -> {
            r.setUsername(u.getUsername());
            r.setNickname(u.getNickname());
        });
        perfumeRepository.findById(r.getPerfumeId()).ifPresent(p -> r.setPerfumeName(p.getName()));
    }

    // ===== 浏览记录 =====
    public void recordBrowse(Long userId, Long perfumeId) {
        BrowseRecord br = new BrowseRecord();
        br.setUserId(userId);
        br.setPerfumeId(perfumeId);
        browseRecordRepository.save(br);
    }

    public List<BrowseRecord> myBrowseHistory(Long userId) {
        List<BrowseRecord> list = browseRecordRepository.findByUserIdOrderByBrowseTimeDesc(userId);
        list.forEach(b -> perfumeRepository.findById(b.getPerfumeId()).ifPresent(b::setPerfume));
        return list;
    }

    @Transactional
    public void clearBrowseHistory(Long userId) { browseRecordRepository.deleteByUserId(userId); }

    // ===== 公告 =====
    public List<Notice> allNotices() { return noticeRepository.findAllByOrderByCreateTimeDesc(); }
    public Notice findNotice(Long id) { return noticeRepository.findById(id).orElse(null); }
    public Notice saveNotice(Notice n) { return noticeRepository.save(n); }
    public void deleteNotice(Long id) { noticeRepository.deleteById(id); }

    // ===== 统计仪表盘数据 =====
    public Map<String, Object> perfumeStats(Long perfumeId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("favoriteCount", favoriteRepository.countByPerfumeId(perfumeId));
        stats.put("reviewCount", reviewRepository.countByPerfumeId(perfumeId));
        stats.put("avgRating", reviewRepository.avgRatingByPerfumeId(perfumeId));
        return stats;
    }

    public Brand findBrand(Long id) { return brandRepository.findById(id).orElse(null); }
    public Category findCategory(Long id) { return categoryRepository.findById(id).orElse(null); }
    public List<Brand> allBrands() { return brandRepository.findAll(); }
    public List<Category> allCategories() { return categoryRepository.findAll(); }
}
