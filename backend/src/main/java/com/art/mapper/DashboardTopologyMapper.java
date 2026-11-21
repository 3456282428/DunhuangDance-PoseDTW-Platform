package com.art.mapper;

import com.art.entity.DashboardTopology;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 仪表盘拓扑关系 Mapper
  * @author liukang&&heyan
 */
@Mapper
public interface DashboardTopologyMapper {

    List<DashboardTopology> selectAll();
}
