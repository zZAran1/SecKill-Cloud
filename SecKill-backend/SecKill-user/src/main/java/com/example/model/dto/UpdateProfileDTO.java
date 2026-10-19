package com.example.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人资料修改：所有字段可选，只更新传入的字段
 */
@Data
public class UpdateProfileDTO {

    @Size(max = 50, message = "用户名不能超过50个字")
    private String username;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不合法")
    private String email;

    @Size(max = 10, message = "性别长度不合法")
    private String sex;

    @Min(value = 0, message = "年龄不合法")
    @Max(value = 150, message = "年龄不合法")
    private Integer age;

    @Size(max = 255, message = "地址不能超过255个字")
    private String address;
}
