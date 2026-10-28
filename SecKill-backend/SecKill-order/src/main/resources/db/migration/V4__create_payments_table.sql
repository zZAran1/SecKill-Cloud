CREATE TABLE payments (
                           id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
                           payment_no  VARCHAR(32)   NOT NULL COMMENT '支付单号（业务唯一）',
                           order_no    VARCHAR(32)   NOT NULL COMMENT '关联订单号',
                           user_id     BIGINT        NOT NULL COMMENT '支付用户 ID',
                           amount      DECIMAL(10,2) NOT NULL COMMENT '支付金额（元）',
                           channel     VARCHAR(16)   NOT NULL DEFAULT 'MOCK' COMMENT '支付渠道 MOCK/ALIPAY/WXPAY',
                           status      TINYINT       NOT NULL DEFAULT 0 COMMENT '0待支付 1支付成功 2支付失败 3已关闭',
                           trade_no    VARCHAR(64)   NULL COMMENT '第三方支付流水号',
                           create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           PRIMARY KEY (id),
                           UNIQUE KEY uk_payment_no (payment_no),
                           UNIQUE KEY uk_order_no (order_no),
                           KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';
