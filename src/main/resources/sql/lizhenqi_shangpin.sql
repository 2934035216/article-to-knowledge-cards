CREATE DATABASE IF NOT EXISTS lizhenqi_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE lizhenqi_db;

CREATE TABLE IF NOT EXISTS lizhenqi_shangpin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mingcheng VARCHAR(100) NOT NULL COMMENT '商品名称',
    leibie VARCHAR(50) COMMENT '商品类别',
    jiage DECIMAL(10,2) COMMENT '商品价格',
    kucun INT DEFAULT 0 COMMENT '库存数量',
    beizhu VARCHAR(500) COMMENT '备注信息',
    chuangjian_shijian DATETIME COMMENT '创建时间',
    gengxin_shijian DATETIME COMMENT '更新时间',
    INDEX idx_mingcheng (mingcheng),
    INDEX idx_leibie (leibie)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

INSERT INTO lizhenqi_shangpin (mingcheng, leibie, jiage, kucun, beizhu, chuangjian_shijian, gengxin_shijian) VALUES
('iPhone 15 Pro Max', '电子产品', 9999.00, 50, '苹果最新旗舰手机，搭载A17 Pro芯片', NOW(), NOW()),
('华为Mate 60 Pro', '电子产品', 6999.00, 80, '华为旗舰手机，支持卫星通话', NOW(), NOW()),
('小米14 Ultra', '电子产品', 5999.00, 120, '小米高端旗舰，徕卡影像系统', NOW(), NOW()),
('MacBook Pro 16寸', '电子产品', 19999.00, 30, '苹果笔记本电脑，M3 Max芯片', NOW(), NOW()),
('戴尔XPS 15', '电子产品', 12999.00, 45, '戴尔高端轻薄本，4K触控屏', NOW(), NOW()),
('Nike Air Jordan 1', '服装鞋帽', 1299.00, 200, '经典篮球鞋，限量版配色', NOW(), NOW()),
('Adidas Ultraboost', '服装鞋帽', 1199.00, 150, '舒适跑鞋，Boost中底科技', NOW(), NOW()),
('优衣库羽绒服', '服装鞋帽', 599.00, 300, '轻薄保暖，防风防水', NOW(), NOW()),
('可口可乐 500ml', '食品饮料', 3.50, 1000, '经典碳酸饮料', NOW(), NOW()),
('农夫山泉矿泉水', '食品饮料', 2.00, 2000, '天然矿泉水，源自长白山', NOW(), NOW()),
('旺仔牛奶', '食品饮料', 5.00, 500, '儿童营养牛奶，香甜可口', NOW(), NOW()),
('三只松鼠坚果礼盒', '食品饮料', 128.00, 200, '混合坚果，送礼佳品', NOW(), NOW()),
('晨光中性笔套装', '图书文具', 25.00, 500, '学生必备，书写流畅', NOW(), NOW()),
('得力文件夹', '图书文具', 15.00, 800, '办公必备，质量可靠', NOW(), NOW()),
('笔记本电脑包', '日用百货', 99.00, 150, '防水材质，大容量', NOW(), NOW()),
('保温杯', '日用百货', 68.00, 200, '304不锈钢，保温12小时', NOW(), NOW()),
('洗衣液', '日用百货', 35.00, 300, '深层洁净，持久留香', NOW(), NOW()),
('抽纸', '日用百货', 20.00, 500, '原木浆，柔软亲肤', NOW(), NOW()),
('乐高积木套装', '图书文具', 299.00, 80, '儿童益智玩具，拼搭乐趣', NOW(), NOW()),
('Kindle Paperwhite', '电子产品', 1258.00, 60, '电子书阅读器，护眼墨水屏', NOW(), NOW());