CREATE TABLE orders (
                        id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
                        order_no      VARCHAR(32)   NOT NULL COMMENT '订单号（业务唯一）',
                        user_id       BIGINT        NOT NULL COMMENT '下单用户 ID',
                        product_id    BIGINT        NOT NULL COMMENT '商品 ID',
                        activity_id   BIGINT        NULL     COMMENT '秒杀活动 ID，普通订单为 NULL',
                        quantity      INT           NOT NULL COMMENT '购买数量',
                        total_amount  DECIMAL(10,2) NOT NULL COMMENT '订单总金额（元）',
                        status        TINYINT       NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已取消',
                        create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        PRIMARY KEY (id),
                        UNIQUE KEY uk_order_no (order_no),
                        KEY idx_user_id (user_id),
                        KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';