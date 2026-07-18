package com.tugulu.controller;

import com.tugulu.common.ApiResponse;
import com.tugulu.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stat")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @GetMapping("/inbound")
    public ApiResponse<List<Map<String, Object>>> inbound(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ApiResponse.ok(statService.inboundStats(startDate, endDate));
    }

    @GetMapping("/packing")
    public ApiResponse<List<Map<String, Object>>> packing(@RequestParam String statDate) {
        return ApiResponse.ok(statService.packingStats(statDate));
    }
}
