package com.tugulu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConfirmShipRequest {
    @NotNull(message = "件数不能为空")
    @Min(value = 1, message = "件数必须大于0")
    private Integer quantity;

    @NotNull(message = "重量不能为空")
    @DecimalMin(value = "0.01", message = "重量必须大于0")
    private BigDecimal weight;

    @NotNull(message = "体积不能为空")
    @DecimalMin(value = "0.01", message = "体积必须大于0")
    private BigDecimal volume;
}
