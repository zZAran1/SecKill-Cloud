-- MySQL 容器首次启动时自动执行（仅在数据卷为空时运行一次）
-- 创建三个业务库：每个服务只连自己的库，避免 Flyway 版本冲突
CREATE DATABASE IF NOT EXISTS seckill_user DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS seckill_product DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS seckill_order DEFAULT CHARACTER SET utf8mb4;

-- 业务专用账号（密码必须与 .env 里的 DB_PASSWORD 保持一致）
CREATE USER IF NOT EXISTS 'seckill'@'%' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON seckill_user.* TO 'seckill'@'%';
GRANT ALL PRIVILEGES ON seckill_product.* TO 'seckill'@'%';
GRANT ALL PRIVILEGES ON seckill_order.* TO 'seckill'@'%';
FLUSH PRIVILEGES;