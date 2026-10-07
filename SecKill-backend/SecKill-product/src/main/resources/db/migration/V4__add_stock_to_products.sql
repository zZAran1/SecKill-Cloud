-- 添加普通商品库存字段
ALTER TABLE products ADD COLUMN stock INT NOT NULL DEFAULT 0 COMMENT '普通商品库存';

-- 初始化现有商品库存为 100（根据业务调整）
UPDATE products SET stock = 100 WHERE stock = 0;