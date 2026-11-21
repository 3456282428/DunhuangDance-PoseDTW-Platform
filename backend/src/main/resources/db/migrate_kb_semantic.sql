-- =============================================================
-- 知识库语义检索升级 + 跨库关联映射 + Agent 多会话持久化
-- 内容：
--   1) kb_doc_vector  知识条目向量表（本地/远程 Embedding 统一落库，冷启动免重算）
--   2) kb_relation    知识库关联映射表（由业务外键自动推导，支持跨库关联召回与图谱）
--   3) agent_session  Agent 会话表（多段对话管理）
--   4) agent_message  Agent 会话消息表（上下文记忆持久化，重启可续聊）
-- @author liukang&&heyan
-- 用法：mysql -uroot art_training < migrate_kb_semantic.sql
-- =============================================================

USE art_training;

SET NAMES utf8mb4;

-- 1. 知识条目向量表（三端共用一张表，kb_scope 区分端别）
CREATE TABLE IF NOT EXISTS `kb_doc_vector` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `kb_scope` varchar(10) NOT NULL COMMENT '端别：ADMIN/TEACHER/STUDENT',
  `kb_id` int NOT NULL COMMENT '知识条目ID（对应三端各自表的 kb_id）',
  `owner_id` int DEFAULT NULL COMMENT '归属人ID（冗余，用于按端过滤检索）',
  `dim` int NOT NULL DEFAULT '256' COMMENT '向量维度',
  `model` varchar(60) NOT NULL DEFAULT 'local-hash-256' COMMENT '向量化模型标识（远程为 model_name）',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '文本指纹（分词后 token 序列的MD5，用于增量刷新判断）',
  `vec` mediumtext NOT NULL COMMENT '向量（逗号分隔的 float，已 L2 归一化）',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '刷新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vec_doc` (`kb_scope`,`kb_id`),
  KEY `idx_vec_scope_owner` (`kb_scope`,`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识条目向量表（语义检索）';

-- 2. 知识库关联映射表（ref 级关系，查询时按 source_ref 前缀展开到三端条目）
CREATE TABLE IF NOT EXISTS `kb_relation` (
  `relation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `src_ref` varchar(100) NOT NULL COMMENT '源业务引用（如 schedule:12）',
  `dst_ref` varchar(100) NOT NULL COMMENT '目标业务引用（如 class:3）',
  `rel_type` varchar(30) NOT NULL COMMENT '关系类型：BELONGS_TO/OF_CLASS/IN_CLASS/OF_TEACHER/OF_STUDENT/OF_COURSE/IN_ROOM/OF_PROJECT/OF_HOMEWORK/OF_SCHEDULE',
  `src_label` varchar(60) DEFAULT NULL COMMENT '源实体名称（可视化节点标注）',
  `dst_label` varchar(60) DEFAULT NULL COMMENT '目标实体名称（可视化节点标注）',
  `weight` decimal(5,2) NOT NULL DEFAULT '1.00' COMMENT '关联强度',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (`relation_id`),
  UNIQUE KEY `uk_relation` (`src_ref`,`dst_ref`,`rel_type`),
  KEY `idx_rel_src` (`src_ref`),
  KEY `idx_rel_dst` (`dst_ref`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库关联映射表';

-- 3. Agent 会话表（多段对话）
CREATE TABLE IF NOT EXISTS `agent_session` (
  `session_id` varchar(32) NOT NULL COMMENT '会话ID',
  `user_id` int NOT NULL COMMENT '归属用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '归属账号',
  `title` varchar(60) DEFAULT NULL COMMENT '会话标题（首条用户消息摘要）',
  `msg_count` int NOT NULL DEFAULT '0' COMMENT '消息条数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
  PRIMARY KEY (`session_id`),
  KEY `idx_agent_sess_user` (`user_id`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent会话表';

-- 4. Agent 会话消息表（上下文记忆持久化）
CREATE TABLE IF NOT EXISTS `agent_message` (
  `msg_id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` varchar(32) NOT NULL COMMENT '会话ID',
  `role` varchar(12) NOT NULL COMMENT '角色：user/assistant',
  `content` text NOT NULL COMMENT '消息内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`msg_id`),
  KEY `idx_agent_msg_session` (`session_id`,`msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent会话消息表';
