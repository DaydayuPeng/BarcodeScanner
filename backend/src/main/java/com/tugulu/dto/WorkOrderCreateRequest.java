package com.tugulu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkOrderCreateRequest {
    @NotBlank(message = "客户编号不能为空")
    private String customerId;
    @NotBlank(message = "批次号不能为空")
    private String batchNo;
}
