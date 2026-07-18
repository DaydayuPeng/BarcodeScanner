package com.tugulu.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum WorkOrderStatus {
    PENDING_PREP("待备货"),
    PENDING_PACK("待打包"),
    PACKED("已打包"),
    PENDING_SEAL("待封箱"),
    PENDING_SHIP("待发货"),
    COMPLETED("已完成");

    private final String label;

    WorkOrderStatus(String label) {
        this.label = label;
    }

    private static final Map<String, WorkOrderStatus> MAP =
            Arrays.stream(values()).collect(Collectors.toMap(Enum::name, e -> e));

    public static WorkOrderStatus of(String code) {
        WorkOrderStatus status = MAP.get(code);
        if (status == null) {
            throw new IllegalArgumentException("未知工单状态: " + code);
        }
        return status;
    }

    public static String labelOf(String code) {
        WorkOrderStatus status = MAP.get(code);
        return status == null ? code : status.getLabel();
    }
}
