create table if not exists seckill_activity(
    id bigint primary key auto_increment comment'主键',
    product_id bigint comment '关联商品',
    seckill_price decimal(10,2) comment'秒杀价',
    stock int comment'秒杀专用库存',
    start_time datetime comment '活动开始时间',
    end_time datetime comment'活动结束时间',
    status int not null default 1 comment'0-下线，1-上线',
    version int not null default 0 comment'乐观锁',
    create_time datetime not null default current_timestamp comment'创建时间',
    update_time datetime not null default current_timestamp on update current_timestamp comment'更新时间'
    ) engine = InnoDB default charset = utf8mb4 comment ='秒杀活动表';