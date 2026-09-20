package com.example.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


import java.time.LocalDateTime;
@Data
@TableName("users")
public class Users {
    @TableId(type= IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private Integer role;
    private Integer status;
    private String sex;
    private Integer age;
    private String address;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
