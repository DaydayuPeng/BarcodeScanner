package com.tugulu.controller;

import com.tugulu.common.ApiResponse;
import com.tugulu.dto.CmsUpdateRequest;
import com.tugulu.service.CmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cms")
@RequiredArgsConstructor
public class CmsController {

    private final CmsService cmsService;

    @GetMapping("/{type}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String type) {
        return ApiResponse.ok(cmsService.getByType(type));
    }

    @PutMapping("/{type}")
    public ApiResponse<Void> update(@PathVariable String type, @RequestBody CmsUpdateRequest request) {
        cmsService.update(type, request);
        return ApiResponse.okMsg("保存成功");
    }
}
