package com.tugulu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tugulu.annotation.WorkOrderTransition;
import com.tugulu.aspect.WorkOrderLogAspect;
import com.tugulu.common.BusinessException;
import com.tugulu.common.UserContext;
import com.tugulu.dto.ConfirmShipRequest;
import com.tugulu.dto.QuantityRequest;
import com.tugulu.dto.ReplenishRequest;
import com.tugulu.dto.WorkOrderCreateRequest;
import com.tugulu.entity.StatPacking;
import com.tugulu.entity.SysUser;
import com.tugulu.entity.WorkOrder;
import com.tugulu.entity.WorkOrderLog;
import com.tugulu.enums.WorkOrderStatus;
import com.tugulu.mapper.StatPackingMapper;
import com.tugulu.mapper.SysUserMapper;
import com.tugulu.mapper.WorkOrderLogMapper;
import com.tugulu.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderLogMapper workOrderLogMapper;
    private final SysUserMapper sysUserMapper;
    private final StatPackingMapper statPackingMapper;

    public Map<String, Object> list(String status, int page, int size) {
        Page<WorkOrder> p = workOrderMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<WorkOrder>()
                        .eq(status != null && !status.isBlank(), WorkOrder::getStatus, status)
                        .ne(status == null || status.isBlank(), WorkOrder::getStatus, WorkOrderStatus.COMPLETED.name())
                        .orderByDesc(WorkOrder::getCreatedAt)
        );
        List<Map<String, Object>> records = p.getRecords().stream().map(this::toListItem).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("total", p.getTotal());
        data.put("records", records);
        return data;
    }

    @Transactional
    @WorkOrderTransition(remark = "创建工单")
    public Map<String, Object> create(WorkOrderCreateRequest request) {
        WorkOrder order = new WorkOrder();
        order.setCustomerId(request.getCustomerId().trim());
        order.setBatchNo(request.getBatchNo().trim());
        order.setStatus(WorkOrderStatus.PENDING_PREP.name());
        order.setTotalQuantity(0);
        order.setCreateBy(UserContext.getUserId());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setVersion(0);
        workOrderMapper.insert(order);
        WorkOrderLogAspect.setContext(order.getId(), null, WorkOrderStatus.PENDING_PREP.name(), "创建工单");
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getId());
        return data;
    }

    @Transactional
    @WorkOrderTransition
    public void startPack(Long orderId) {
        WorkOrder order = requireOrder(orderId);
        requireStatus(order, WorkOrderStatus.PENDING_PREP);
        String from = order.getStatus();
        order.setStatus(WorkOrderStatus.PENDING_PACK.name());
        updateWithOptimisticLock(order);
        WorkOrderLogAspect.setContext(orderId, from, WorkOrderStatus.PENDING_PACK.name(), "确认备货");
    }

    @Transactional
    @WorkOrderTransition
    public String confirmPack(Long orderId, QuantityRequest request) {
        WorkOrder order = requireOrder(orderId);
        requireStatus(order, WorkOrderStatus.PENDING_PACK);
        String from = order.getStatus();
        order.setStatus(WorkOrderStatus.PACKED.name());
        order.setTotalQuantity(request.getQuantity());
        order.setPackerId(UserContext.getUserId());
        updateWithOptimisticLock(order);
        WorkOrderLogAspect.setContext(orderId, from, WorkOrderStatus.PACKED.name(),
                "打包确认，件数=" + request.getQuantity());
        return "打包成功，当前总件数：" + order.getTotalQuantity();
    }

    @Transactional
    @WorkOrderTransition
    public String replenish(Long orderId, ReplenishRequest request) {
        WorkOrder order = requireOrder(orderId);
        requireStatus(order, WorkOrderStatus.PACKED);
        String from = order.getStatus();
        int total = (order.getTotalQuantity() == null ? 0 : order.getTotalQuantity()) + request.getAdditionalQuantity();
        order.setTotalQuantity(total);
        updateWithOptimisticLock(order);
        WorkOrderLogAspect.setContext(orderId, from, WorkOrderStatus.PACKED.name(),
                "补货件数=" + request.getAdditionalQuantity());
        return "补货成功，当前总件数累加至：" + total;
    }

    @Transactional
    @WorkOrderTransition
    public void seal(Long orderId) {
        WorkOrder order = requireOrder(orderId);
        requireStatus(order, WorkOrderStatus.PACKED);
        String from = order.getStatus();
        order.setStatus(WorkOrderStatus.PENDING_SEAL.name());
        updateWithOptimisticLock(order);
        WorkOrderLogAspect.setContext(orderId, from, WorkOrderStatus.PENDING_SEAL.name(), "封箱");
    }

    @Transactional
    @WorkOrderTransition
    public void confirmShip(Long orderId, ConfirmShipRequest request) {
        WorkOrder order = requireOrder(orderId);
        requireStatus(order, WorkOrderStatus.PENDING_SEAL);
        String from = order.getStatus();
        order.setStatus(WorkOrderStatus.PENDING_SHIP.name());
        order.setTotalQuantity(request.getQuantity());
        order.setWeight(request.getWeight());
        order.setVolume(request.getVolume());
        updateWithOptimisticLock(order);
        upsertPackingStat(order, request.getWeight());
        WorkOrderLogAspect.setContext(orderId, from, WorkOrderStatus.PENDING_SHIP.name(),
                "确认发货，重量=" + request.getWeight() + ",体积=" + request.getVolume());
    }

    @Transactional
    @WorkOrderTransition
    public void complete(Long orderId) {
        WorkOrder order = requireOrder(orderId);
        requireStatus(order, WorkOrderStatus.PENDING_SHIP);
        String from = order.getStatus();
        order.setStatus(WorkOrderStatus.COMPLETED.name());
        updateWithOptimisticLock(order);
        WorkOrderLogAspect.setContext(orderId, from, WorkOrderStatus.COMPLETED.name(), "完成工单");
    }

    public List<Map<String, Object>> logs(Long orderId) {
        requireOrder(orderId);
        List<WorkOrderLog> logs = workOrderLogMapper.selectList(new LambdaQueryWrapper<WorkOrderLog>()
                .eq(WorkOrderLog::getOrderId, orderId)
                .orderByAsc(WorkOrderLog::getOperateTime));
        return logs.stream().map(log -> {
            Map<String, Object> item = new HashMap<>();
            SysUser user = sysUserMapper.selectById(log.getOperatorId());
            item.put("operatorName", user == null ? "-" : user.getRealName());
            item.put("fromStatus", log.getFromStatus() == null ? "" : WorkOrderStatus.labelOf(log.getFromStatus()));
            item.put("toStatus", WorkOrderStatus.labelOf(log.getToStatus()));
            item.put("remark", log.getRemark() == null ? "" : log.getRemark());
            item.put("operateTime", log.getOperateTime() == null ? null : log.getOperateTime().format(FORMATTER));
            return item;
        }).collect(Collectors.toList());
    }

    private void upsertPackingStat(WorkOrder order, BigDecimal weight) {
        Long packerId = order.getPackerId() == null ? UserContext.getUserId() : order.getPackerId();
        LocalDate today = LocalDate.now();
        StatPacking stat = statPackingMapper.selectOne(new LambdaQueryWrapper<StatPacking>()
                .eq(StatPacking::getStatDate, today)
                .eq(StatPacking::getPackerId, packerId)
                .eq(StatPacking::getCustomerId, order.getCustomerId()));
        if (stat == null) {
            stat = new StatPacking();
            stat.setStatDate(today);
            stat.setPackerId(packerId);
            stat.setCustomerId(order.getCustomerId());
            stat.setTotalWeight(weight);
            statPackingMapper.insert(stat);
        } else {
            BigDecimal current = stat.getTotalWeight() == null ? BigDecimal.ZERO : stat.getTotalWeight();
            stat.setTotalWeight(current.add(weight));
            statPackingMapper.updateById(stat);
        }
    }

    private WorkOrder requireOrder(Long orderId) {
        WorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("工单不存在");
        }
        return order;
    }

    private void requireStatus(WorkOrder order, WorkOrderStatus expected) {
        if (!Objects.equals(order.getStatus(), expected.name())) {
            throw new BusinessException("当前状态不允许该操作，当前为: " + WorkOrderStatus.labelOf(order.getStatus()));
        }
    }

    private void updateWithOptimisticLock(WorkOrder order) {
        order.setUpdatedAt(LocalDateTime.now());
        int rows = workOrderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException(409, "工单已被他人更新，请刷新后重试");
        }
    }

    private Map<String, Object> toListItem(WorkOrder order) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", order.getId());
        item.put("customerId", order.getCustomerId());
        item.put("batchNo", order.getBatchNo());
        item.put("status", order.getStatus());
        item.put("totalQuantity", order.getTotalQuantity());
        item.put("weight", order.getWeight());
        item.put("volume", order.getVolume());
        String packerName = null;
        if (order.getPackerId() != null) {
            SysUser packer = sysUserMapper.selectById(order.getPackerId());
            packerName = packer == null ? null : packer.getRealName();
        }
        item.put("packerName", packerName);
        item.put("createdAt", order.getCreatedAt() == null ? null : order.getCreatedAt().format(FORMATTER));
        return item;
    }
}
