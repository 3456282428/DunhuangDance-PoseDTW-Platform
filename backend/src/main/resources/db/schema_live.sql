-- =====================================================
-- art_training 运行库结构快照（仅结构，不含业务数据）
-- 由运行库 mysqldump --no-data --routines --triggers 导出
-- 导出时间: 2026-10-16 11:02:41
-- 权威建表请使用 init.sql；全新数据请使用 seed_clean.sql
-- 本文件仅用于结构对照，不直接用于部署
-- =====================================================

-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: art_training
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `art_training`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `art_training` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `art_training`;

--
-- Table structure for table `agent_message`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_message` (
  `msg_id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` varchar(32) NOT NULL COMMENT '会话ID',
  `role` varchar(12) NOT NULL COMMENT '角色：user/assistant',
  `content` text NOT NULL COMMENT '消息内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`msg_id`),
  KEY `idx_agent_msg_session` (`session_id`,`msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent会话消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `agent_session`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_session` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_agent_log`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_agent_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(32) NOT NULL COMMENT '会话ID',
  `user_id` int NOT NULL COMMENT '操作者用户ID',
  `username` varchar(64) NOT NULL COMMENT '操作者账号',
  `user_message` varchar(500) DEFAULT NULL COMMENT '用户原始输入',
  `tool_name` varchar(64) DEFAULT NULL COMMENT '触发工具名（纯对话为NULL）',
  `tool_args` varchar(500) DEFAULT NULL COMMENT '工具参数JSON',
  `confirmed` tinyint NOT NULL DEFAULT '0' COMMENT '0无需确认 1已确认 2已拒绝',
  `exec_status` varchar(16) NOT NULL COMMENT 'SUCCESS/FAILED/REJECTED/PENDING/CHAT',
  `result_summary` varchar(500) DEFAULT NULL COMMENT '执行结果摘要',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_session` (`session_id`),
  KEY `idx_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent智能体操作审计日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_invoke_log`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI调用日志表（预留）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_model`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  `current_version` varchar(50) DEFAULT NULL COMMENT '当前运行版本',
  `latest_version` varchar(50) DEFAULT NULL COMMENT '可升级到的版本',
  `update_status` varchar(20) NOT NULL DEFAULT 'UNKNOWN' COMMENT '更新状态：UP_TO_DATE / UPDATE_AVAILABLE / UNKNOWN',
  `last_upgrade_time` datetime DEFAULT NULL COMMENT '最近一次升级登记时间',
  PRIMARY KEY (`model_id`),
  UNIQUE KEY `uk_model_name` (`model_name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型注册表（预留）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_model_kb_binding`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型-知识库关联约定表（预留）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_model_metric`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model_metric` (
  `metric_id` bigint NOT NULL AUTO_INCREMENT,
  `model_id` int NOT NULL COMMENT 'AI模型ID（ai_model.model_id）',
  `metric_time` datetime NOT NULL COMMENT '采样时间',
  `latency_ms` decimal(10,2) DEFAULT NULL COMMENT '平均响应延迟(ms)',
  `throughput_qps` decimal(10,2) DEFAULT NULL COMMENT '吞吐量(次/分钟)',
  `accuracy` decimal(6,4) DEFAULT NULL COMMENT '准确率(0-1)',
  `success_rate` decimal(6,4) DEFAULT NULL COMMENT '调用成功率(0-1)',
  `call_count` int DEFAULT '0' COMMENT '采样周期内调用次数',
  `kb_hit_avg` decimal(6,2) DEFAULT NULL COMMENT '知识库平均命中条数',
  `metric_source` varchar(20) DEFAULT 'SAMPLE' COMMENT '来源：SAMPLE系统采样 / SEED初始化 / MANUAL人工',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`metric_id`),
  KEY `idx_model_time` (`model_id`,`metric_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1112 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型性能指标时序';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_model_version`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model_version` (
  `version_id` int NOT NULL AUTO_INCREMENT,
  `model_id` int NOT NULL COMMENT 'AI模型ID（ai_model.model_id）',
  `version` varchar(50) NOT NULL COMMENT '版本号',
  `release_date` date DEFAULT NULL COMMENT '版本日期',
  `changelog` varchar(500) DEFAULT NULL COMMENT '版本说明',
  `is_current` tinyint NOT NULL DEFAULT '0' COMMENT '是否当前在跑版本：1是 0否',
  `upgrade_status` varchar(20) NOT NULL DEFAULT 'APPLIED' COMMENT '升级状态：APPLIED已应用 / PENDING待接入执行 / FAILED失败',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`version_id`),
  KEY `idx_model` (`model_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型版本迭代记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `api_access_log`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `api_access_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问日志ID',
  `endpoint` varchar(200) NOT NULL COMMENT '接口端点模板',
  `method` varchar(10) NOT NULL,
  `api_group` varchar(50) NOT NULL,
  `username` varchar(50) DEFAULT NULL,
  `role_scope` varchar(10) DEFAULT NULL,
  `status` int NOT NULL DEFAULT '200',
  `duration_ms` int NOT NULL DEFAULT '0',
  `access_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_apilog_time` (`access_time`),
  KEY `idx_apilog_endpoint` (`endpoint`),
  KEY `idx_apilog_group` (`api_group`)
) ENGINE=InnoDB AUTO_INCREMENT=2587 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='接口访问留痕（API调用与用量数据源）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `appointment`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `appointment` (
  `appointment_id` int NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `student_id` int NOT NULL COMMENT '学员ID',
  `teacher_id` int NOT NULL COMMENT '教师ID',
  `class_id` int DEFAULT NULL COMMENT '关联班级ID',
  `appointment_type` varchar(20) DEFAULT '试听' COMMENT '类型：试听/补课',
  `appointment_time` datetime NOT NULL COMMENT '预约时间',
  `status` varchar(20) DEFAULT '待确认' COMMENT '状态：待确认/已接受/已拒绝',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`appointment_id`),
  KEY `idx_appt_teacher` (`teacher_id`),
  KEY `idx_appt_student` (`student_id`),
  KEY `fk_appt_class` (`class_id`),
  CONSTRAINT `fk_appt_class` FOREIGN KEY (`class_id`) REFERENCES `course_class` (`class_id`),
  CONSTRAINT `fk_appt_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`),
  CONSTRAINT `fk_appt_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`teacher_id`),
  CONSTRAINT `chk_app_status` CHECK ((`status` in (_utf8mb4'待确认',_utf8mb4'已接受',_utf8mb4'已拒绝',_utf8mb4'已取消')))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预约表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `attendance`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance` (
  `attendance_id` int NOT NULL AUTO_INCREMENT COMMENT '考勤ID',
  `schedule_id` int NOT NULL COMMENT '排课ID',
  `student_id` int NOT NULL COMMENT '学员ID',
  `request_id` int DEFAULT NULL COMMENT '关联请假申请 student_request.request_id（请假批准自动登记时回填）',
  `status` varchar(20) NOT NULL COMMENT '状态：出勤/迟到/缺勤/请假',
  `record_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`attendance_id`),
  UNIQUE KEY `uk_schedule_student` (`schedule_id`,`student_id`),
  KEY `idx_schedule` (`schedule_id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_att_request` (`request_id`),
  CONSTRAINT `attendance_ibfk_1` FOREIGN KEY (`schedule_id`) REFERENCES `course_schedule` (`schedule_id`),
  CONSTRAINT `attendance_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`),
  CONSTRAINT `fk_att_request` FOREIGN KEY (`request_id`) REFERENCES `student_request` (`request_id`),
  CONSTRAINT `chk_att_status` CHECK ((`status` in (_utf8mb4'出勤',_utf8mb4'迟到',_utf8mb4'缺勤',_utf8mb4'请假')))
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考勤表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_attendance_after_insert` AFTER INSERT ON `attendance` FOR EACH ROW BEGIN
    DECLARE v_class_id INT;

    SELECT class_id INTO v_class_id
    FROM course_schedule
    WHERE schedule_id = NEW.schedule_id;

    IF NEW.status IN ('出勤','迟到') THEN
        UPDATE enrollment
        SET attended_count = attended_count + 1
        WHERE student_id = NEW.student_id
          AND class_id = v_class_id
          AND status = '在读';
    ELSEIF NEW.status IN ('缺勤','请假') THEN
        UPDATE enrollment
        SET absent_count = absent_count + 1
        WHERE student_id = NEW.student_id
          AND class_id = v_class_id
          AND status = '在读';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_attendance_after_update` AFTER UPDATE ON `attendance` FOR EACH ROW BEGIN
    DECLARE v_class_id INT;
    SELECT class_id INTO v_class_id FROM course_schedule WHERE schedule_id = NEW.schedule_id;
    IF OLD.status IN ('出勤','迟到') THEN
        UPDATE enrollment SET attended_count = GREATEST(attended_count - 1, 0)
        WHERE student_id = OLD.student_id AND class_id = v_class_id AND status = '在读';
    ELSEIF OLD.status IN ('缺勤','请假') THEN
        UPDATE enrollment SET absent_count = GREATEST(absent_count - 1, 0)
        WHERE student_id = OLD.student_id AND class_id = v_class_id AND status = '在读';
    END IF;
    IF NEW.status IN ('出勤','迟到') THEN
        UPDATE enrollment SET attended_count = attended_count + 1
        WHERE student_id = NEW.student_id AND class_id = v_class_id AND status = '在读';
    ELSEIF NEW.status IN ('缺勤','请假') THEN
        UPDATE enrollment SET absent_count = absent_count + 1
        WHERE student_id = NEW.student_id AND class_id = v_class_id AND status = '在读';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_attendance_after_delete` AFTER DELETE ON `attendance` FOR EACH ROW BEGIN
    DECLARE v_class_id INT;
    SELECT class_id INTO v_class_id FROM course_schedule WHERE schedule_id = OLD.schedule_id;
    IF OLD.status IN ('出勤','迟到') THEN
        UPDATE enrollment SET attended_count = GREATEST(attended_count - 1, 0)
        WHERE student_id = OLD.student_id AND class_id = v_class_id AND status = '在读';
    ELSEIF OLD.status IN ('缺勤','请假') THEN
        UPDATE enrollment SET absent_count = GREATEST(absent_count - 1, 0)
        WHERE student_id = OLD.student_id AND class_id = v_class_id AND status = '在读';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `classroom`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `classroom` (
  `classroom_id` int NOT NULL AUTO_INCREMENT COMMENT '教室ID',
  `classroom_name` varchar(50) NOT NULL COMMENT '教室名称',
  `capacity` int DEFAULT NULL COMMENT '容量',
  `room_type` varchar(50) DEFAULT NULL COMMENT '教室类型（钢琴教室/舞蹈教室等）',
  `status` tinyint DEFAULT '1' COMMENT '状态：1可用 0停用',
  PRIMARY KEY (`classroom_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教室表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course` (
  `course_id` int NOT NULL AUTO_INCREMENT,
  `course_no` varchar(10) DEFAULT NULL,
  `course_name` varchar(100) NOT NULL COMMENT '课程名称(唯一)',
  `category` varchar(50) DEFAULT NULL COMMENT '课程类别:乐器/舞蹈/美术/声乐(文本快照)',
  `category_id` int DEFAULT NULL COMMENT '类别ID(权威外键->course_category)',
  `age_group` varchar(50) DEFAULT NULL COMMENT '适用年龄',
  `level` varchar(50) DEFAULT NULL COMMENT '难度级别',
  `total_hours` int DEFAULT NULL COMMENT '总课时',
  `lesson_duration` int DEFAULT '45' COMMENT '单次时长(分钟)',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `price` decimal(10,2) DEFAULT NULL COMMENT '优惠价',
  `intro` text COMMENT '课程简介',
  `status` tinyint DEFAULT '1' COMMENT '1上架 0下架',
  `sort_order` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`course_id`),
  UNIQUE KEY `course_name` (`course_name`),
  UNIQUE KEY `uk_course_no` (`course_no`),
  KEY `fk_course_category` (`category_id`),
  CONSTRAINT `fk_course_category` FOREIGN KEY (`category_id`) REFERENCES `course_category` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程产品';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course_category`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_category` (
  `category_id` int NOT NULL AUTO_INCREMENT COMMENT '类别ID',
  `category_name` varchar(50) NOT NULL COMMENT '类别名称（乐器/舞蹈/美术/声乐）',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程类别表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course_class`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_class` (
  `class_id` int NOT NULL AUTO_INCREMENT COMMENT '班级ID',
  `course_id` int NOT NULL COMMENT '所属课程产品',
  `class_name` varchar(100) NOT NULL COMMENT '班级名称',
  `category_id` int DEFAULT NULL COMMENT '类别ID(由课程category_id回填,供统计视图使用)',
  `teacher_id` int DEFAULT NULL COMMENT '授课教师ID',
  `classroom_id` int DEFAULT NULL COMMENT '固定教室ID',
  `semester` varchar(50) DEFAULT NULL COMMENT '学期',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `total_hours` int DEFAULT '0' COMMENT '总课时',
  `lesson_duration` int DEFAULT '45' COMMENT '单次时长(分钟)',
  `tuition` decimal(10,2) DEFAULT '0.00' COMMENT '学费',
  `max_students` int DEFAULT '0' COMMENT '最大人数',
  `status` tinyint DEFAULT '1' COMMENT '状态：1正常 0停用',
  `age_group` varchar(20) DEFAULT NULL COMMENT '年龄段',
  `level` varchar(20) DEFAULT NULL COMMENT '难度',
  `description` text COMMENT '课程简介',
  PRIMARY KEY (`class_id`),
  KEY `category_id` (`category_id`),
  KEY `teacher_id` (`teacher_id`),
  KEY `classroom_id` (`classroom_id`),
  KEY `idx_class_name` (`class_name`),
  KEY `fk_class_course` (`course_id`),
  CONSTRAINT `course_class_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `course_category` (`category_id`),
  CONSTRAINT `course_class_ibfk_2` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`teacher_id`),
  CONSTRAINT `course_class_ibfk_3` FOREIGN KEY (`classroom_id`) REFERENCES `classroom` (`classroom_id`),
  CONSTRAINT `fk_class_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course_schedule`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_schedule` (
  `schedule_id` int NOT NULL AUTO_INCREMENT COMMENT '排课ID',
  `class_id` int NOT NULL COMMENT '班级ID',
  `teacher_id` int NOT NULL COMMENT '教师ID',
  `classroom_id` int NOT NULL COMMENT '教室ID',
  `course_date` date NOT NULL COMMENT '上课日期',
  `start_time` time NOT NULL COMMENT '开始时间',
  `end_time` time NOT NULL COMMENT '结束时间',
  `hours` decimal(4,1) DEFAULT '0.0' COMMENT '课时数',
  `status` varchar(20) DEFAULT '待上课' COMMENT '状态：待上课/已完成/已取消',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`schedule_id`),
  UNIQUE KEY `uk_teacher_time` (`teacher_id`,`course_date`,`start_time`),
  UNIQUE KEY `uk_classroom_time` (`classroom_id`,`course_date`,`start_time`),
  UNIQUE KEY `uk_class_time` (`class_id`,`course_date`,`start_time`),
  KEY `idx_course_date` (`course_date`),
  KEY `idx_teacher_date` (`teacher_id`,`course_date`),
  KEY `idx_classroom_date` (`classroom_id`,`course_date`),
  CONSTRAINT `course_schedule_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `course_class` (`class_id`),
  CONSTRAINT `course_schedule_ibfk_2` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`teacher_id`),
  CONSTRAINT `course_schedule_ibfk_3` FOREIGN KEY (`classroom_id`) REFERENCES `classroom` (`classroom_id`),
  CONSTRAINT `chk_sched_status` CHECK ((`status` in (_utf8mb4'待上课',_utf8mb4'已完成',_utf8mb4'已取消')))
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='排课表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dashboard_operation_log`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dashboard_operation_log` (
  `dash_log_id` bigint NOT NULL AUTO_INCREMENT,
  `op_type` varchar(30) NOT NULL COMMENT '操作类型：SYNC_COMPONENT组件同步 / UPGRADE_MODEL模型更新 / EXPORT_REPORT报表导出 / VIEW_DETAIL查看详情',
  `op_target` varchar(200) DEFAULT NULL COMMENT '操作对象',
  `op_detail` varchar(1000) DEFAULT NULL COMMENT '操作说明',
  `op_result` varchar(20) DEFAULT 'SUCCESS' COMMENT '结果：SUCCESS / FAILED / PENDING',
  `op_user_id` int DEFAULT NULL,
  `op_username` varchar(50) DEFAULT NULL,
  `op_role` varchar(20) DEFAULT NULL COMMENT '操作角色 ADMIN/TEACHER/STUDENT',
  `op_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`dash_log_id`),
  KEY `idx_op_time` (`op_time`),
  KEY `idx_op_type` (`op_type`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仪表盘管理操作日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dashboard_topology`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dashboard_topology` (
  `edge_id` int NOT NULL AUTO_INCREMENT,
  `source_type` varchar(20) NOT NULL COMMENT '源类型：COMPONENT / AI_MODEL / KB_SCOPE',
  `source_key` varchar(100) NOT NULL COMMENT '源标识（组件名/模型名/ADMIN|TEACHER|STUDENT）',
  `target_type` varchar(20) NOT NULL COMMENT '目标类型：COMPONENT / AI_MODEL / KB_SCOPE',
  `target_key` varchar(100) NOT NULL COMMENT '目标标识',
  `relation` varchar(30) DEFAULT 'depends_on' COMMENT '关系：depends_on依赖 / retrieves检索 / invokes调用',
  `dep_type` varchar(20) DEFAULT 'runtime' COMMENT '依赖类型：runtime运行时 / build构建期 / peer同级',
  `version_constraint` varchar(50) DEFAULT NULL COMMENT '版本约束，如 >=2.9',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`edge_id`),
  UNIQUE KEY `uk_edge` (`source_type`,`source_key`,`target_type`,`target_key`,`relation`),
  KEY `idx_source` (`source_type`,`source_key`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仪表盘拓扑关系（组件依赖/AI模型知识库关联）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `enrollment`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollment` (
  `enrollment_id` int NOT NULL AUTO_INCREMENT COMMENT '报名ID',
  `student_id` int NOT NULL COMMENT '学员ID',
  `class_id` int NOT NULL COMMENT '班级ID',
  `enroll_date` date DEFAULT NULL COMMENT '报名日期',
  `tuition_amount` decimal(10,2) DEFAULT '0.00' COMMENT '缴费金额',
  `status` varchar(20) DEFAULT '在读' COMMENT '状态：在读/退学/结课',
  `attended_count` int DEFAULT '0' COMMENT '已出勤次数',
  `absent_count` int DEFAULT '0' COMMENT '缺勤次数',
  `total_paid` decimal(10,2) DEFAULT '0.00' COMMENT '累计缴费',
  PRIMARY KEY (`enrollment_id`),
  UNIQUE KEY `uk_student_class` (`student_id`,`class_id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_class` (`class_id`),
  CONSTRAINT `enrollment_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`),
  CONSTRAINT `enrollment_ibfk_2` FOREIGN KEY (`class_id`) REFERENCES `course_class` (`class_id`),
  CONSTRAINT `chk_enroll_status` CHECK ((`status` in (_utf8mb4'在读',_utf8mb4'退学',_utf8mb4'结课')))
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报名表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `exam_project`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_project` (
  `project_id` int NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `name` varchar(100) NOT NULL COMMENT '项目名称',
  `activity_type` varchar(10) NOT NULL DEFAULT '考级' COMMENT '考级/比赛',
  `category` varchar(20) DEFAULT NULL COMMENT '乐器/舞蹈/美术/声乐',
  `organizer` varchar(100) DEFAULT NULL COMMENT '主办单位',
  `signup_start` date DEFAULT NULL COMMENT '报名开始',
  `signup_end` date DEFAULT NULL COMMENT '报名截止',
  `exam_date` date DEFAULT NULL COMMENT '考试/比赛日期',
  `exam_start` varchar(8) DEFAULT NULL,
  `exam_end` varchar(8) DEFAULT NULL,
  `exam_room` varchar(100) DEFAULT NULL,
  `location` varchar(100) DEFAULT NULL COMMENT '地点',
  `fee` decimal(10,2) DEFAULT '0.00' COMMENT '报名费用',
  `requirement` text COMMENT '报名条件/曲目作品要求',
  `award_desc` varchar(255) DEFAULT NULL COMMENT '奖项设置',
  `materials` varchar(255) DEFAULT NULL COMMENT '所需材料',
  `status` varchar(10) NOT NULL DEFAULT '报名中' COMMENT '报名中/即将截止/已结束',
  PRIMARY KEY (`project_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考级比赛项目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `exam_registration`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_registration` (
  `reg_id` int NOT NULL AUTO_INCREMENT COMMENT '报名ID',
  `project_id` int NOT NULL COMMENT '项目ID',
  `student_id` int NOT NULL COMMENT '学员ID',
  `class_id` int DEFAULT NULL COMMENT '关联班级',
  `real_name` varchar(50) DEFAULT NULL COMMENT '报名姓名',
  `gender` varchar(6) DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `id_card` varchar(30) DEFAULT NULL COMMENT '身份证号',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `material_url` varchar(255) DEFAULT NULL COMMENT '材料附件',
  `teacher_id` int DEFAULT NULL COMMENT '推荐教师',
  `teacher_comment` varchar(255) DEFAULT NULL COMMENT '教师推荐意见',
  `status` varchar(10) NOT NULL DEFAULT '待审核' COMMENT '待审核/已确认/已缴费/已完成/已驳回/已取消',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核留言',
  `exam_time` varchar(50) DEFAULT NULL COMMENT '考试时间安排',
  `exam_room` varchar(50) DEFAULT NULL COMMENT '考场',
  `seat_no` varchar(20) DEFAULT NULL COMMENT '座位号',
  `result` varchar(50) DEFAULT NULL COMMENT '成绩/结果',
  `award` varchar(100) DEFAULT NULL COMMENT '获奖情况',
  `certificate_no` varchar(50) DEFAULT NULL COMMENT '证书编号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`reg_id`),
  UNIQUE KEY `uk_project_student` (`project_id`,`student_id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_project` (`project_id`),
  KEY `fk_examreg_class` (`class_id`),
  KEY `fk_examreg_teacher` (`teacher_id`),
  CONSTRAINT `fk_examreg_class` FOREIGN KEY (`class_id`) REFERENCES `course_class` (`class_id`),
  CONSTRAINT `fk_examreg_project` FOREIGN KEY (`project_id`) REFERENCES `exam_project` (`project_id`),
  CONSTRAINT `fk_examreg_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`),
  CONSTRAINT `fk_examreg_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`teacher_id`),
  CONSTRAINT `chk_er_status` CHECK ((`status` in (_utf8mb4'待审核',_utf8mb4'已确认',_utf8mb4'已驳回',_utf8mb4'已缴费',_utf8mb4'已完成',_utf8mb4'已取消'))),
  CONSTRAINT `chk_exam_reg_phone` CHECK (((`phone` is null) or regexp_like(`phone`,_utf8mb4'^1[3-9][0-9]{9}$')))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考级比赛报名表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `feedback`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `feedback` (
  `feedback_id` int NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  `student_id` int NOT NULL COMMENT '学员ID',
  `content` text NOT NULL COMMENT '反馈内容',
  `type` varchar(20) DEFAULT '建议' COMMENT '类型：建议/投诉/咨询',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `status` varchar(20) DEFAULT '待处理' COMMENT '状态：待处理/已回复',
  `reply_content` varchar(255) DEFAULT NULL COMMENT '回复内容',
  `reply_time` datetime DEFAULT NULL COMMENT '回复时间',
  `reply_by` varchar(50) DEFAULT NULL COMMENT '回复人(文本快照)',
  `reply_user_id` int DEFAULT NULL COMMENT '回复人用户ID(权威外键->sys_user)',
  PRIMARY KEY (`feedback_id`),
  KEY `idx_feedback_student` (`student_id`),
  KEY `fk_fb_reply_user` (`reply_user_id`),
  CONSTRAINT `fk_fb_reply_user` FOREIGN KEY (`reply_user_id`) REFERENCES `sys_user` (`user_id`),
  CONSTRAINT `fk_fb_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`),
  CONSTRAINT `chk_fb_status` CHECK ((`status` in (_utf8mb4'待处理',_utf8mb4'已回复'))),
  CONSTRAINT `chk_fb_type` CHECK ((`type` in (_utf8mb4'建议',_utf8mb4'投诉',_utf8mb4'咨询',_utf8mb4'报修')))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='建议投诉反馈表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `homework`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `homework` (
  `homework_id` int NOT NULL AUTO_INCREMENT COMMENT '作业ID',
  `class_id` int NOT NULL COMMENT '班级ID',
  `schedule_id` int DEFAULT NULL COMMENT '关联排课ID（可选）',
  `title` varchar(100) DEFAULT NULL COMMENT '作业标题',
  `content` text COMMENT '作业内容/学生答案',
  `ref_video_url` varchar(500) DEFAULT NULL COMMENT '教师参考视频URL（AI评分基准）',
  `ref_video_name` varchar(255) DEFAULT NULL COMMENT '教师参考视频原始文件名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '布置时间',
  `deadline` datetime DEFAULT NULL COMMENT '截止时间',
  PRIMARY KEY (`homework_id`),
  KEY `schedule_id` (`schedule_id`),
  KEY `idx_class` (`class_id`),
  CONSTRAINT `homework_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `course_class` (`class_id`),
  CONSTRAINT `homework_ibfk_2` FOREIGN KEY (`schedule_id`) REFERENCES `course_schedule` (`schedule_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `homework_submission`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `homework_submission` (
  `submission_id` int NOT NULL AUTO_INCREMENT,
  `homework_id` int NOT NULL,
  `student_id` int NOT NULL,
  `content` text,
  `attachment_url` varchar(1000) DEFAULT NULL,
  `attachment_name` varchar(500) DEFAULT NULL,
  `submit_time` datetime DEFAULT NULL,
  `score` decimal(5,2) DEFAULT NULL,
  `teacher_comment` varchar(255) DEFAULT NULL,
  `ai_score` decimal(5,2) DEFAULT NULL COMMENT 'AI智能评分总分',
  `ai_report` text COMMENT 'AI评分报告JSON（部位分/维度分/建议）',
  `ai_status` varchar(20) DEFAULT NULL COMMENT 'AI评分状态：评分中/已完成/失败',
  `ai_scored_at` datetime DEFAULT NULL COMMENT 'AI评分完成时间',
  `status` varchar(20) DEFAULT '未提交',
  PRIMARY KEY (`submission_id`),
  UNIQUE KEY `uk_homework_student` (`homework_id`,`student_id`),
  KEY `fk_sub_student` (`student_id`),
  CONSTRAINT `fk_sub_homework` FOREIGN KEY (`homework_id`) REFERENCES `homework` (`homework_id`),
  CONSTRAINT `fk_sub_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`),
  CONSTRAINT `chk_hs_status` CHECK ((`status` in (_utf8mb4'未提交',_utf8mb4'已提交',_utf8mb4'已批改')))
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业提交记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kb_admin_doc`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  UNIQUE KEY `uk_admin_source` (`source_type`,`source_ref`),
  KEY `idx_admin_category` (`category`),
  KEY `idx_admin_title` (`title`)
) ENGINE=InnoDB AUTO_INCREMENT=199 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员端知识库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kb_backup`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库备份记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kb_doc_vector`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kb_doc_vector` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kb_operation_log`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `idx_oplog_scope_time` (`kb_scope`,`op_time`),
  KEY `idx_oplog_kb` (`kb_scope`,`kb_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kb_relation`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kb_relation` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kb_student_doc`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  UNIQUE KEY `uk_student_source` (`source_type`,`source_ref`),
  KEY `idx_student_category` (`category`),
  KEY `idx_student_owner` (`owner_id`),
  KEY `idx_student_title` (`title`)
) ENGINE=InnoDB AUTO_INCREMENT=199 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生端知识库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kb_teacher_doc`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  UNIQUE KEY `uk_teacher_source` (`source_type`,`source_ref`),
  KEY `idx_teacher_category` (`category`),
  KEY `idx_teacher_owner` (`owner_id`),
  KEY `idx_teacher_title` (`title`)
) ENGINE=InnoDB AUTO_INCREMENT=141 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师端知识库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `message`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message` (
  `message_id` int NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `receiver_user_id` int NOT NULL COMMENT '接收人sys_user.user_id',
  `msg_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '系统' COMMENT '系统/作业/申请/考级/调课/预约',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内容',
  `biz_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联业务类型',
  `biz_id` int DEFAULT NULL COMMENT '关联业务ID',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '0未读 1已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_msg_receiver` (`receiver_user_id`,`is_read`),
  CONSTRAINT `fk_msg_receiver` FOREIGN KEY (`receiver_user_id`) REFERENCES `sys_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='站内消息通知';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `news`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `news` (
  `news_id` int NOT NULL AUTO_INCREMENT COMMENT '资讯ID',
  `title` varchar(100) NOT NULL COMMENT '标题',
  `content` text COMMENT '内容',
  `publisher` varchar(50) DEFAULT NULL COMMENT '发布人(文本快照)',
  `publisher_id` int DEFAULT NULL COMMENT '发布人用户ID(权威外键->sys_user)',
  `publish_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `view_count` int DEFAULT '0' COMMENT '浏览量',
  PRIMARY KEY (`news_id`),
  KEY `fk_news_publisher` (`publisher_id`),
  CONSTRAINT `fk_news_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `sys_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资讯表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `open_component`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_component` (
  `component_id` int NOT NULL AUTO_INCREMENT COMMENT '组件ID',
  `name` varchar(100) NOT NULL COMMENT '组件名称',
  `platform` varchar(20) NOT NULL DEFAULT 'GITHUB' COMMENT '来源平台：GITHUB / GITEE',
  `repo_owner` varchar(100) NOT NULL COMMENT '仓库归属（owner 或 group）',
  `repo_name` varchar(100) NOT NULL COMMENT '仓库名',
  `repo_url` varchar(255) DEFAULT NULL COMMENT '仓库地址',
  `category` varchar(50) DEFAULT NULL COMMENT '用途分类（姿态识别/视频处理/数值计算/前端框架/数据可视化等）',
  `language` varchar(50) DEFAULT NULL COMMENT '主要开发语言',
  `license` varchar(50) DEFAULT NULL COMMENT '开源协议',
  `used_by_module` varchar(100) DEFAULT NULL COMMENT '本系统内使用模块',
  `current_version` varchar(50) DEFAULT NULL COMMENT '当前使用版本',
  `latest_version` varchar(50) DEFAULT NULL COMMENT '最新可用版本（远端最新发布）',
  `latest_release_date` date DEFAULT NULL COMMENT '最新版本发布时间',
  `update_status` varchar(20) NOT NULL DEFAULT 'UNKNOWN' COMMENT '更新状态：UP_TO_DATE已是最新 / UPDATE_AVAILABLE有可用更新 / UNKNOWN未知 / SYNC_FAILED同步失败',
  `star_count` int DEFAULT '0' COMMENT 'Star数',
  `fork_count` int DEFAULT '0' COMMENT 'Fork数',
  `open_issue_count` int DEFAULT '0' COMMENT '开放Issue数',
  `description` varchar(500) DEFAULT NULL COMMENT '组件说明',
  `homepage` varchar(255) DEFAULT NULL COMMENT '官方主页',
  `sync_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否参与自动同步：1是 0否',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最近一次同步时间',
  `last_sync_status` varchar(20) DEFAULT NULL COMMENT '最近一次同步结果：OK / FAILED / SKIPPED',
  `last_sync_message` varchar(500) DEFAULT NULL COMMENT '最近一次同步说明（失败原因等）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`component_id`),
  UNIQUE KEY `uk_repo` (`platform`,`repo_owner`,`repo_name`),
  KEY `idx_platform` (`platform`),
  KEY `idx_update_status` (`update_status`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='开源组件登记与版本追踪';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `open_component_version`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_component_version` (
  `version_id` int NOT NULL AUTO_INCREMENT,
  `component_id` int NOT NULL COMMENT '组件ID',
  `version` varchar(50) NOT NULL COMMENT '版本号',
  `release_date` date DEFAULT NULL COMMENT '发布时间',
  `changelog` varchar(500) DEFAULT NULL COMMENT '变更说明',
  `is_current` tinyint NOT NULL DEFAULT '0' COMMENT '是否当前使用版本：1是 0否',
  `detect_source` varchar(20) DEFAULT 'API' COMMENT '数据来源：API远端拉取 / MANUAL手工登记 / SEED初始化',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`version_id`),
  KEY `idx_component` (`component_id`),
  CONSTRAINT `fk_ocv_component` FOREIGN KEY (`component_id`) REFERENCES `open_component` (`component_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='开源组件版本迭代记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment` (
  `payment_id` int NOT NULL AUTO_INCREMENT COMMENT '缴费ID',
  `student_id` int DEFAULT NULL COMMENT '学员ID（班级类缴费可空）',
  `class_id` int DEFAULT NULL COMMENT '班级ID',
  `enrollment_id` int DEFAULT NULL,
  `reg_id` int DEFAULT NULL COMMENT '关联考级/比赛报名ID(exam_registration)',
  `pay_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '缴费时间',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `pay_type` varchar(20) NOT NULL COMMENT '缴费类型：报名/续费/考级/比赛/其他',
  `pay_method` varchar(20) DEFAULT NULL COMMENT '支付方式：微信/支付宝/现金/刷卡',
  `operator_id` int DEFAULT NULL COMMENT '操作员ID（管理员/财务）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `status` varchar(20) NOT NULL DEFAULT '已支付' COMMENT '状态：已支付/退款中/已退款',
  `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
  `refund_operator_id` int DEFAULT NULL COMMENT '退款操作人sys_user.user_id',
  PRIMARY KEY (`payment_id`),
  KEY `class_id` (`class_id`),
  KEY `operator_id` (`operator_id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_pay_date` (`pay_date`),
  KEY `fk_payment_enrollment` (`enrollment_id`),
  KEY `fk_pay_refund_op` (`refund_operator_id`),
  KEY `idx_pay_reg` (`reg_id`),
  CONSTRAINT `fk_pay_refund_op` FOREIGN KEY (`refund_operator_id`) REFERENCES `sys_user` (`user_id`),
  CONSTRAINT `fk_pay_reg` FOREIGN KEY (`reg_id`) REFERENCES `exam_registration` (`reg_id`),
  CONSTRAINT `fk_payment_enrollment` FOREIGN KEY (`enrollment_id`) REFERENCES `enrollment` (`enrollment_id`),
  CONSTRAINT `payment_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`),
  CONSTRAINT `payment_ibfk_2` FOREIGN KEY (`class_id`) REFERENCES `course_class` (`class_id`),
  CONSTRAINT `payment_ibfk_3` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`user_id`),
  CONSTRAINT `chk_pay_fk_consistency` CHECK (((`pay_type` = _utf8mb4'其他') or ((`pay_type` = _utf8mb4'退款') and ((`enrollment_id` is not null) or (`reg_id` is not null))) or ((`pay_type` in (_utf8mb4'报名',_utf8mb4'续费')) and (`enrollment_id` is not null) and (`student_id` is not null) and (`class_id` is not null) and (`reg_id` is null)) or ((`pay_type` in (_utf8mb4'考级',_utf8mb4'比赛')) and (`reg_id` is not null) and (`student_id` is not null) and (`enrollment_id` is null)))),
  CONSTRAINT `chk_pay_status` CHECK ((`status` in (_utf8mb4'已支付',_utf8mb4'退款中',_utf8mb4'已退款'))),
  CONSTRAINT `chk_pay_type` CHECK ((`pay_type` in (_utf8mb4'报名',_utf8mb4'续费',_utf8mb4'考级',_utf8mb4'比赛',_utf8mb4'退款',_utf8mb4'其他')))
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='缴费表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reschedule_request`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reschedule_request` (
  `request_id` int NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `schedule_id` int NOT NULL COMMENT '排课ID',
  `request_type` varchar(10) NOT NULL DEFAULT '调课' COMMENT '调课/交换',
  `target_schedule_id` int DEFAULT NULL COMMENT '交换目标排课ID',
  `new_date` date NOT NULL COMMENT '新日期',
  `new_start_time` time NOT NULL COMMENT '新开始时间',
  `new_end_time` time NOT NULL COMMENT '新结束时间',
  `reason` varchar(255) DEFAULT NULL COMMENT '调课原因',
  `status` varchar(20) DEFAULT '待审核' COMMENT '状态：待审核/已通过/已拒绝',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `handle_time` datetime DEFAULT NULL COMMENT '审批时间',
  `handle_remark` varchar(255) DEFAULT NULL COMMENT '审批意见',
  `handler_id` int DEFAULT NULL COMMENT '审批人sys_user.user_id',
  PRIMARY KEY (`request_id`),
  KEY `idx_rr_schedule` (`schedule_id`),
  KEY `fk_rr_handler` (`handler_id`),
  KEY `idx_rr_status` (`status`),
  KEY `fk_rr_target` (`target_schedule_id`),
  CONSTRAINT `fk_rr_handler` FOREIGN KEY (`handler_id`) REFERENCES `sys_user` (`user_id`),
  CONSTRAINT `fk_rr_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `course_schedule` (`schedule_id`),
  CONSTRAINT `fk_rr_target` FOREIGN KEY (`target_schedule_id`) REFERENCES `course_schedule` (`schedule_id`),
  CONSTRAINT `chk_rr_status` CHECK ((`status` in (_utf8mb4'待审核',_utf8mb4'已通过',_utf8mb4'已拒绝')))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='调课申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `student`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student` (
  `student_id` int NOT NULL AUTO_INCREMENT COMMENT '学员ID',
  `student_no` varchar(20) DEFAULT NULL COMMENT '学员编号',
  `user_id` int DEFAULT NULL,
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `gender` char(1) DEFAULT NULL COMMENT '性别：男/女',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `parent_phone` varchar(20) DEFAULT NULL COMMENT '家长电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `enroll_date` date DEFAULT NULL COMMENT '入学日期',
  `status` tinyint DEFAULT '1' COMMENT '状态：1在读 0离校',
  PRIMARY KEY (`student_id`),
  UNIQUE KEY `uk_student_user` (`user_id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_student_no` (`student_no`),
  CONSTRAINT `fk_student_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`),
  CONSTRAINT `chk_student_parent_phone` CHECK (((`parent_phone` is null) or regexp_like(`parent_phone`,_utf8mb4'^1[3-9][0-9]{9}$')))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `student_comment`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_comment` (
  `comment_id` int NOT NULL AUTO_INCREMENT COMMENT '评语ID',
  `student_id` int NOT NULL COMMENT '学员ID',
  `class_id` int DEFAULT NULL COMMENT '班级ID',
  `teacher_id` int DEFAULT NULL COMMENT '教师ID',
  `content` varchar(500) DEFAULT NULL COMMENT '评语内容',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '评语时间',
  PRIMARY KEY (`comment_id`),
  KEY `idx_sc_student` (`student_id`,`class_id`),
  KEY `fk_sc_class` (`class_id`),
  KEY `fk_sc_teacher` (`teacher_id`),
  CONSTRAINT `fk_sc_class` FOREIGN KEY (`class_id`) REFERENCES `course_class` (`class_id`),
  CONSTRAINT `fk_sc_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`),
  CONSTRAINT `fk_sc_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`teacher_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学员评语表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `student_request`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_request` (
  `request_id` int NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `student_id` int NOT NULL COMMENT '学员ID',
  `teacher_id` int DEFAULT NULL COMMENT '教师ID',
  `class_id` int DEFAULT NULL COMMENT '班级ID',
  `request_type` varchar(20) NOT NULL COMMENT '类型：请假/缓考/补考',
  `request_date` date DEFAULT NULL COMMENT '申请日期',
  `reason` varchar(500) DEFAULT NULL COMMENT '申请理由',
  `status` varchar(20) DEFAULT '待处理' COMMENT '状态：待处理/已批准/已拒绝',
  `reply_remark` varchar(500) DEFAULT NULL COMMENT '处理意见',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`request_id`),
  KEY `idx_sr_student` (`student_id`),
  KEY `idx_sr_teacher` (`teacher_id`),
  KEY `fk_sr_class` (`class_id`),
  CONSTRAINT `fk_sr_class` FOREIGN KEY (`class_id`) REFERENCES `course_class` (`class_id`),
  CONSTRAINT `fk_sr_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`),
  CONSTRAINT `fk_sr_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`teacher_id`),
  CONSTRAINT `chk_sr_status` CHECK ((`status` in (_utf8mb4'待处理',_utf8mb4'已批准',_utf8mb4'已拒绝'))),
  CONSTRAINT `chk_sr_type` CHECK ((`request_type` in (_utf8mb4'请假',_utf8mb4'缓考',_utf8mb4'补考',_utf8mb4'休学',_utf8mb4'其他')))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学员请假/缓考/补考申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `user_id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `role` varchar(20) NOT NULL COMMENT '角色：ADMIN/TEACHER/STUDENT/FINANCE/ACADEMIC',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `status` tinyint DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_username` (`username`),
  CONSTRAINT `chk_sys_user_phone` CHECK (((`phone` is null) or regexp_like(`phone`,_utf8mb4'^1[3-9][0-9]{9}$')))
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_config`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_config` (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` varchar(50) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值(JSON)',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `teacher`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher` (
  `teacher_id` int NOT NULL AUTO_INCREMENT COMMENT '教师ID',
  `teacher_no` varchar(20) DEFAULT NULL COMMENT '教师编号',
  `user_id` int DEFAULT NULL,
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `gender` char(1) DEFAULT NULL COMMENT '性别：男/女',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `address` varchar(255) DEFAULT NULL COMMENT '住址',
  `emergency_contact` varchar(50) DEFAULT NULL COMMENT '紧急联系人',
  `emergency_phone` varchar(20) DEFAULT NULL COMMENT '紧急联系电话',
  `major` varchar(50) DEFAULT NULL COMMENT '专业（如钢琴、舞蹈）',
  `education` varchar(50) DEFAULT NULL COMMENT '学历',
  `school` varchar(100) DEFAULT NULL COMMENT '毕业院校',
  `title` varchar(50) DEFAULT NULL COMMENT '职称',
  `employ_type` varchar(20) DEFAULT '全职' COMMENT '全职/兼职/实习/离职',
  `teach_years` int DEFAULT '0' COMMENT '教龄',
  `photo` varchar(255) DEFAULT NULL COMMENT '照片URL',
  `certs` varchar(1000) DEFAULT NULL COMMENT '资质证件URL(JSON)',
  `hourly_rate` decimal(10,2) DEFAULT '0.00' COMMENT '课时费',
  `base_salary` decimal(10,2) DEFAULT '0.00' COMMENT '底薪',
  `intro` text COMMENT '教师简介',
  `hire_date` date DEFAULT NULL COMMENT '入职日期',
  `status` tinyint DEFAULT '1' COMMENT '状态：1在职 0离职',
  PRIMARY KEY (`teacher_id`),
  UNIQUE KEY `uk_teacher_user` (`user_id`),
  UNIQUE KEY `uk_teacher_no` (`teacher_no`),
  KEY `idx_teacher_no` (`teacher_no`),
  CONSTRAINT `fk_teacher_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`),
  CONSTRAINT `chk_teacher_emergency_phone` CHECK (((`emergency_phone` is null) or regexp_like(`emergency_phone`,_utf8mb4'^1[3-9][0-9]{9}$'))),
  CONSTRAINT `chk_teacher_phone` CHECK (((`phone` is null) or regexp_like(`phone`,_utf8mb4'^1[3-9][0-9]{9}$')))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `teacher_course`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_course` (
  `teacher_id` int NOT NULL COMMENT '教师ID',
  `course_id` int NOT NULL COMMENT '擅长课程ID',
  PRIMARY KEY (`teacher_id`,`course_id`),
  KEY `idx_tc_course` (`course_id`),
  CONSTRAINT `fk_tc_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`),
  CONSTRAINT `fk_tc_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师擅长课程关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `v_course_attendance_rate`
