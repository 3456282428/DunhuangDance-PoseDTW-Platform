-- =====================================================
-- 知识库与AI模型管理 · 可视化仪表盘数据库迁移脚本（migrate_tech_dashboard.sql）
-- -----------------------------------------------------
-- 内容：
--   1) 开源组件登记表（GitHub/Gitee 组件拉取、版本追踪、更新状态监控）
--   2) 开源组件版本迭代表（版本历史留痕）
--   3) 仪表盘拓扑边表（组件依赖关系 + AI模型↔知识库关联，统一供拓扑图渲染）
--   4) AI模型性能指标时序表（延迟/吞吐/准确率/成功率/知识库命中，实时曲线数据源）
--   5) 仪表盘操作日志表（同步/升级/导出等管理操作留痕）
-- 环境：MySQL 8.0+，库 art_training
-- 用法：mysql -uroot art_training < migrate_tech_dashboard.sql
-- =====================================================

USE art_training;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `dashboard_operation_log`;
DROP TABLE IF EXISTS `ai_model_metric`;
DROP TABLE IF EXISTS `dashboard_topology`;
DROP TABLE IF EXISTS `open_component_version`;
DROP TABLE IF EXISTS `open_component`;

-- =====================================================
-- 一、开源组件登记表（对应 GitHub / Gitee 代码仓库）
-- =====================================================
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
  `star_count` int DEFAULT 0 COMMENT 'Star数',
  `fork_count` int DEFAULT 0 COMMENT 'Fork数',
  `open_issue_count` int DEFAULT 0 COMMENT '开放Issue数',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开源组件登记与版本追踪';

-- =====================================================
-- 二、开源组件版本迭代表
-- =====================================================
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开源组件版本迭代记录';

-- =====================================================
-- 三、仪表盘拓扑边表（组件依赖 + AI模型↔知识库，统一渲染拓扑图）
-- =====================================================
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仪表盘拓扑关系（组件依赖/AI模型知识库关联）';

