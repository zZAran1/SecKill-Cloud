package com.example.model.vo;

import lombok.Data;

@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String sex;
    private Integer age;
    private String phone;
    private String email;
    private Integer role;
    private Integer status;
}
