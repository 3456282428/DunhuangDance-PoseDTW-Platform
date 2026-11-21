-- =====================================================
-- 知识库与AI模型管理模块 · 数据库迁移脚本（migrate_knowledge_ai.sql）
-- -----------------------------------------------------
-- 内容：
--   1) 三端知识库表（管理员/教师/学生），同构隔离、统一归集视图
--   2) 知识库操作日志表、备份记录表
--   3) AI模型预留表 + 模型-知识库关联约定表 + AI调用日志表
-- 环境：MySQL 8.0+，库 art_training
-- 用法：mysql -uroot art_training < migrate_knowledge_ai.sql
-- =====================================================

USE art_training;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP VIEW IF EXISTS `v_knowledge_all`;
DROP TABLE IF EXISTS `ai_invoke_log`;
DROP TABLE IF EXISTS `ai_model_kb_binding`;
DROP TABLE IF EXISTS `ai_model`;
DROP TABLE IF EXISTS `kb_backup`;
DROP TABLE IF EXISTS `kb_operation_log`;
DROP TABLE IF EXISTS `kb_student_doc`;
DROP TABLE IF EXISTS `kb_teacher_doc`;
DROP TABLE IF EXISTS `kb_admin_doc`;

-- =====================================================
-- 一、三端知识库表（同构设计：数据逻辑隔离，统一结构便于归集检索）
-- =====================================================

-- 1. 管理员端知识库（运营制度/公告/管理规范等）
CREATE TABLE `kb_admin_doc` (
  `kb_id` int NOT NULL AUTO_INCREMENT COMMENT '知识条目ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `category` varchar(50) NOT NULL DEFAULT '综合' COMMENT '分类（制度规范/公告通知/运营经验/日报快照/综合）',
  `content` longtext COMMENT '正文内容（AI检索主体）',
  `summary` varchar(500) DEFAULT NULL COMMENT '内容摘要（供AI快速定位）',
  `tags` varchar(200) DEFAULT NULL COMMENT '标签（逗号分隔）',
  `source_type` varchar(30) NOT NULL DEFAULT 'MANUAL' COMMENT '来源：MANUAL手动/AUTO_SCHEDULE定时归集',
  `source_ref` varchar(100) DEFAULT NULL COMMENT '来源引用（如 daily_report:20261015，唯一防重复归集）',
  `owner_id` int DEFAULT NULL COMMENT '归属用户ID（管理员端可为NULL=机构级公开）',
  `owner_name` varchar(50) DEFAULT NULL COMMENT '归属人姓名',
  `file_url` varchar(255) DEFAULT NULL COMMENT '附件URL',
  `file_name` varchar(200) DEFAULT NULL COMMENT '附件名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用（AI可检索） 0停用',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`kb_id`),
  UNIQUE KEY `uk_admin_source` (`source_type`, `source_ref`),
  KEY `idx_admin_category` (`category`),
  KEY `idx_admin_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员端知识库';

-- 2. 教师端知识库（教案课件/教学方法/作业布置与批改记录自动归集）
CREATE TABLE `kb_teacher_doc` (
  `kb_id` int NOT NULL AUTO_INCREMENT COMMENT '知识条目ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `category` varchar(50) NOT NULL DEFAULT '教学资料' COMMENT '分类（教案课件/教学方法/作业布置/批改点评/教学资料）',
  `content` longtext COMMENT '正文内容（AI检索主体）',
  `summary` varchar(500) DEFAULT NULL COMMENT '内容摘要（供AI快速定位）',
  `tags` varchar(200) DEFAULT NULL COMMENT '标签（逗号分隔）',
  `source_type` varchar(30) NOT NULL DEFAULT 'MANUAL' COMMENT '来源：MANUAL手动/AUTO_ASSIGN布置作业归集/AUTO_GRADE批改归集',
  `source_ref` varchar(100) DEFAULT NULL COMMENT '来源引用（如 homework:12 / submission:9，唯一防重复归集）',
  `owner_id` int DEFAULT NULL COMMENT '归属教师ID（NULL=机构级共享）',
  `owner_name` varchar(50) DEFAULT NULL COMMENT '归属教师姓名',
  `file_url` varchar(255) DEFAULT NULL COMMENT '附件URL',
  `file_name` varchar(200) DEFAULT NULL COMMENT '附件名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用（AI可检索） 0停用',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`kb_id`),
  UNIQUE KEY `uk_teacher_source` (`source_type`, `source_ref`),
  KEY `idx_teacher_category` (`category`),
  KEY `idx_teacher_owner` (`owner_id`),
  KEY `idx_teacher_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师端知识库';

