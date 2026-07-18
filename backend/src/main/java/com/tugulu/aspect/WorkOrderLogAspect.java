package com.tugulu.aspect;

import com.tugulu.annotation.WorkOrderTransition;
import com.tugulu.service.WorkOrderLogService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Captures from/to status around annotated work-order transitions and writes logs after success.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class WorkOrderLogAspect {

    private static final ThreadLocal<Map<String, Object>> CTX = new ThreadLocal<>();

    private final WorkOrderLogService workOrderLogService;

    public static void setContext(Long orderId, String fromStatus, String toStatus, String remark) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", orderId);
        map.put("fromStatus", fromStatus);
        map.put("toStatus", toStatus);
        map.put("remark", remark);
        CTX.set(map);
    }

    @Before("@annotation(com.tugulu.annotation.WorkOrderTransition)")
    public void before(JoinPoint joinPoint) {
        CTX.remove();
    }

    @AfterReturning("@annotation(com.tugulu.annotation.WorkOrderTransition)")
    public void afterReturning(JoinPoint joinPoint) {
        Map<String, Object> map = CTX.get();
        if (map == null) {
            return;
        }
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            WorkOrderTransition ann = signature.getMethod().getAnnotation(WorkOrderTransition.class);
            String remark = map.get("remark") == null ? ann.remark() : String.valueOf(map.get("remark"));
            workOrderLogService.log(
                    (Long) map.get("orderId"),
                    (String) map.get("fromStatus"),
                    (String) map.get("toStatus"),
                    remark
            );
        } finally {
            CTX.remove();
        }
    }
}
