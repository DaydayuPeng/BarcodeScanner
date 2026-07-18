package com.tugulu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inbound_order")
public class InboundOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String trackingNo;
    private String shelfNo;
    private String imageUrl;
    private LocalDateTime inboundTime;
    private String createBy;
    private Integer status;
}