-- 3. 学生端知识库（学习资料/练习指导/作业提交成果自动归集）
CREATE TABLE `kb_student_doc` (
  `kb_id` int NOT NULL AUTO_INCREMENT COMMENT '知识条目ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `category` varchar(50) NOT NULL DEFAULT '学习资料' COMMENT '分类（学习资料/练习指导/作业成果/学习资料）',
  `content` longtext COMMENT '正文内容（AI检索主体）',
  `summary` varchar(500) DEFAULT NULL COMMENT '内容摘要（供AI快速定位）',
  `tags` varchar(200) DEFAULT NULL COMMENT '标签（逗号分隔）',
  `source_type` varchar(30) NOT NULL DEFAULT 'MANUAL' COMMENT '来源：MANUAL手动/AUTO_SUBMIT作业提交归集',
  `source_ref` varchar(100) DEFAULT NULL COMMENT '来源引用（如 submission:9，唯一防重复归集）',
  `owner_id` int DEFAULT NULL COMMENT '归属学员ID（NULL=公开共享）',
  `owner_name` varchar(50) DEFAULT NULL COMMENT '归属学员姓名',
  `file_url` varchar(255) DEFAULT NULL COMMENT '附件URL',
  `file_name` varchar(200) DEFAULT NULL COMMENT '附件名称',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用（AI可检索） 0停用',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`kb_id`),
  UNIQUE KEY `uk_student_source` (`source_type`, `source_ref`),
  KEY `idx_student_category` (`category`),
  KEY `idx_student_owner` (`owner_id`),
  KEY `idx_student_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生端知识库';

-- 4. 统一归集视图：三端知识库统一检索入口（kb_scope 区分端别）
CREATE VIEW `v_knowledge_all` AS
SELECT 'ADMIN' AS kb_scope, kb_id, title, category, content, summary, tags,
       source_type, source_ref, owner_id, owner_name, file_url, file_name,
       status, view_count, create_time, update_time
  FROM kb_admin_doc
UNION ALL
SELECT 'TEACHER' AS kb_scope, kb_id, title, category, content, summary, tags,
       source_type, source_ref, owner_id, owner_name, file_url, file_name,
       status, view_count, create_time, update_time
  FROM kb_teacher_doc
UNION ALL
SELECT 'STUDENT' AS kb_scope, kb_id, title, category, content, summary, tags,
       source_type, source_ref, owner_id, owner_name, file_url, file_name,
       status, view_count, create_time, update_time
  FROM kb_student_doc;

-- =====================================================
-- 二、操作日志与备份
-- =====================================================

-- 5. 知识库操作日志表（增删改/备份/恢复/AI检索全留痕）
CREATE TABLE `kb_operation_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `kb_scope` varchar(10) NOT NULL COMMENT '端别：ADMIN/TEACHER/STUDENT/SYSTEM',
  `kb_id` int DEFAULT NULL COMMENT '关联知识条目ID（备份/恢复类操作为NULL）',
  `op_type` varchar(20) NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/AUTO_INGEST/BACKUP/RESTORE/AI_QUERY',
  `op_user_id` int DEFAULT NULL COMMENT '操作人用户ID（定时任务为NULL）',
  `op_username` varchar(50) DEFAULT NULL COMMENT '操作人用户名（定时任务为SYSTEM）',
  `op_detail` varchar(1000) DEFAULT NULL COMMENT '操作详情描述',
  `op_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_oplog_scope_time` (`kb_scope`, `op_time`),
  KEY `idx_oplog_kb` (`kb_scope`, `kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库操作日志表';

-- 6. 知识库备份记录表（快照文件落 uploads/kb_backup/，支持恢复）
CREATE TABLE `kb_backup` (
  `backup_id` int NOT NULL AUTO_INCREMENT COMMENT '备份ID',
  `backup_name` varchar(100) NOT NULL COMMENT '备份名称',
  `backup_type` varchar(20) NOT NULL DEFAULT 'MANUAL' COMMENT '类型：MANUAL手动/AUTO每日定时/RESTORE恢复前安全备份',
  `table_scope` varchar(20) NOT NULL DEFAULT 'ALL' COMMENT '备份范围：ALL/ADMIN/TEACHER/STUDENT',
  `item_count` int NOT NULL DEFAULT '0' COMMENT '备份条目数',
  `backup_file` varchar(255) NOT NULL COMMENT '备份文件相对路径',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建人（定时任务为SYSTEM）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '备份时间',
  PRIMARY KEY (`backup_id`),
  KEY `idx_backup_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库备份记录表';

-- =====================================================
-- 三、AI模型预留（本期仅建表+接口预留，不实现业务逻辑）
-- =====================================================

-- 7. AI模型注册表（预留：后续接入大模型时在此登记）
CREATE TABLE `ai_model` (
  `model_id` int NOT NULL AUTO_INCREMENT COMMENT '模型ID',
  `model_name` varchar(100) NOT NULL COMMENT '模型名称',
  `model_type` varchar(30) NOT NULL DEFAULT 'LLM' COMMENT '模型类型：LLM/EMBEDDING/RERANK',
  `provider` varchar(50) DEFAULT NULL COMMENT '提供方（OpenAI/本地部署等）',
  `endpoint` varchar(255) DEFAULT NULL COMMENT '服务地址',
  `api_key` varchar(255) DEFAULT NULL COMMENT '密钥（脱敏存储，后续加密）',
  `description` varchar(500) DEFAULT NULL COMMENT '模型描述',
  `status` varchar(20) NOT NULL DEFAULT 'UNCONFIGURED' COMMENT '状态：UNCONFIGURED未配置/ENABLED启用/DISABLED停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`model_id`),
  UNIQUE KEY `uk_model_name` (`model_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型注册表（预留）';

-- 8. AI模型-知识库关联约定表（约定：AI调用时优先检索哪些知识库范围/分类）
CREATE TABLE `ai_model_kb_binding` (
  `binding_id` int NOT NULL AUTO_INCREMENT COMMENT '绑定ID',
  `model_id` int NOT NULL COMMENT '模型ID（-> ai_model）',
  `kb_scope` varchar(10) NOT NULL COMMENT '知识库范围：ADMIN/TEACHER/STUDENT/ALL',
  `kb_category` varchar(50) DEFAULT NULL COMMENT '限定分类（NULL=不限）',
  `priority` int NOT NULL DEFAULT '0' COMMENT '检索优先级（越大越先检索）',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否生效：1是 0否',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`binding_id`),
  KEY `idx_binding_model` (`model_id`),
  CONSTRAINT `fk_binding_model` FOREIGN KEY (`model_id`) REFERENCES `ai_model` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型-知识库关联约定表（预留）';

-- 9. AI调用日志表（统一调用入口全量留痕，含知识库检索命中情况）
CREATE TABLE `ai_invoke_log` (
  `invoke_id` bigint NOT NULL AUTO_INCREMENT COMMENT '调用ID',
  `caller_scope` varchar(10) NOT NULL COMMENT '调用端别：ADMIN/TEACHER/STUDENT',
  `caller_user_id` int DEFAULT NULL COMMENT '调用人用户ID',
  `question` varchar(1000) NOT NULL COMMENT '提问内容',
  `kb_scope` varchar(10) DEFAULT NULL COMMENT '实际检索的知识库范围',
  `hit_count` int NOT NULL DEFAULT '0' COMMENT '知识库检索命中条数',
  `answer` text COMMENT 'AI回答（预留字段，当前为NULL）',
  `invoke_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待接入/SUCCESS成功/FAILED失败',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `invoke_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
  PRIMARY KEY (`invoke_id`),
  KEY `idx_invoke_time` (`invoke_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI调用日志表（预留）';

-- =====================================================
-- 四、默认数据：登记占位AI模型与知识库检索约定
-- =====================================================

INSERT INTO `ai_model` (`model_name`, `model_type`, `provider`, `description`, `status`)
VALUES ('platform-knowledge-llm', 'LLM', '预留', '平台知识问答大模型（待接入）：调用时优先检索知识库已有数据作为回答依据', 'UNCONFIGURED');

INSERT INTO `ai_model_kb_binding` (`model_id`, `kb_scope`, `kb_category`, `priority`, `enabled`)
SELECT model_id, 'ALL', NULL, 0, 1 FROM `ai_model` WHERE `model_name` = 'platform-knowledge-llm';

SET FOREIGN_KEY_CHECKS = 1;
