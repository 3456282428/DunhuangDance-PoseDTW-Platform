-- =====================================================
-- API 调用与用量监控 · 数据库迁移脚本（migrate_api_usage.sql）
-- -----------------------------------------------------
-- 内容：
--   1) 接口访问留痕表 api_access_log（拦截器自动写入 /api/** 每次真实访问）
--   2) 供「API 调用与用量」可视化使用的索引
-- 说明：本表由 com.art.interceptor.ApiUsageInterceptor 自动写入，无需人工维护；
--       非破坏性脚本，可重复执行（IF NOT EXISTS）。
-- 环境：MySQL 8.0+，库 art_training
-- 用法：mysql -uroot art_training < migrate_api_usage.sql
-- =====================================================

USE art_training;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `api_access_log` (
  `log_id`      bigint       NOT NULL AUTO_INCREMENT COMMENT '访问日志ID',
  `endpoint`    varchar(200) NOT NULL COMMENT '接口端点模板（数字段归一为 {id}，如 /api/teacher/homework/{id}/ai-score）',
  `method`      varchar(10)  NOT NULL COMMENT '请求方法 GET/POST/PUT/DELETE',
  `api_group`   varchar(50)  NOT NULL COMMENT '业务分组：AI模型调用/AI动作评分/知识库管理/可视化仪表盘/身份认证/门户接口/教师业务接口/学生业务接口/管理业务接口/公共业务接口/其他接口',
  `username`    varchar(50)  DEFAULT NULL COMMENT '调用人用户名（匿名访问为 NULL）',
  `role_scope`  varchar(10)  DEFAULT NULL COMMENT '调用角色：ADMIN/TEACHER/STUDENT/ANON',
  `status`      int          NOT NULL DEFAULT '200' COMMENT 'HTTP响应状态码',
  `duration_ms` int          NOT NULL DEFAULT '0' COMMENT '服务端处理耗时(毫秒)',
  `access_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_apilog_time` (`access_time`),
  KEY `idx_apilog_endpoint` (`endpoint`),
  KEY `idx_apilog_group` (`api_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='接口访问留痕（API调用与用量数据源）';
