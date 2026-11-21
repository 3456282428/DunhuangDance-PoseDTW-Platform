package com.art.mapper;

import com.art.entity.RescheduleRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface RescheduleRequestMapper {
    int insert(RescheduleRequest request);
    RescheduleRequest selectById(@Param("requestId") Integer requestId);
    int update(RescheduleRequest request);
    List<RescheduleRequest> selectByScheduleId(@Param("scheduleId") Integer scheduleId);
    List<RescheduleRequest> selectAll();
}
