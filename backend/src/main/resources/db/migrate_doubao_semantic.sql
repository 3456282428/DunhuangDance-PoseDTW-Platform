-- =====================================================
-- 豆包语义增强通道配置（勿并入seed_clean.sql，仅对现库执行）
-- 注册豆包多模态理解模型（方舟Ark /responses端点），双通道架构中的
-- 通道二（语义增强层）：ENABLED，用于AI教练点评
-- 通道一（MediaPipe+DTW精确评分）不依赖此模型，不受影响
-- 执行：mysql -uroot -p art_training < migrate_doubao_semantic.sql
-- =====================================================
USE art_training;

-- 1. 注册豆包视觉理解模型（语义增强，启用）
INSERT INTO `ai_model` (`model_name`, `model_type`, `provider`, `endpoint`, `api_key`, `current_version`, `description`, `status`)
SELECT 'doubao-seed-2-0-mini', 'VISION', 'DOUBAO', 'https://ark.cn-beijing.volces.com/api/v3',
       '', 'doubao-seed-2-0-mini-260428',
       '豆包多模态理解模型（通道二语义增强层，方舟Ark /responses）：解析教师/学生视频关键帧+量化评分上下文，生成AI教练自然语言点评',
       'ENABLED'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ai_model` WHERE `model_name` = 'doubao-seed-2-0-mini');

-- 2. 为豆包视频生成模型补齐api_key（保持DISABLED，仅预留凭证）
UPDATE `ai_model`
SET `api_key` = ''
WHERE `model_name` = 'doubao-seedance-video' AND (`api_key` IS NULL OR `api_key` = '');
