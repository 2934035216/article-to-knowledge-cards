package com.lizhenqi.service;

import com.lizhenqi.entity.LizhenqiShangpin;

import java.util.List;
import java.util.Optional;

public interface LizhenqiShangpinService {

    List<LizhenqiShangpin> findAll();

    Optional<LizhenqiShangpin> findById(Long id);

    LizhenqiShangpin save(LizhenqiShangpin lizhenqiShangpin);

    void deleteById(Long id);

    List<LizhenqiShangpin> searchByMingcheng(String mingcheng);

    List<LizhenqiShangpin> findByLeibie(String leibie);
}