--

SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_course_attendance_rate` AS SELECT 
 1 AS `class_id`,
 1 AS `class_name`,
 1 AS `category_name`,
 1 AS `enrolled_count`,
 1 AS `finished_schedule_count`,
 1 AS `total_attendance_records`,
 1 AS `attended_times`,
 1 AS `attendance_rate`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_knowledge_all`
--

SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_knowledge_all` AS SELECT 
 1 AS `kb_scope`,
 1 AS `kb_id`,
 1 AS `title`,
 1 AS `category`,
 1 AS `content`,
 1 AS `summary`,
 1 AS `tags`,
 1 AS `source_type`,
 1 AS `source_ref`,
 1 AS `owner_id`,
 1 AS `owner_name`,
 1 AS `file_url`,
 1 AS `file_name`,
 1 AS `status`,
 1 AS `view_count`,
 1 AS `create_time`,
 1 AS `update_time`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `visitor_booking`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `visitor_booking` (
  `booking_id` int NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `name` varchar(50) NOT NULL COMMENT '访客姓名',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `class_id` int DEFAULT NULL COMMENT '试听课程ID',
  `teacher_id` int DEFAULT NULL COMMENT '教师ID',
  `preferred_time` datetime DEFAULT NULL COMMENT '期望试听时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `status` varchar(20) DEFAULT '待确认' COMMENT '状态：待确认/已确认/已取消',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (`booking_id`),
  KEY `idx_vb_phone` (`phone`),
  KEY `fk_vb_class` (`class_id`),
  KEY `fk_vb_teacher` (`teacher_id`),
  CONSTRAINT `fk_vb_class` FOREIGN KEY (`class_id`) REFERENCES `course_class` (`class_id`),
  CONSTRAINT `fk_vb_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`teacher_id`),
  CONSTRAINT `chk_vb_status` CHECK ((`status` in (_utf8mb4'待确认',_utf8mb4'已确认',_utf8mb4'已取消'))),
  CONSTRAINT `chk_visitor_phone` CHECK (((`phone` is null) or regexp_like(`phone`,_utf8mb4'^1[3-9][0-9]{9}$')))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='游客试听预约表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'art_training'
