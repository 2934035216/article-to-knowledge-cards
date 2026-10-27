package com.lizhenqi.service;

import com.lizhenqi.entity.Order;
import com.lizhenqi.entity.Perfume;
import com.lizhenqi.repository.OrderRepository;
import com.lizhenqi.repository.PerfumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 系统管理模块：订单管理 + 数据统计
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PerfumeRepository perfumeRepository;

    public OrderService(OrderRepository orderRepository, PerfumeRepository perfumeRepository) {
        this.orderRepository = orderRepository;
        this.perfumeRepository = perfumeRepository;
    }

    @Transactional
    public Order createOrder(Long userId, Long perfumeId, int quantity,
                             String receiverName, String receiverPhone, String receiverAddr) {
        Perfume p = perfumeRepository.findById(perfumeId)
                .orElseThrow(() -> new RuntimeException("香水不存在"));

        if (p.getStock() != null && p.getStock() < quantity) {
            throw new RuntimeException("库存不足");
        }

        Order o = new Order();
        o.setOrderNo(generateOrderNo());
        o.setUserId(userId);
        o.setPerfumeId(perfumeId);
        o.setQuantity(quantity);
        o.setPrice(p.getPrice());
        o.setTotalAmount(p.getPrice().multiply(BigDecimal.valueOf(quantity)));
        o.setReceiverName(receiverName);
        o.setReceiverPhone(receiverPhone);
        o.setReceiverAddr(receiverAddr);
        o.setStatus("PENDING");

        // 扣库存 & 增销量
        p.setStock((p.getStock() == null ? 0 : p.getStock()) - quantity);
        p.setSales((p.getSales() == null ? 0 : p.getSales()) + quantity);
        perfumeRepository.save(p);

        return orderRepository.save(o);
    }

    public Order findById(Long id) { return orderRepository.findById(id).orElse(null); }

    public List<Order> myOrders(Long userId) { return orderRepository.findByUserIdOrderByCreateTimeDesc(userId); }

    public List<Order> allOrders() { return orderRepository.findAll(); }

    public List<Order> ordersByStatus(String status) { return orderRepository.findByStatus(status); }

    @Transactional
    public Order updateStatus(Long id, String status) {
        return orderRepository.findById(id).map(o -> {
            o.setStatus(status);
            return orderRepository.save(o);
        }).orElse(null);
    }

    @Transactional
    public void delete(Long id) { orderRepository.deleteById(id); }

    // ===== 统计 =====
    public BigDecimal totalRevenue() { return orderRepository.totalRevenue(); }
    public long totalOrders() { return orderRepository.countAll(); }
    public List<Object[]> orderStatusBreakdown() { return orderRepository.countByStatus(); }

    private String generateOrderNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String rand = String.valueOf((int)(Math.random() * 9000) + 1000);
        return "P" + ts + rand;
    }
}
