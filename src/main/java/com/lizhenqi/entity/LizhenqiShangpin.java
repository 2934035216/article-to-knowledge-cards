package com.lizhenqi.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "lizhenqi_shangpin")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LizhenqiShangpin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mingcheng", nullable = false, length = 100)
    private String mingcheng;

    @Column(name = "leibie", length = 50)
    private String leibie;

    @Column(name = "jiage", precision = 10, scale = 2)
    private BigDecimal jiage;

    @Column(name = "kucun")
    private Integer kucun;

    @Column(name = "beizhu", length = 500)
    private String beizhu;

    @Column(name = "chuangjian_shijian")
    private Date chuangjianShijian;

    @Column(name = "gengxin_shijian")
    private Date gengxinShijian;

    @PrePersist
    protected void onCreate() {
        chuangjianShijian = new Date();
        gengxinShijian = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        gengxinShijian = new Date();
    }
}