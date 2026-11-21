package com.art.mapper;

import com.art.entity.OpenComponent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 开源组件登记 Mapper
  * @author liukang&&heyan
 */
@Mapper
public interface OpenComponentMapper {

    List<OpenComponent> selectList(@Param("platform") String platform,
                                   @Param("category") String category,
                                   @Param("updateStatus") String updateStatus,
                                   @Param("keyword") String keyword);

    OpenComponent selectById(@Param("componentId") Integer componentId);

    List<OpenComponent> selectSyncEnabled();

    int insert(OpenComponent component);

    int update(OpenComponent component);

    /** 同步结果回写：远端最新版本、更新状态、统计量、同步状态 */
    int updateSyncResult(@Param("componentId") Integer componentId,
                         @Param("latestVersion") String latestVersion,
                         @Param("latestReleaseDate") LocalDate latestReleaseDate,
                         @Param("updateStatus") String updateStatus,
                         @Param("starCount") Integer starCount,
                         @Param("forkCount") Integer forkCount,
                         @Param("openIssueCount") Integer openIssueCount,
                         @Param("description") String description,
                         @Param("homepage") String homepage,
                         @Param("syncStatus") String syncStatus,
                         @Param("syncMessage") String syncMessage);

    int deleteById(@Param("componentId") Integer componentId);

    int countAll();

    int countByPlatform(@Param("platform") String platform);

    int countByUpdateStatus(@Param("updateStatus") String updateStatus);
}
