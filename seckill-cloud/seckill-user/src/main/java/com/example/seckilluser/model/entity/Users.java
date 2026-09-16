package com.example.seckilluser.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("users")
public class Users {
    @TableId
    private Integer id;
    private String email;
    private String password;
    private String username;
    private String phone;
    private String sex;
    private int age;
    private Integer status;
    private Integer role;
    private LocalDateTime create_time;
    private LocalDateTime update_time;
}
