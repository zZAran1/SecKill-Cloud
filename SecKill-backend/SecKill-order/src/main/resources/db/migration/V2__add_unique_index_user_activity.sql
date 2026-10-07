-- 添加用户-活动联合唯一索引，防止同一用户重复秒杀同一活动
ALTER TABLE orders ADD UNIQUE KEY uk_user_activity (user_id, activity_id);