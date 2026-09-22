CREATE TABLE IF NOT EXISTS users (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50) NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    role        INT NOT NULL DEFAULT 1 COMMENT '角色',
    status      INT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
    sex         VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    age         INT          DEFAULT NULL COMMENT '年龄',
    address     VARCHAR(255) DEFAULT NULL COMMENT '地址',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY idx_username (username),
    UNIQUE KEY idx_phone (phone),
    UNIQUE KEY idx_email (email)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';