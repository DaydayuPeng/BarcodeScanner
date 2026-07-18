package com.tugulu.dto;

import lombok.Data;

import java.util.List;

@Data
public class InboundQueryRequest {
    private List<String> trackingNos;
    private String keyword;
}
