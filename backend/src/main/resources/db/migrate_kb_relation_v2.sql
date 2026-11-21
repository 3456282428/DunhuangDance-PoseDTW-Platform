-- =============================================================
-- 知识库关联维度/来源升级 + 3D模型资源库 + 条目-模型推荐映射
-- 内容：
--   1) kb_relation      关联映射表升级：新增关联维度(rel_dimension)与关联来源(rel_source)
--   2) kb_model_resource 3D模型资源库（艺术培训场景模型，含种子数据）
--   3) kb_entry_model    知识条目-3D模型推荐映射（随知识库内容变化自动重算）
-- @author liukang&&heyan
-- 用法：mysql -uroot --default-character-set=utf8mb4 art_training < migrate_kb_relation_v2.sql
-- =============================================================

USE art_training;

SET NAMES utf8mb4;

-- 1. kb_relation 升级：关联维度 + 关联来源（幂等：存在即跳过）
ALTER TABLE `kb_relation`
  ADD COLUMN `rel_dimension` varchar(20) NOT NULL DEFAULT 'ORG' COMMENT '关联维度：ORG组织/TEACH教学/FIN财务/ASSET资产/CONTENT内容' AFTER `rel_type`,
  ADD COLUMN `rel_source` varchar(20) NOT NULL DEFAULT 'BUSINESS_FK' COMMENT '关联来源：BUSINESS_FK业务外键推导/CONTENT_SIM内容相似计算' AFTER `rel_dimension`;

