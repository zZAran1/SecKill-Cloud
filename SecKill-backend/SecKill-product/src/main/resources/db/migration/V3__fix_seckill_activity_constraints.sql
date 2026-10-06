alter table seckill_activity
    modify product_id bigint not null comment'关联商品',
    modify seckill_price decimal(10,2) not null comment'秒杀价格',
    modify stock int not null comment'秒杀专用库存',
    modify start_time datetime not null comment'开始时间',
    modify end_time datetime not null comment'结束时间',
    add key idx_product_id(product_id),
    add key idx_start_time(start_time);