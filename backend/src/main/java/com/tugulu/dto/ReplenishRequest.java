package com.tugulu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReplenishRequest {
    @NotNull(message = "补货件数不能为空")
    @Min(value = 1, message = "补货件数必须大于0")
    private Integer additionalQuantity;
}