--
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_student_annual_records`(
    IN p_student_id INT,
    IN p_year INT
)
BEGIN
    SELECT
        cs.course_date,
        cc.class_name,
        cat.category_name,
        cs.start_time,
        cs.end_time,
        t.name AS teacher_name,
        cr.classroom_name,
        a.status AS attendance_status
    FROM enrollment e
    JOIN course_class cc ON e.class_id = cc.class_id
    JOIN course_category cat ON cc.category_id = cat.category_id
    JOIN course_schedule cs ON cc.class_id = cs.class_id
    JOIN teacher t ON cs.teacher_id = t.teacher_id
    JOIN classroom cr ON cs.classroom_id = cr.classroom_id
    LEFT JOIN attendance a
        ON cs.schedule_id = a.schedule_id
        AND a.student_id = e.student_id
    WHERE e.student_id = p_student_id
      AND e.status = '在读'
      AND YEAR(cs.course_date) = p_year
    ORDER BY cs.course_date, cs.start_time;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Current Database: `art_training`
--

USE `art_training`;

--
-- Final view structure for view `v_course_attendance_rate`
--

/*!50001 DROP VIEW IF EXISTS `v_course_attendance_rate`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_course_attendance_rate` AS select `cc`.`class_id` AS `class_id`,`cc`.`class_name` AS `class_name`,`cat`.`category_name` AS `category_name`,(select count(0) from `enrollment` `e` where ((`e`.`class_id` = `cc`.`class_id`) and (`e`.`status` = '在读'))) AS `enrolled_count`,(select count(0) from `course_schedule` `cs` where ((`cs`.`class_id` = `cc`.`class_id`) and (`cs`.`status` = '已完成'))) AS `finished_schedule_count`,(select count(0) from (`attendance` `a` join `course_schedule` `cs2` on((`a`.`schedule_id` = `cs2`.`schedule_id`))) where (`cs2`.`class_id` = `cc`.`class_id`)) AS `total_attendance_records`,(select count(0) from (`attendance` `a` join `course_schedule` `cs2` on((`a`.`schedule_id` = `cs2`.`schedule_id`))) where ((`cs2`.`class_id` = `cc`.`class_id`) and (`a`.`status` in ('出勤','迟到')))) AS `attended_times`,round(ifnull((((select count(0) from (`attendance` `a` join `course_schedule` `cs2` on((`a`.`schedule_id` = `cs2`.`schedule_id`))) where ((`cs2`.`class_id` = `cc`.`class_id`) and (`a`.`status` in ('出勤','迟到')))) / nullif((select count(0) from (`attendance` `a` join `course_schedule` `cs2` on((`a`.`schedule_id` = `cs2`.`schedule_id`))) where (`cs2`.`class_id` = `cc`.`class_id`)),0)) * 100),2),2) AS `attendance_rate` from (`course_class` `cc` join `course_category` `cat` on((`cc`.`category_id` = `cat`.`category_id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_knowledge_all`
--

/*!50001 DROP VIEW IF EXISTS `v_knowledge_all`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_knowledge_all` AS select 'ADMIN' AS `kb_scope`,`kb_admin_doc`.`kb_id` AS `kb_id`,`kb_admin_doc`.`title` AS `title`,`kb_admin_doc`.`category` AS `category`,`kb_admin_doc`.`content` AS `content`,`kb_admin_doc`.`summary` AS `summary`,`kb_admin_doc`.`tags` AS `tags`,`kb_admin_doc`.`source_type` AS `source_type`,`kb_admin_doc`.`source_ref` AS `source_ref`,`kb_admin_doc`.`owner_id` AS `owner_id`,`kb_admin_doc`.`owner_name` AS `owner_name`,`kb_admin_doc`.`file_url` AS `file_url`,`kb_admin_doc`.`file_name` AS `file_name`,`kb_admin_doc`.`status` AS `status`,`kb_admin_doc`.`view_count` AS `view_count`,`kb_admin_doc`.`create_time` AS `create_time`,`kb_admin_doc`.`update_time` AS `update_time` from `kb_admin_doc` union all select 'TEACHER' AS `kb_scope`,`kb_teacher_doc`.`kb_id` AS `kb_id`,`kb_teacher_doc`.`title` AS `title`,`kb_teacher_doc`.`category` AS `category`,`kb_teacher_doc`.`content` AS `content`,`kb_teacher_doc`.`summary` AS `summary`,`kb_teacher_doc`.`tags` AS `tags`,`kb_teacher_doc`.`source_type` AS `source_type`,`kb_teacher_doc`.`source_ref` AS `source_ref`,`kb_teacher_doc`.`owner_id` AS `owner_id`,`kb_teacher_doc`.`owner_name` AS `owner_name`,`kb_teacher_doc`.`file_url` AS `file_url`,`kb_teacher_doc`.`file_name` AS `file_name`,`kb_teacher_doc`.`status` AS `status`,`kb_teacher_doc`.`view_count` AS `view_count`,`kb_teacher_doc`.`create_time` AS `create_time`,`kb_teacher_doc`.`update_time` AS `update_time` from `kb_teacher_doc` union all select 'STUDENT' AS `kb_scope`,`kb_student_doc`.`kb_id` AS `kb_id`,`kb_student_doc`.`title` AS `title`,`kb_student_doc`.`category` AS `category`,`kb_student_doc`.`content` AS `content`,`kb_student_doc`.`summary` AS `summary`,`kb_student_doc`.`tags` AS `tags`,`kb_student_doc`.`source_type` AS `source_type`,`kb_student_doc`.`source_ref` AS `source_ref`,`kb_student_doc`.`owner_id` AS `owner_id`,`kb_student_doc`.`owner_name` AS `owner_name`,`kb_student_doc`.`file_url` AS `file_url`,`kb_student_doc`.`file_name` AS `file_name`,`kb_student_doc`.`status` AS `status`,`kb_student_doc`.`view_count` AS `view_count`,`kb_student_doc`.`create_time` AS `create_time`,`kb_student_doc`.`update_time` AS `update_time` from `kb_student_doc` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-16 11:02:41

