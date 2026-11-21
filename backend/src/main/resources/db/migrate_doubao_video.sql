-- =====================================================
-- 豆包视频模型预留配置（勿并入seed_clean.sql，仅对现库执行）
-- 注册豆包视频生成模型（火山引擎方舟Ark），默认DISABLED：
-- 仅完成接口定义/参数规范/调用入口预留，不实现具体生成业务逻辑
-- 执行：mysql -uroot -p art_training < migrate_doubao_video.sql
-- =====================================================
USE art_training;

INSERT INTO `ai_model` (`model_name`, `model_type`, `provider`, `endpoint`, `description`, `status`)
SELECT 'doubao-seedance-video', 'VIDEO', 'DOUBAO', 'https://ark.cn-beijing.volces.com/api/v3',
       '豆包视频生成模型（预留，火山引擎方舟Ark / doubao-seedance系列）：用于教学视频生成等场景，调用入口 /api/ai/video/tasks，启用前需配置api_key',
       'DISABLED'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_model` WHERE `model_name` = 'doubao-seedance-video');