-- 2. 3D模型资源库
CREATE TABLE IF NOT EXISTS `kb_model_resource` (
  `model_id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `model_name` varchar(100) NOT NULL COMMENT '模型名称',
  `category` varchar(20) NOT NULL COMMENT '模型类别：DANCE舞蹈/MUSIC音乐/ART美术/CALLIGRAPHY书法/SCENE场景/PROP道具',
  `tags` varchar(200) NOT NULL COMMENT '适配标签（逗号分隔，用于条目匹配推荐）',
  `format` varchar(20) NOT NULL DEFAULT 'glb' COMMENT '模型格式',
  `file_url` varchar(255) DEFAULT NULL COMMENT '模型文件地址',
  `preview_url` varchar(255) DEFAULT NULL COMMENT '预览图地址',
  `description` varchar(500) DEFAULT NULL COMMENT '模型说明',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`model_id`),
  UNIQUE KEY `uk_model_name` (`model_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='3D模型资源库（知识条目适配推荐）';

-- 3. 知识条目-3D模型推荐映射（变量映射：知识库内容变化后自动重算）
CREATE TABLE IF NOT EXISTS `kb_entry_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `kb_scope` varchar(10) NOT NULL COMMENT '端别：ADMIN/TEACHER/STUDENT',
  `kb_id` int NOT NULL COMMENT '知识条目ID',
  `model_id` int NOT NULL COMMENT '推荐的3D模型ID',
  `match_score` decimal(6,4) NOT NULL DEFAULT '0.0000' COMMENT '匹配分（0~1，规则可追溯）',
  `match_rule` varchar(300) DEFAULT NULL COMMENT '匹配规则说明（标签命中/关联加成/类别默认，可追溯）',
  `match_tags` varchar(200) DEFAULT NULL COMMENT '命中的标签集合',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '推荐重算时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entry_model` (`kb_scope`,`kb_id`),
  KEY `idx_entry_model_model` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识条目-3D模型推荐映射（随知识库内容动态更新）';

-- 4. 种子3D模型资源（艺术培训场景，幂等写入）
INSERT INTO `kb_model_resource` (`model_name`, `category`, `tags`, `format`, `file_url`, `preview_url`, `description`) VALUES
  ('芭蕾舞者-基础站姿', 'DANCE', '芭蕾,舞蹈,形体,基训,剧目,考级,舞蹈教室', 'glb', 'assets/models/dance_ballet_pose.glb', 'assets/models/dance_ballet_pose.png', '芭蕾舞者基础站姿模型，适用于芭蕾/形体基训类知识条目'),
  ('街舞舞者-律动动作', 'DANCE', '街舞,舞蹈,律动,嘻哈,流行舞,breaking,齐舞', 'glb', 'assets/models/dance_hiphop_move.glb', 'assets/models/dance_hiphop_move.png', '街舞律动动作模型，适用于街舞/流行舞教学类条目'),
  ('民族舞舞者-旋转舞姿', 'DANCE', '民族舞,舞蹈,中国舞,旋转,身韵,古典舞,考级', 'glb', 'assets/models/dance_folk_spin.glb', 'assets/models/dance_folk_spin.png', '民族舞旋转舞姿模型，适用于中国舞/民族舞类条目'),
  ('拉丁舞者-恰恰步型', 'DANCE', '拉丁,恰恰,舞蹈,国标,摩登,牛仔,伦巴', 'glb', 'assets/models/dance_latin_chacha.glb', 'assets/models/dance_latin_chacha.png', '拉丁舞恰恰基本步型模型，适用于国标/拉丁舞类条目'),
  ('舞蹈把杆', 'PROP', '把杆,压腿,热身,基训,舞蹈,形体,软开度', 'glb', 'assets/models/prop_barre.glb', 'assets/models/prop_barre.png', '舞蹈把杆道具模型，适用于基训/热身训练类条目'),
  ('舞蹈教室场景', 'SCENE', '舞蹈,舞蹈教室,排练厅,镜子,把杆,地板,教室,场地', 'glb', 'assets/models/scene_dance_studio.glb', 'assets/models/scene_dance_studio.png', '舞蹈教室全景场景模型（镜墙+把杆+木地板），适用于排课/教室类条目'),
  ('三角钢琴', 'MUSIC', '钢琴,琴键,乐理,考级,练习曲,弹奏,器乐', 'glb', 'assets/models/music_piano.glb', 'assets/models/music_piano.png', '三角钢琴模型，适用于钢琴/乐理/器乐类条目'),
  ('小提琴', 'MUSIC', '小提琴,弦乐,乐理,考级,乐器,运弓,音阶', 'glb', 'assets/models/music_violin.glb', 'assets/models/music_violin.png', '小提琴模型，适用于弦乐/器乐教学类条目'),
  ('音乐教室场景', 'SCENE', '音乐,音乐教室,琴房,合唱,视唱练耳,教室,声乐,演出', 'glb', 'assets/models/scene_music_room.glb', 'assets/models/scene_music_room.png', '音乐教室场景模型（琴房+谱架），适用于音乐类教学条目'),
  ('舞台剧场场景', 'SCENE', '舞台,演出,汇报,剧场,比赛,展演,考级,活动,演出报告,名次', 'glb', 'assets/models/scene_stage.glb', 'assets/models/scene_stage.png', '剧场舞台场景模型，适用于演出/比赛/考级/运营类条目'),
  ('画架与调色板', 'ART', '美术,素描,色彩,绘画,画架,创作,水粉,油画,儿童画', 'glb', 'assets/models/art_easel_palette.glb', 'assets/models/art_easel_palette.png', '画架与调色板模型，适用于美术/绘画教学类条目'),
  ('素描石膏组合', 'ART', '素描,石膏,几何体,临摹,美术,绘画,静物,明暗', 'glb', 'assets/models/art_plaster_set.glb', 'assets/models/art_plaster_set.png', '素描石膏几何体组合模型，适用于素描基础类条目'),
  ('毛笔与砚台', 'CALLIGRAPHY', '书法,毛笔,硬笔,临帖,书写,笔墨,楷书,隶书,字帖', 'glb', 'assets/models/calligraphy_brush.glb', 'assets/models/calligraphy_brush.png', '毛笔与砚台模型，适用于书法/写字教学类条目'),
  ('书法教室场景', 'SCENE', '书法,书法教室,文房四宝,字帖,教室,国学', 'glb', 'assets/models/scene_calligraphy_room.glb', 'assets/models/scene_calligraphy_room.png', '书法教室场景模型，适用于书法教学类条目'),
  ('奖杯与证书', 'PROP', '考级,比赛,获奖,证书,奖杯,名次,成果,奖项,晋级', 'glb', 'assets/models/prop_trophy.glb', 'assets/models/prop_trophy.png', '奖杯与考级证书模型，适用于考级比赛/获奖成果类条目')
ON DUPLICATE KEY UPDATE
  `category` = VALUES(`category`), `tags` = VALUES(`tags`), `format` = VALUES(`format`),
  `file_url` = VALUES(`file_url`), `preview_url` = VALUES(`preview_url`), `description` = VALUES(`description`);
