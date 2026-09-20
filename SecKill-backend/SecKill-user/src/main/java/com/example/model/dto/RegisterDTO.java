package com.example.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "用户名不能为空")
    @Size(min=2,max=20,message="用户名长度必须在2-20个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min=6,max=20,message="密码长度必须在6-20个字符之间")
    private String password;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp="^1[3-9]\\d{9}$",message="手机号格式不正确")
    private String phone;

    @NotNull(message = "年龄不能为空")
    @Min(value = 1,message="年龄必须在1~120之间")
    @Max(value = 120 ,message="年龄必须在1~120之间")
    private Integer age;

    @NotBlank(message = "性别不能为空")
    private String sex;
}
