-- =====================================================
-- 水稻产品安全质量检测系统 - 数据库初始化脚本
-- =====================================================

-- 创建数据库（如不存在）
-- CREATE DATABASE IF NOT EXISTS rice_quality DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE rice_quality;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    enabled TINYINT(1) DEFAULT 1 COMMENT '1-启用 0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称，如 ROLE_ADMIN, ROLE_USER',
    description VARCHAR(255),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ==================== 初始化数据 ====================

-- 插入默认角色
INSERT INTO roles (name, description) VALUES ('ROLE_ADMIN', '管理员');
INSERT INTO roles (name, description) VALUES ('ROLE_MANAGER', '经理');
INSERT INTO roles (name, description) VALUES ('ROLE_USER', '普通用户');

-- 插入默认管理员（密码为 bcrypt 加密的 "admin123"）
-- 注意：生产环境务必修改密码！
INSERT INTO users (username, password, email, enabled) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@example.com', 1),
('user', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user@example.com', 1);

-- 分配角色
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1),  -- admin -> ROLE_ADMIN
(1, 3),  -- admin -> ROLE_USER
(2, 3);  -- user -> ROLE_USER
