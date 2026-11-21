package com.art.mapper;

import com.art.entity.DashboardOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 仪表盘管理操作日志 Mapper
  * @author liukang&&heyan
 */
@Mapper
public interface DashboardOperationLogMapper {

    int insert(DashboardOperationLog log);

    List<DashboardOperationLog> selectRecent(@Param("limit") Integer limit);
}
