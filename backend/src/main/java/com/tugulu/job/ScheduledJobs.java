package com.tugulu.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tugulu.entity.InboundOrder;
import com.tugulu.entity.StatInbound;
import com.tugulu.mapper.InboundOrderMapper;
import com.tugulu.mapper.StatInboundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledJobs {

    private final InboundOrderMapper inboundOrderMapper;
    private final StatInboundMapper statInboundMapper;

    /** 每日凌晨 1 点汇总昨日入库件数 */
    @Scheduled(cron = "0 0 1 * * ?")
    public void aggregateYesterdayInbound() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        int count = inboundOrderMapper.countByDate(yesterday.toString());
        StatInbound existing = statInboundMapper.selectOne(new LambdaQueryWrapper<StatInbound>()
                .eq(StatInbound::getStatDate, yesterday));
        if (existing == null) {
            StatInbound stat = new StatInbound();
            stat.setStatDate(yesterday);
            stat.setTotalCount(count);
            statInboundMapper.insert(stat);
        } else {
            existing.setTotalCount(count);
            statInboundMapper.updateById(existing);
        }
        log.info("Aggregated inbound stats for {}: {}", yesterday, count);
    }

    /** 每日凌晨 2 点清理超 12 个月且已逻辑删除的入库记录 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredInbound() {
        LocalDateTime threshold = LocalDateTime.now().minusMonths(12);
        int deleted = inboundOrderMapper.delete(new LambdaQueryWrapper<InboundOrder>()
                .eq(InboundOrder::getStatus, 0)
                .lt(InboundOrder::getInboundTime, threshold));
        log.info("Cleaned expired inbound records before {}: {}", threshold, deleted);
    }
}
