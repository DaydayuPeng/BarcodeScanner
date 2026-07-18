package com.tugulu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stat_packing")
public class StatPacking {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Long packerId;
    private String customerId;
    private BigDecimal totalWeight;
    private LocalDateTime updateTime;
}
