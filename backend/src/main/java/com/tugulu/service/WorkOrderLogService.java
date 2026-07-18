package com.tugulu.service;

import com.tugulu.common.UserContext;
import com.tugulu.entity.WorkOrderLog;
import com.tugulu.mapper.WorkOrderLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WorkOrderLogService {

    private final WorkOrderLogMapper workOrderLogMapper;

    public void log(Long orderId, String fromStatus, String toStatus, String remark) {
        WorkOrderLog log = new WorkOrderLog();
        log.setOrderId(orderId);
        log.setOperatorId(UserContext.getUserId());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(remark);
        log.setOperateTime(LocalDateTime.now());
        workOrderLogMapper.insert(log);
    }
}
