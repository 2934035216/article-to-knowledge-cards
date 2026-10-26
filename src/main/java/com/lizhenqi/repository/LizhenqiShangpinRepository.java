package com.lizhenqi.repository;

import com.lizhenqi.entity.LizhenqiShangpin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LizhenqiShangpinRepository extends JpaRepository<LizhenqiShangpin, Long> {

    List<LizhenqiShangpin> findByMingchengContaining(String mingcheng);

    List<LizhenqiShangpin> findByLeibie(String leibie);
}