package com.example.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**提交退款申请*/
@Data
public class CreateRefundDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotBlank(message = "退款原因不能为空")
    @Size(max = 255, message = "退款原因不能超过 255 个字符")
    private String reason;
}
