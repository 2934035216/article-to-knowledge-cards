package com.lizhenqi.service;

import com.lizhenqi.entity.Brand;
import com.lizhenqi.entity.Category;
import com.lizhenqi.entity.User;
import com.lizhenqi.repository.BrandRepository;
import com.lizhenqi.repository.CategoryRepository;
import com.lizhenqi.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户管理模块：用户注册登录、商家注册登录、个人信息管理、账号状态管理
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public UserService(UserRepository userRepository,
                       BrandRepository brandRepository,
                       CategoryRepository categoryRepository) {
        this.userRepository = userRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    public boolean exists(String username) {
        return userRepository.existsByUsername(username);
    }

    public User registerUser(String username, String password, String nickname) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(password);
        u.setNickname(nickname != null ? nickname : username);
        u.setRole("USER");
        u.setStatus("ACTIVE");
        return userRepository.save(u);
    }

    public User registerMerchant(String username, String password, String nickname) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(password);
        u.setNickname(nickname != null ? nickname : username);
        u.setRole("MERCHANT");
        u.setStatus("ACTIVE");
        return userRepository.save(u);
    }

    public User save(User user) { return userRepository.save(user); }

    public User findById(Long id) { return userRepository.findById(id).orElse(null); }

    public List<User> listAll() { return userRepository.findAll(); }

    public List<User> listByRole(String role) { return userRepository.findByRole(role); }

    public void ban(Long id) {
        userRepository.findById(id).ifPresent(u -> {
            u.setStatus("BANNED");
            userRepository.save(u);
        });
    }

    public void unban(Long id) {
        userRepository.findById(id).ifPresent(u -> {
            u.setStatus("ACTIVE");
            userRepository.save(u);
        });
    }

    public void delete(Long id) { userRepository.deleteById(id); }

    public List<Brand> allBrands() { return brandRepository.findAll(); }
    public List<Category> allCategories() { return categoryRepository.findAll(); }
}
