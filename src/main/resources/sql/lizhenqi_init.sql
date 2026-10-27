CREATE DATABASE IF NOT EXISTS lizhenqi_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE lizhenqi_db;

-- ===== 用户表（消费者 & 商家共用，通过 role 区分） =====
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username        VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录账号',
    password        VARCHAR(100) NOT NULL COMMENT '密码',
    nickname        VARCHAR(50)  COMMENT '昵称',
    phone           VARCHAR(20)  COMMENT '手机号',
    email           VARCHAR(100) COMMENT '邮箱',
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色 USER/MERCHANT/ADMIN',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/BANNED',
    avatar          VARCHAR(255) COMMENT '头像URL',
    create_time     DATETIME     COMMENT '创建时间',
    update_time     DATETIME     COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ===== 品牌表 =====
DROP TABLE IF EXISTS brand;
CREATE TABLE brand (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name            VARCHAR(100) NOT NULL COMMENT '品牌名称',
    description     VARCHAR(500) COMMENT '品牌简介',
    logo_url        VARCHAR(255) COMMENT '品牌Logo',
    create_time     DATETIME     COMMENT '创建时间',
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='品牌表';

-- ===== 分类表 =====
DROP TABLE IF EXISTS category;
CREATE TABLE category (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name            VARCHAR(100) NOT NULL COMMENT '分类名称',
    description     VARCHAR(500) COMMENT '分类简介',
    create_time     DATETIME     COMMENT '创建时间',
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- ===== 香水表 =====
DROP TABLE IF EXISTS perfume;
CREATE TABLE perfume (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name            VARCHAR(100) NOT NULL COMMENT '香水名称',
    brand_id        BIGINT       COMMENT '品牌ID',
    category_id     BIGINT       COMMENT '分类ID',
    price           DECIMAL(10,2) COMMENT '价格',
    volume          VARCHAR(50)  COMMENT '容量(ml)',
    main_notes      VARCHAR(255) COMMENT '前调',
    heart_notes     VARCHAR(255) COMMENT '中调',
    base_notes      VARCHAR(255) COMMENT '后调',
    description     TEXT         COMMENT '详情描述',
    image_url       VARCHAR(255) COMMENT '主图',
    stock           INT DEFAULT 0 COMMENT '库存',
    sales           INT DEFAULT 0 COMMENT '销量',
    merchant_id     BIGINT       COMMENT '商家ID',
    create_time     DATETIME     COMMENT '创建时间',
    update_time     DATETIME     COMMENT '更新时间',
    INDEX idx_name (name),
    INDEX idx_brand (brand_id),
    INDEX idx_category (category_id),
    FOREIGN KEY (brand_id)    REFERENCES brand(id)    ON DELETE SET NULL,
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='香水表';

-- ===== 个人偏好表 =====
DROP TABLE IF EXISTS user_preference;
CREATE TABLE user_preference (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id             BIGINT       NOT NULL COMMENT '用户ID',
    preferred_brands    VARCHAR(500) COMMENT '偏好品牌(逗号分隔)',
    preferred_categories VARCHAR(500) COMMENT '偏好分类(逗号分隔)',
    preferred_notes     VARCHAR(500) COMMENT '偏好香调',
    update_time         DATETIME     COMMENT '更新时间',
    UNIQUE KEY uk_user (user_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个人偏好表';

-- ===== 收藏表 =====
DROP TABLE IF EXISTS favorite;
CREATE TABLE favorite (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id         BIGINT   NOT NULL COMMENT '用户ID',
    perfume_id      BIGINT   NOT NULL COMMENT '香水ID',
    create_time     DATETIME COMMENT '创建时间',
    UNIQUE KEY uk_user_perfume (user_id, perfume_id),
    FOREIGN KEY (user_id)    REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (perfume_id) REFERENCES perfume(id)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- ===== 评价表 =====
DROP TABLE IF EXISTS review;
CREATE TABLE review (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    perfume_id      BIGINT       NOT NULL COMMENT '香水ID',
    rating          INT          NOT NULL DEFAULT 5 COMMENT '评分1-5',
    content         VARCHAR(1000) COMMENT '评价内容',
    create_time     DATETIME     COMMENT '创建时间',
    update_time     DATETIME     COMMENT '更新时间',
    INDEX idx_perfume (perfume_id),
    FOREIGN KEY (user_id)    REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (perfume_id) REFERENCES perfume(id)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';

-- ===== 浏览记录表 =====
DROP TABLE IF EXISTS browse_record;
CREATE TABLE browse_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id         BIGINT   NOT NULL COMMENT '用户ID',
    perfume_id      BIGINT   NOT NULL COMMENT '香水ID',
    browse_time     DATETIME COMMENT '浏览时间',
    INDEX idx_user (user_id),
    INDEX idx_perfume (perfume_id),
    FOREIGN KEY (user_id)    REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (perfume_id) REFERENCES perfume(id)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浏览记录表';

-- ===== 公告表 =====
DROP TABLE IF EXISTS notice;
CREATE TABLE notice (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title           VARCHAR(200) NOT NULL COMMENT '公告标题',
    content         TEXT         COMMENT '公告内容',
    create_time     DATETIME     COMMENT '创建时间',
    update_time     DATETIME     COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

-- ===== 订单表 =====
DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_no        VARCHAR(50)  NOT NULL UNIQUE COMMENT '订单号',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    perfume_id      BIGINT       NOT NULL COMMENT '香水ID',
    quantity        INT          NOT NULL DEFAULT 1 COMMENT '数量',
    price           DECIMAL(10,2) COMMENT '单价',
    total_amount    DECIMAL(10,2) COMMENT '总价',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/PAID/SHIPPED/COMPLETED/CANCELED',
    receiver_name   VARCHAR(50)  COMMENT '收货人',
    receiver_phone  VARCHAR(20)  COMMENT '收货电话',
    receiver_addr   VARCHAR(300) COMMENT '收货地址',
    create_time     DATETIME     COMMENT '创建时间',
    update_time     DATETIME     COMMENT '更新时间',
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id)    REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (perfume_id) REFERENCES perfume(id)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- =====================================================
-- 种子数据
-- =====================================================

-- 用户（密码均为 123456，明文示例，实际应加盐哈希）
INSERT INTO sys_user (username, password, nickname, phone, email, role, status, create_time) VALUES
('admin',    '123456', '超级管理员', '13800000000', 'admin@demo.com',    'ADMIN',    'ACTIVE', NOW()),
('merchant1','123456', '香奈儿专柜', '13800000001', 'merchant1@demo.com', 'MERCHANT', 'ACTIVE', NOW()),
('merchant2','123456', '迪奥专柜',   '13800000002', 'merchant2@demo.com', 'MERCHANT', 'ACTIVE', NOW()),
('user1',    '123456', '小资情调',   '13800000011', 'user1@demo.com',     'USER',     'ACTIVE', NOW()),
('user2',    '123456', '香水小白',   '13800000012', 'user2@demo.com',     'USER',     'ACTIVE', NOW()),
('user3',    '123456', '香评大师',   '13800000013', 'user3@demo.com',     'USER',     'ACTIVE', NOW());

-- 品牌
INSERT INTO brand (name, description, create_time) VALUES
('香奈儿 Chanel',     '法国高级时装与香水品牌，N°5 闻名世界', NOW()),
('迪奥 Dior',         '法国奢侈品牌，香水系列经典优雅',       NOW()),
('古驰 Gucci',        '意大利奢侈品牌，香水时尚潮流',         NOW()),
('爱马仕 Hermès',     '法国顶级奢华品牌，大地男士香水知名',   NOW()),
('兰蔻 Lancôme',      '法国美妆护肤品牌，香水温和甜美',       NOW()),
('祖玛珑 Jo Malone',  '英国小众沙龙香，简洁纯净',             NOW()),
('YSL 圣罗兰',        '法国先锋时尚品牌，黑鸦片等大热款',     NOW());

-- 分类
INSERT INTO category (name, description, create_time) VALUES
('花香调',     '以鲜花为主要香调，柔美优雅',   NOW()),
('果香调',     '以水果为香调，清新甜美',       NOW()),
('木质调',     '木质为主，温暖沉稳',           NOW()),
('东方调',     '浓郁神秘，温暖性感',           NOW()),
('清新调',     '柑橘绿叶，清爽干净',           NOW()),
('美食调',     '香草焦糖，甜蜜诱人',           NOW());

-- 香水
INSERT INTO perfume (name, brand_id, category_id, price, volume, main_notes, heart_notes, base_notes, description, image_url, stock, sales, merchant_id, create_time, update_time) VALUES
('香奈儿 N°5',       1, 1, 1280.00, '100ml', '柑橘、橙花', '茉莉、玫瑰', '檀香、香草', '经典花香调，玛丽莲·梦露最爱', '/lizhenqi/img/perfume-1.png', 50, 230, 2, NOW(), NOW()),
('香奈儿 可可小姐',  1, 1, 1080.00, '100ml', '柑橘、佛手柑', '玫瑰、茉莉', '广藿香、香草', '性感优雅的花香东方调',        '/lizhenqi/img/perfume-2.png', 60, 180, 2, NOW(), NOW()),
('迪奥 真我',        2, 1, 1180.00, '100ml', '香柠檬、甜瓜', '茉莉、玫瑰', '檀香、香草', '纯净花香，温柔灵动',           '/lizhenqi/img/perfume-3.jpg', 80, 320, 3, NOW(), NOW()),
('迪奥 旷野男士',    2, 3, 980.00,  '100ml', '佛手柑、胡椒', '龙涎醚', '雪松、琥珀', '阳刚木质调，当代男士香水标杆', '/lizhenqi/img/perfume-4.jpg', 45, 410, 3, NOW(), NOW()),
('古驰 花之舞',      3, 2, 880.00,  '100ml', '柑橘、草莓', '茉莉、玫瑰', '麝香、木质', '甜美果香花香，年轻活泼',       '/lizhenqi/img/perfume-5.jpg', 70, 150, 2, NOW(), NOW()),
('古驰 罪爱女士',    3, 4, 920.00,  '100ml', '粉红胡椒', '紫罗兰、广藿香', '琥珀、麝香', '性感东方调，成熟女性之选',     '/lizhenqi/img/perfume-6.jpg', 35, 200, 2, NOW(), NOW()),
('爱马仕 大地男士',  4, 3, 1080.00, '100ml', '橙子、葡萄柚', '胡椒、天竺葵', '香根草、雪松', '温暖木质，当代男性气质',       '/lizhenqi/img/perfume-7.jpg', 40, 380, 3, NOW(), NOW()),
('爱马仕 尼罗河花园',4, 5, 980.00,  '100ml', '柑橘、青芒果', '莲花、风信子', '雪松、檀香', '清新花园调，夏日必备',         '/lizhenqi/img/perfume-8.jpg', 55, 260, 3, NOW(), NOW()),
('兰蔻 奇迹',        5, 2, 680.00,  '100ml', '桃子、荔枝', '茉莉、铃兰', '麝香、檀香', '甜美果香花香，日常百搭',       '/lizhenqi/img/perfume-9.jpg', 100, 520, 2, NOW(), NOW()),
('祖玛珑 蓝风铃',    6, 5, 1080.00, '100ml', '蓝风铃', '野草莓叶', '麝香', '清澈干净，少女感十足',         '/lizhenqi/img/perfume-10.jpg', 30, 290, 2, NOW(), NOW()),
('祖玛珑 英国梨与小苍兰', 6, 2, 1080.00, '100ml', '梨、小苍兰', '玫瑰', '琥珀、广藿香', '清甜果香，温柔知性',     '/lizhenqi/img/perfume-11.png', 40, 340, 2, NOW(), NOW()),
('YSL 黑鸦片',       7, 4, 1080.00, '100ml', '粉红胡椒、橙花', '咖啡、玫瑰', '香草、广藿香', '浓郁东方调，神秘性感',       '/lizhenqi/img/perfume-12.jpg', 60, 450, 3, NOW(), NOW()),
('YSL 自由之水',     7, 1, 980.00,  '100ml', '柑橘、薰衣草', '橙花、茉莉', '香草、麝香', '现代女性独立自由之选',         '/lizhenqi/img/perfume-13.jpg', 50, 310, 3, NOW(), NOW());

-- 偏好
INSERT INTO user_preference (user_id, preferred_brands, preferred_categories, preferred_notes, update_time) VALUES
(4, '1,2',  '1,2',  '花香,甜美', NOW()),
(5, '6,5',  '5,2',  '清新,果香', NOW()),
(6, '4,7',  '3,4',  '木质,东方', NOW());

-- 收藏
INSERT INTO favorite (user_id, perfume_id, create_time) VALUES
(4, 1, NOW()), (4, 3, NOW()), (4, 10, NOW()),
(5, 8, NOW()), (5, 10, NOW()),
(6, 4, NOW()), (6, 7, NOW()), (6, 12, NOW());

-- 评价
INSERT INTO review (user_id, perfume_id, rating, content, create_time) VALUES
(4, 1, 5, '经典就是经典，每次闻都很惊艳', NOW()),
(4, 10, 4, '很干净的味道，留香稍短', NOW()),
(5, 8, 5, '夏天用太舒服了！', NOW()),
(5, 11, 5, '英国梨真的是闻一次爱一次', NOW()),
(6, 4, 5, '旷野的味道太有荷尔蒙了', NOW()),
(6, 12, 4, '黑鸦片浓郁但很上头', NOW()),
(6, 7, 5, '大地是永远的神', NOW());

-- 浏览记录
INSERT INTO browse_record (user_id, perfume_id, browse_time) VALUES
(4, 1, NOW()), (4, 2, NOW()), (4, 3, NOW()),
(4, 10, NOW()), (4, 11, NOW()), (4, 13, NOW()),
(5, 8, NOW()), (5, 9, NOW()), (5, 10, NOW()),
(6, 4, NOW()), (6, 7, NOW()), (6, 12, NOW()), (6, 13, NOW());

-- 公告
INSERT INTO notice (title, content, create_time, update_time) VALUES
('系统上线公告', '香水推荐系统正式上线！欢迎大家体验，有任何问题请联系管理员。', NOW(), NOW()),
('春季上新活动', '春日新品香水限时折扣，全场满 1000 减 200，活动截止 4 月 30 日。', NOW(), NOW()),
('关于用户隐私保护', '我们高度重视您的隐私，所有用户数据均加密存储，详情请查看隐私政策。', NOW(), NOW());

-- 订单
INSERT INTO orders (order_no, user_id, perfume_id, quantity, price, total_amount, status, receiver_name, receiver_phone, receiver_addr, create_time, update_time) VALUES
('P20261001001', 4, 1, 1, 1280.00, 1280.00, 'COMPLETED', '小资情调', '13800000011', '北京市朝阳区', NOW(), NOW()),
('P20261005002', 4, 10, 1, 1080.00, 1080.00, 'SHIPPED',   '小资情调', '13800000011', '北京市朝阳区', NOW(), NOW()),
('P20261010003', 5, 8, 2, 980.00, 1960.00, 'PAID',      '香水小白', '13800000012', '上海市浦东新区', NOW(), NOW()),
('P20261015004', 6, 4, 1, 980.00, 980.00,  'PENDING',   '香评大师', '13800000013', '广州市天河区', NOW(), NOW());

SELECT '香水推荐系统数据初始化完成' AS result;
