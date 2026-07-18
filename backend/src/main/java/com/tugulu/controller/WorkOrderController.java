package com.tugulu.controller;

import com.tugulu.common.ApiResponse;
import com.tugulu.dto.ConfirmShipRequest;
import com.tugulu.dto.QuantityRequest;
import com.tugulu.dto.ReplenishRequest;
import com.tugulu.dto.WorkOrderCreateRequest;
import com.tugulu.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-order")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(workOrderService.list(status, page, size));
    }

    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody WorkOrderCreateRequest request) {
        return ApiResponse.ok("工单创建成功", workOrderService.create(request));
    }

    @PutMapping("/{orderId}/start-pack")
    public ApiResponse<Void> startPack(@PathVariable Long orderId) {
        workOrderService.startPack(orderId);
        return ApiResponse.okMsg("已转为待打包");
    }

    @PutMapping("/{orderId}/confirm-pack")
    public ApiResponse<Void> confirmPack(@PathVariable Long orderId, @Valid @RequestBody QuantityRequest request) {
        String msg = workOrderService.confirmPack(orderId, request);
        return ApiResponse.okMsg(msg);
    }

    @PutMapping("/{orderId}/replenish")
    public ApiResponse<Void> replenish(@PathVariable Long orderId, @Valid @RequestBody ReplenishRequest request) {
        String msg = workOrderService.replenish(orderId, request);
        return ApiResponse.okMsg(msg);
    }

    @PutMapping("/{orderId}/seal")
    public ApiResponse<Void> seal(@PathVariable Long orderId) {
        workOrderService.seal(orderId);
        return ApiResponse.okMsg("已转为待封箱");
    }

    @PutMapping("/{orderId}/confirm-ship")
    public ApiResponse<Void> confirmShip(@PathVariable Long orderId, @Valid @RequestBody ConfirmShipRequest request) {
        workOrderService.confirmShip(orderId, request);
        return ApiResponse.okMsg("发货确认成功");
    }

    @PutMapping("/{orderId}/complete")
    public ApiResponse<Void> complete(@PathVariable Long orderId) {
        workOrderService.complete(orderId);
        return ApiResponse.okMsg("工单已完成");
    }

    @GetMapping("/{orderId}/logs")
    public ApiResponse<List<Map<String, Object>>> logs(@PathVariable Long orderId) {
        return ApiResponse.ok(workOrderService.logs(orderId));
    }
}
