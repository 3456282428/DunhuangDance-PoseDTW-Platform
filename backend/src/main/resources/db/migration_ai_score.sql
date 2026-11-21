-- =====================================================
-- 舞蹈AI智能评分功能 - 数据库迁移脚本
-- 功能：作业模块集成AI评分
--   1. homework 表增加教师参考视频字段（AI评分基准）
--   2. homework_submission 表增加AI评分结果字段
-- 执行方式：mysql -u<user> -p <database> < migration_ai_score.sql
-- 日期：2026-10-12
-- =====================================================

-- 1. 作业表：教师参考视频（AI评分基准）
ALTER TABLE `homework`
  ADD COLUMN `ref_video_url` VARCHAR(500) DEFAULT NULL COMMENT '教师参考视频URL（AI评分基准）' AFTER `content`,
  ADD COLUMN `ref_video_name` VARCHAR(255) DEFAULT NULL COMMENT '教师参考视频原始文件名' AFTER `ref_video_url`;

-- 2. 作业提交表：AI评分结果
ALTER TABLE `homework_submission`
  ADD COLUMN `ai_score` DECIMAL(5,2) DEFAULT NULL COMMENT 'AI智能评分总分' AFTER `teacher_comment`,
  ADD COLUMN `ai_report` TEXT DEFAULT NULL COMMENT 'AI评分报告JSON（部位分/维度分/建议）' AFTER `ai_score`,
  ADD COLUMN `ai_status` VARCHAR(20) DEFAULT NULL COMMENT 'AI评分状态：评分中/已完成/失败' AFTER `ai_report`,
  ADD COLUMN `ai_scored_at` DATETIME DEFAULT NULL COMMENT 'AI评分完成时间' AFTER `ai_status`;
