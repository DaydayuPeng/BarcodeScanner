package com.tugulu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tugulu.common.BusinessException;
import com.tugulu.common.UserContext;
import com.tugulu.dto.InboundQueryRequest;
import com.tugulu.dto.InboundScanRequest;
import com.tugulu.entity.InboundOrder;
import com.tugulu.entity.StatInbound;
import com.tugulu.mapper.InboundOrderMapper;
import com.tugulu.mapper.StatInboundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InboundService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final InboundOrderMapper inboundOrderMapper;
    private final StatInboundMapper statInboundMapper;

    public List<Map<String, Object>> query(InboundQueryRequest request) {
        boolean hasNos = !CollectionUtils.isEmpty(request.getTrackingNos());
        boolean hasKeyword = StringUtils.hasText(request.getKeyword());
        if (!hasNos && !hasKeyword) {
            throw new BusinessException("trackingNos 和 keyword 必须二选一");
        }
        if (hasNos && hasKeyword) {
            throw new BusinessException("trackingNos 和 keyword 只能传其中一个");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        if (hasKeyword) {
            String keyword = request.getKeyword().trim();
            if (keyword.length() < 6) {
                throw new BusinessException("keyword 长度必须 >= 6");
            }
            List<InboundOrder> list = inboundOrderMapper.selectList(new LambdaQueryWrapper<InboundOrder>()
                    .eq(InboundOrder::getStatus, 1)
                    .likeRight(InboundOrder::getTrackingNo, keyword)
                    .orderByDesc(InboundOrder::getInboundTime));
            if (list.isEmpty()) {
                Map<String, Object> item = new HashMap<>();
                item.put("trackingNo", keyword);
                item.put("status", 0);
                item.put("message", "该单号未入库");
                result.add(item);
            } else {
                list.forEach(o -> result.add(toHit(o)));
            }
            return result;
        }

        List<String> trackingNos = request.getTrackingNos().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (trackingNos.isEmpty()) {
            throw new BusinessException("trackingNos 不能为空");
        }
        List<InboundOrder> found = inboundOrderMapper.selectList(new LambdaQueryWrapper<InboundOrder>()
                .eq(InboundOrder::getStatus, 1)
                .in(InboundOrder::getTrackingNo, trackingNos));
        Map<String, InboundOrder> foundMap = found.stream()
                .collect(Collectors.toMap(InboundOrder::getTrackingNo, o -> o, (a, b) -> a, LinkedHashMap::new));
        for (String no : trackingNos) {
            InboundOrder order = foundMap.get(no);
            if (order != null) {
                result.add(toHit(order));
            } else {
                Map<String, Object> miss = new HashMap<>();
                miss.put("trackingNo", no);
                miss.put("status", 0);
                miss.put("message", "该单号未入库");
                result.add(miss);
            }
        }
        return result;
    }

    @Transactional
    public Map<String, Object> scan(InboundScanRequest request) {
        int successCount = 0;
        List<Map<String, String>> failList = new ArrayList<>();
        String operator = UserContext.getRealName();
        for (InboundScanRequest.ScanItem item : request.getList()) {
            String trackingNo = item.getTrackingNo().trim();
            Long exists = inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrder>()
                    .eq(InboundOrder::getTrackingNo, trackingNo)
                    .eq(InboundOrder::getStatus, 1));
            if (exists != null && exists > 0) {
                Map<String, String> fail = new HashMap<>();
                fail.put("trackingNo", trackingNo);
                fail.put("reason", "单号已存在");
                failList.add(fail);
                continue;
            }
            InboundOrder order = new InboundOrder();
            order.setTrackingNo(trackingNo);
            order.setShelfNo(item.getShelfNo());
            order.setImageUrl(request.getImageUrl());
            order.setInboundTime(LocalDateTime.now());
            order.setCreateBy(operator);
            order.setStatus(1);
            inboundOrderMapper.insert(order);
            successCount++;
        }
        if (successCount > 0) {
            bumpTodayInboundStat(successCount);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("successCount", successCount);
        data.put("failList", failList);
        return data;
    }

    private void bumpTodayInboundStat(int delta) {
        LocalDate today = LocalDate.now();
        StatInbound stat = statInboundMapper.selectOne(new LambdaQueryWrapper<StatInbound>()
                .eq(StatInbound::getStatDate, today));
        if (stat == null) {
            stat = new StatInbound();
            stat.setStatDate(today);
            stat.setTotalCount(delta);
            statInboundMapper.insert(stat);
        } else {
            stat.setTotalCount((stat.getTotalCount() == null ? 0 : stat.getTotalCount()) + delta);
            statInboundMapper.updateById(stat);
        }
    }

    private Map<String, Object> toHit(InboundOrder order) {
        Map<String, Object> item = new HashMap<>();
        item.put("trackingNo", order.getTrackingNo());
        item.put("inboundTime", order.getInboundTime() == null ? null : order.getInboundTime().format(FORMATTER));
        item.put("shelfNo", order.getShelfNo());
        item.put("imageUrl", order.getImageUrl());
        item.put("status", 1);
        return item;
    }
}
