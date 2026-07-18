package com.tugulu.controller;

import com.tugulu.common.ApiResponse;
import com.tugulu.dto.InboundQueryRequest;
import com.tugulu.dto.InboundScanRequest;
import com.tugulu.service.InboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inbound")
@RequiredArgsConstructor
public class InboundController {

    private final InboundService inboundService;

    @PostMapping("/query")
    public ApiResponse<List<Map<String, Object>>> query(@RequestBody InboundQueryRequest request) {
        return ApiResponse.ok("查询成功", inboundService.query(request));
    }

    @PostMapping("/scan")
    public ApiResponse<Map<String, Object>> scan(@Valid @RequestBody InboundScanRequest request) {
        Map<String, Object> data = inboundService.scan(request);
        int success = (int) data.get("successCount");
        return ApiResponse.ok("全部入库成功，共 " + success + " 件", data);
    }
}