-- =====================================================
-- 四、AI模型性能指标时序表（实时曲线数据源）
-- =====================================================
CREATE TABLE `ai_model_metric` (
  `metric_id` bigint NOT NULL AUTO_INCREMENT,
  `model_id` int NOT NULL COMMENT 'AI模型ID（ai_model.model_id）',
  `metric_time` datetime NOT NULL COMMENT '采样时间',
  `latency_ms` decimal(10,2) DEFAULT NULL COMMENT '平均响应延迟(ms)',
  `throughput_qps` decimal(10,2) DEFAULT NULL COMMENT '吞吐量(次/分钟)',
  `accuracy` decimal(6,4) DEFAULT NULL COMMENT '准确率(0-1)',
  `success_rate` decimal(6,4) DEFAULT NULL COMMENT '调用成功率(0-1)',
  `call_count` int DEFAULT 0 COMMENT '采样周期内调用次数',
  `kb_hit_avg` decimal(6,2) DEFAULT NULL COMMENT '知识库平均命中条数',
  `metric_source` varchar(20) DEFAULT 'SAMPLE' COMMENT '来源：SAMPLE系统采样 / SEED初始化 / MANUAL人工',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`metric_id`),
  KEY `idx_model_time` (`model_id`,`metric_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型性能指标时序';

-- =====================================================
-- 五、仪表盘操作日志表
-- =====================================================
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仪表盘管理操作日志';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 六、初始化数据：本系统实际依赖的开源组件（含 GitHub 与 Gitee 来源）
-- =====================================================
INSERT INTO `open_component`
(`name`,`platform`,`repo_owner`,`repo_name`,`repo_url`,`category`,`language`,`license`,`used_by_module`,
 `current_version`,`latest_version`,`latest_release_date`,`update_status`,`star_count`,`fork_count`,`open_issue_count`,
 `description`,`homepage`,`sync_enabled`,`last_sync_status`) VALUES
('MediaPipe','GITHUB','google-ai-edge','mediapipe','https://github.com/google-ai-edge/mediapipe','姿态识别','C++/Python','Apache-2.0','AI动作评分-姿态提取','0.10.14','0.10.14','2024-11-12','UP_TO_DATE',29500,4900,320,'跨平台机器学习管线框架，提供33关键点人体姿态估计','https://mediapipe.dev',1,'OK'),
('OpenCV','GITHUB','opencv','opencv','https://github.com/opencv/opencv','视频处理','C++','Apache-2.0','视频预处理-帧率统一与裁剪','4.10.0','4.11.0','2025-01-15','UPDATE_AVAILABLE',78900,56000,2400,'计算机视觉基础库，负责视频解码编码与图像处理','https://opencv.org',1,'OK'),
('NumPy','GITHUB','numpy','numpy','https://github.com/numpy/numpy','数值计算','Python/C','BSD-3-Clause','评分算法-矩阵与角度计算','1.26.4','2.2.1','2025-01-18','UPDATE_AVAILABLE',28600,7900,1900,'Python科学计算基础库，提供高效多维数组运算','https://numpy.org',1,'OK'),
('Spring Boot','GITHUB','spring-projects','spring-boot','https://github.com/spring-projects/spring-boot','后端框架','Java','Apache-2.0','后端服务-Web与依赖注入','3.2.5','3.4.2','2025-01-23','UPDATE_AVAILABLE',75500,41300,620,'Spring生态快速开发脚手架','https://spring.io/projects/spring-boot',1,'OK'),
('MyBatis-3','GITHUB','mybatis','mybatis-3','https://github.com/mybatis/mybatis-3','数据访问','Java','Apache-2.0','数据访问层-SQL映射','3.5.16','3.5.18','2025-01-08','UPDATE_AVAILABLE',19800,12800,80,'半自动ORM框架，XML映射SQL','https://mybatis.org',1,'OK'),
('Vue.js','GITHUB','vuejs','core','https://github.com/vuejs/core','前端框架','TypeScript','MIT','前端-三端页面框架','3.4.21','3.5.13','2025-01-06','UPDATE_AVAILABLE',48300,8400,780,'渐进式JavaScript框架','https://vuejs.org',1,'OK'),
('Element Plus','GITHUB','element-plus','element-plus','https://github.com/element-plus/element-plus','UI组件库','TypeScript','MIT','前端-管理端UI组件','2.14.5','2.14.5','2024-12-20','UP_TO_DATE',25600,3400,1500,'Vue3企业级UI组件库','https://element-plus.org',1,'OK'),
('Apache ECharts','GITHUB','apache','echarts','https://github.com/apache/echarts','数据可视化','TypeScript','Apache-2.0','可视化仪表盘-全部图表渲染','6.1.0','6.1.0','2024-12-18','UP_TO_DATE',61200,19700,2100,'开源可视化图表库','https://echarts.apache.org',1,'OK'),
('RuoYi','GITEE','y_project','RuoYi','https://gitee.com/y_project/RuoYi','权限管理参考','Java','MIT','权限模型-角色菜单设计参考','4.7.9','4.8.0','2025-01-10','UPDATE_AVAILABLE',3800,2100,60,'基于Spring Boot与MyBatis的权限管理框架','https://ruoyi.vip',1,'OK'),
('MindSpore','GITEE','mindspore','mindspore','https://gitee.com/mindspore/mindspore','模型训练','Python/C++','Apache-2.0','AI评分模型-训练方案备选','2.3.1','2.4.0','2025-01-20','UPDATE_AVAILABLE',1200,560,140,'全场景深度学习训练与推理框架','https://mindspore.cn',1,'OK');

-- 版本迭代历史（每组件保留已用版本 + 可升级版本）
INSERT INTO `open_component_version` (`component_id`,`version`,`release_date`,`changelog`,`is_current`,`detect_source`)
SELECT c.component_id, c.latest_version, c.latest_release_date, CONCAT(c.name, ' 最新版本发布（远端拉取）'), 0, 'API' FROM `open_component` c WHERE c.latest_version IS NOT NULL;

INSERT INTO `open_component_version` (`component_id`,`version`,`release_date`,`changelog`,`is_current`,`detect_source`) VALUES
(1,'0.9.0','2023-03-01','改进面部关键点稳定性',0,'SEED'),
(1,'0.10.0','2023-06-15','新增Pose Landmarker任务API',0,'SEED'),
(1,'0.10.14','2024-11-12','修复视频追踪模式跨序列状态复用问题（当前使用）',1,'SEED'),
(2,'4.8.1','2024-02-10','DNN模块稳定版',0,'SEED'),
(2,'4.10.0','2024-06-02','增强视频解码与GPU加速（当前使用）',1,'SEED'),
(3,'1.24.4','2023-12-18','长期支持版',0,'SEED'),
(3,'1.26.4','2024-09-06','性能与稳定性改进（当前使用）',1,'SEED'),
(4,'3.2.0','2024-01-18','Spring Framework 6.1 基线',0,'SEED'),
(4,'3.2.5','2024-04-11','安全补丁与缺陷修复（当前使用）',1,'SEED'),
(6,'3.4.21','2024-02-28','响应式系统增强（当前使用）',1,'SEED'),
(7,'2.7.0','2024-03-20','表单与表格组件重构',0,'SEED'),
(7,'2.14.5','2024-12-20','组件完善与类型声明补全（当前使用）',1,'SEED'),
(8,'5.5.1','2024-02-14','渲染引擎优化',0,'SEED'),
(8,'6.1.0','2024-12-18','全新主题与性能提升（当前使用）',1,'SEED'),
(9,'4.7.0','2024-06-01','代码生成器升级',0,'SEED'),
(9,'4.7.9','2024-12-05','权限缓存与菜单优化（当前使用）',1,'SEED'),
(10,'2.2.0','2024-04-01','图算融合优化',0,'SEED'),
(10,'2.3.1','2024-10-15','分布式训练改进（当前使用）',1,'SEED');

-- 拓扑关系：组件依赖 + AI模型↔知识库关联约定
INSERT INTO `dashboard_topology`
(`source_type`,`source_key`,`target_type`,`target_key`,`relation`,`dep_type`,`version_constraint`,`remark`) VALUES
('COMPONENT','MediaPipe','COMPONENT','NumPy','depends_on','runtime','>=1.24','姿态关键点数组运算'),
('COMPONENT','MediaPipe','COMPONENT','OpenCV','depends_on','runtime','>=4.8','视频帧读取与图像预处理'),
('COMPONENT','OpenCV','COMPONENT','NumPy','depends_on','runtime','>=1.24','图像矩阵运算'),
('COMPONENT','Element Plus','COMPONENT','Vue.js','depends_on','peer','>=3.4','Vue3组件库同级依赖'),
('COMPONENT','Apache ECharts','COMPONENT','Vue.js','depends_on','peer','>=3.4','仪表盘图表挂载于Vue组件'),
('COMPONENT','Spring Boot','COMPONENT','MyBatis-3','depends_on','build','>=3.5','ORM集成'),
('COMPONENT','RuoYi','COMPONENT','Spring Boot','depends_on','build','>=3.2','权限管理框架技术栈'),
('COMPONENT','RuoYi','COMPONENT','MyBatis-3','depends_on','build','>=3.5','权限管理框架技术栈'),
('COMPONENT','MindSpore','COMPONENT','NumPy','depends_on','runtime','>=1.24','张量与数组互操作'),
('AI_MODEL','platform-knowledge-llm','KB_SCOPE','ADMIN','retrieves','runtime',NULL,'AI回答优先检索管理员端知识库'),
('AI_MODEL','platform-knowledge-llm','KB_SCOPE','TEACHER','retrieves','runtime',NULL,'AI回答优先检索教师端知识库'),
('AI_MODEL','platform-knowledge-llm','KB_SCOPE','STUDENT','retrieves','runtime',NULL,'AI回答优先检索学生端知识库'),
('AI_MODEL','platform-knowledge-llm','COMPONENT','MediaPipe','invokes','runtime',NULL,'动作评分链路调用姿态提取'),
('AI_MODEL','platform-knowledge-llm','COMPONENT','NumPy','invokes','runtime',NULL,'评分特征数值计算');

-- AI模型性能指标时序（近30分钟，1分钟粒度，供实时曲线初始化展示）
INSERT INTO `ai_model_metric`
(`model_id`,`metric_time`,`latency_ms`,`throughput_qps`,`accuracy`,`success_rate`,`call_count`,`kb_hit_avg`,`metric_source`)
SELECT 1,
       DATE_SUB(NOW(), INTERVAL (x.a*6 + y.b) MINUTE),
       ROUND(880 + 210*SIN((x.a*6+y.b)/4.5) + (x.a*6+y.b)%7*12, 1),
       ROUND(4.2 + 1.5*COS((x.a*6+y.b)/5.0) + (x.a*6+y.b)%3*0.35, 2),
       ROUND(0.945 + 0.018*SIN((x.a*6+y.b)/6.0), 4),
       ROUND(0.972 + 0.02*COS((x.a*6+y.b)/7.0), 4),
       6 + ((x.a*6+y.b)*7)%18,
       ROUND(1.8 + 1.5*ABS(SIN((x.a*6+y.b)/5.0)), 2),
       'SEED'
FROM (SELECT 0 AS a UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) x
CROSS JOIN (SELECT 0 AS b UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) y;

-- 仪表盘操作日志初始记录
INSERT INTO `dashboard_operation_log` (`op_type`,`op_target`,`op_detail`,`op_result`,`op_username`,`op_role`) VALUES
('SYNC_COMPONENT','开源组件（10个）','初始化组件登记与版本追踪数据','SUCCESS','SYSTEM','ADMIN');

-- =====================================================
-- 七、AI模型版本迭代扩展（仪表盘「AI模型列表与版本迭代记录」数据源）
-- 说明：本段含对已存在表 ai_model 的字段补充，重复执行会报「列已存在」，
--       属正常现象；如需重跑请先执行对应 DROP COLUMN。
-- =====================================================
DROP TABLE IF EXISTS `ai_model_version`;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型版本迭代记录';

ALTER TABLE `ai_model`
  ADD COLUMN `current_version` varchar(50) DEFAULT NULL COMMENT '当前运行版本',
  ADD COLUMN `latest_version` varchar(50) DEFAULT NULL COMMENT '可升级到的版本',
  ADD COLUMN `update_status` varchar(20) NOT NULL DEFAULT 'UNKNOWN' COMMENT '更新状态：UP_TO_DATE / UPDATE_AVAILABLE / UNKNOWN',
  ADD COLUMN `last_upgrade_time` datetime DEFAULT NULL COMMENT '最近一次升级登记时间';

UPDATE `ai_model` SET `current_version`='1.0.0', `latest_version`='1.1.0', `update_status`='UPDATE_AVAILABLE'
WHERE `model_id` = 1;

INSERT INTO `ai_model_version` (`model_id`,`version`,`release_date`,`changelog`,`is_current`,`upgrade_status`) VALUES
(1,'1.0.0','2026-03-01','初始版本：知识库优先检索 + 调用全量留痕，生成能力待接入',1,'APPLIED'),
(1,'1.1.0','2026-09-20','拟支持：检索结果重排 + 多轮上下文拼接（等待大模型接入后启用）',0,'PENDING');

