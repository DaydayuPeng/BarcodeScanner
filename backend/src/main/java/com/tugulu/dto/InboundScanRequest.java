package com.tugulu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class InboundScanRequest {
    @NotEmpty(message = "入库列表不能为空")
    @Valid
    private List<ScanItem> list;
    private String imageUrl;

    @Data
    public static class ScanItem {
        @jakarta.validation.constraints.NotBlank(message = "快递单号不能为空")
        private String trackingNo;
        private String shelfNo;
    }
}
