package com.lizhenqi.service.impl;

import com.lizhenqi.entity.LizhenqiShangpin;
import com.lizhenqi.repository.LizhenqiShangpinRepository;
import com.lizhenqi.service.LizhenqiShangpinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LizhenqiShangpinServiceImpl implements LizhenqiShangpinService {

    private final LizhenqiShangpinRepository lizhenqiShangpinRepository;

    @Autowired
    public LizhenqiShangpinServiceImpl(LizhenqiShangpinRepository lizhenqiShangpinRepository) {
        this.lizhenqiShangpinRepository = lizhenqiShangpinRepository;
    }

    @Override
    public List<LizhenqiShangpin> findAll() {
        return lizhenqiShangpinRepository.findAll();
    }

    @Override
    public Optional<LizhenqiShangpin> findById(Long id) {
        return lizhenqiShangpinRepository.findById(id);
    }

    @Override
    public LizhenqiShangpin save(LizhenqiShangpin lizhenqiShangpin) {
        return lizhenqiShangpinRepository.save(lizhenqiShangpin);
    }

    @Override
    public void deleteById(Long id) {
        lizhenqiShangpinRepository.deleteById(id);
    }

    @Override
    public List<LizhenqiShangpin> searchByMingcheng(String mingcheng) {
        return lizhenqiShangpinRepository.findByMingchengContaining(mingcheng);
    }

    @Override
    public List<LizhenqiShangpin> findByLeibie(String leibie) {
        return lizhenqiShangpinRepository.findByLeibie(leibie);
    }
}