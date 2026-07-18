package com.tugulu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tugulu.common.BusinessException;
import com.tugulu.entity.StatInbound;
import com.tugulu.entity.StatPacking;
import com.tugulu.entity.SysUser;
import com.tugulu.mapper.StatInboundMapper;
import com.tugulu.mapper.StatPackingMapper;
import com.tugulu.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StatInboundMapper statInboundMapper;
    private final StatPackingMapper statPackingMapper;
    private final SysUserMapper sysUserMapper;

    public List<Map<String, Object>> inboundStats(String startDate, String endDate) {
        LocalDate start = parseDate(startDate, "startDate");
        LocalDate end = parseDate(endDate, "endDate");
        if (end.isBefore(start)) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        List<StatInbound> list = statInboundMapper.selectList(new LambdaQueryWrapper<StatInbound>()
                .between(StatInbound::getStatDate, start, end)
                .orderByAsc(StatInbound::getStatDate));
        return list.stream().map(s -> {
            Map<String, Object> item = new HashMap<>();
            item.put("statDate", s.getStatDate().format(DATE_FMT));
            item.put("totalCount", s.getTotalCount());
            return item;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> packingStats(String statDate) {
        LocalDate date = parseDate(statDate, "statDate");
        List<StatPacking> list = statPackingMapper.selectList(new LambdaQueryWrapper<StatPacking>()
                .eq(StatPacking::getStatDate, date)
                .orderByDesc(StatPacking::getTotalWeight));
        return list.stream().map(s -> {
            Map<String, Object> item = new HashMap<>();
            SysUser user = sysUserMapper.selectById(s.getPackerId());
            item.put("packerName", user == null ? "-" : user.getRealName());
            item.put("customerId", s.getCustomerId());
            item.put("totalWeight", s.getTotalWeight());
            return item;
        }).collect(Collectors.toList());
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value, DATE_FMT);
        } catch (Exception e) {
            throw new BusinessException(field + " 格式应为 yyyy-MM-dd");
        }
    }
}
