package com.example.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 账号状态变更：0 封禁，1 正常
 */
@Data
public class UpdateUserStatusDTO {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
