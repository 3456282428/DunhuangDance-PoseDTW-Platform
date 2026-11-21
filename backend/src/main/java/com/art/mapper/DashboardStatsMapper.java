package com.art.mapper;

import com.art.entity.ApiAccessLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计聚合 Mapper（知识库存储量 / 文档数量趋势）
 * 说明：table 参数由 Service 层白名单校验后传入，不接受前端直接输入
  * @author liukang&&heyan
 */
@Mapper
public interface DashboardStatsMapper {

    /** 单表条目统计：总数、自动归集数、手动数、正文与摘要字符总量、附件数、浏览总量 */
    Map<String, Object> selectDocStat(@Param("table") String table);

    /** 近 days 天按日新增趋势（含无数据的补零由 Service 处理） */
    List<Map<String, Object>> selectDailyTrend(@Param("table") String table, @Param("days") int days);

    /** 按来源类型分组统计（用于筛选视图） */
    List<Map<String, Object>> selectSourceTypeStat(@Param("table") String table);

    /** 按分类分组统计（用于筛选视图） */
    List<Map<String, Object>> selectCategoryStat(@Param("table") String table);

    /** information_schema 表物理占用（data_length + index_length） */
    List<Map<String, Object>> selectTableStorage(@Param("tables") List<String> tables);

    /** 数据库整体占用 */
    Map<String, Object> selectSchemaStorage(@Param("schema") String schema);

    // ==================== AI模型性能指标采样（真实业务数据） ====================

    /** 窗口内 AI 统一入口调用聚合：调用次数、平均知识库命中数、待接入/成功占比 */
    Map<String, Object> selectInvokeAgg(@Param("since") LocalDateTime since);

    /** 窗口内 AI 评分聚合：评分数、AI均分、教师均分、二者一致度（1-|差值|/100） */
    Map<String, Object> selectScorePerf(@Param("since") LocalDateTime since);

    /** 窗口内 AI 评分报告样本（用于解析 processingTime 计算真实推理耗时） */
    List<String> selectRecentAiReports(@Param("since") LocalDateTime since, @Param("limit") int limit);

    // ==================== API 调用与用量（api_access_log 真实访问留痕） ====================

    /** 写入一条接口访问留痕（由 ApiUsageInterceptor 调用） */
    int insertApiAccessLog(ApiAccessLog log);

    /** 窗口内接口调用汇总：总调用、平均耗时、成功率、端点数、调用人数、今日调用 */
    Map<String, Object> selectApiUsageSummary(@Param("since") LocalDateTime since);

    /** 按日调用量 / 平均耗时 / 失败数（traffic 折线数据源） */
    List<Map<String, Object>> selectApiDailyTraffic(@Param("since") LocalDateTime since);

    /** 星期×小时调用分布（punch-card 热力矩阵数据源） */
    List<Map<String, Object>> selectApiPunchCard(@Param("since") LocalDateTime since);

    /** 端点调用排行（次数 + 平均耗时 + 失败数） */
    List<Map<String, Object>> selectApiTopEndpoints(@Param("since") LocalDateTime since, @Param("limit") int limit);

    /** 调用业务分组占比 */
    List<Map<String, Object>> selectApiGroupShare(@Param("since") LocalDateTime since);

    /** 调用角色占比 */
    List<Map<String, Object>> selectApiRoleShare(@Param("since") LocalDateTime since);

    /** 窗口内 AI 统一入口调用的调用端分布 */
    List<Map<String, Object>> selectInvokeScopeStat(@Param("since") LocalDateTime since);
}
