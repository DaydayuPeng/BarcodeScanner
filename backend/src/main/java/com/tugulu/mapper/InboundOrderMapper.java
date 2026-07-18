package com.tugulu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tugulu.entity.InboundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InboundOrderMapper extends BaseMapper<InboundOrder> {

    @Select("SELECT COUNT(*) FROM inbound_order WHERE status = 1 AND DATE(inbound_time) = #{statDate}")
    int countByDate(@Param("statDate") String statDate);
}
