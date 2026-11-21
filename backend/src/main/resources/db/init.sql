-- =====================================================
-- 艺术培训学员与课程管理系统 · 权威数据库初始化脚本（init.sql）
-- -----------------------------------------------------
-- 基线：schema_live.sql（由真实运行库 SHOW CREATE 逆向导出）
--       并与当前运行库 art_training 逐项核对一致，
--       包含后续 migrate_optimize.sql / migrate_phone_check.sql
--       已在运行库落地的全部结构（状态枚举 CHECK、手机号 CHECK 等）。
-- 内容：25 张基表 + 1 个视图 + 3 个触发器 + 1 个存储过程；仅结构 + 最小默认配置
-- 数据：业务演示数据统一由 seed_clean.sql 导入，本脚本不插入业务数据
-- 环境：MySQL 8.0+（使用了 CHECK 约束、REGEXP_LIKE、utf8mb4_0900 排序规则）
-- 用法：mysql -uroot < init.sql，随后执行 seed_clean.sql 导入演示数据
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS art_training DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE art_training;

-- ==================== 清理旧对象（视图先于表） ====================
DROP VIEW IF EXISTS `v_course_attendance_rate`;
DROP PROCEDURE IF EXISTS `sp_student_annual_records`;
DROP TRIGGER IF EXISTS `trg_attendance_after_insert`;
DROP TRIGGER IF EXISTS `trg_attendance_after_update`;
DROP TRIGGER IF EXISTS `trg_attendance_after_delete`;
DROP TABLE IF EXISTS `appointment`;
DROP TABLE IF EXISTS `attendance`;
DROP TABLE IF EXISTS `classroom`;
DROP TABLE IF EXISTS `course`;
DROP TABLE IF EXISTS `course_category`;
DROP TABLE IF EXISTS `course_class`;
DROP TABLE IF EXISTS `course_schedule`;
DROP TABLE IF EXISTS `enrollment`;
DROP TABLE IF EXISTS `teacher_course`;
DROP TABLE IF EXISTS `exam_project`;
DROP TABLE IF EXISTS `exam_registration`;
DROP TABLE IF EXISTS `feedback`;
DROP TABLE IF EXISTS `homework_submission`;
DROP TABLE IF EXISTS `homework`;
DROP TABLE IF EXISTS `message`;
DROP TABLE IF EXISTS `news`;
DROP TABLE IF EXISTS `payment`;
DROP TABLE IF EXISTS `reschedule_request`;
DROP TABLE IF EXISTS `student_comment`;
DROP TABLE IF EXISTS `student_request`;
DROP TABLE IF EXISTS `student`;
DROP TABLE IF EXISTS `system_config`;
DROP TABLE IF EXISTS `teacher`;
DROP TABLE IF EXISTS `visitor_booking`;
DROP TABLE IF EXISTS `sys_user`;

-- =====================================================
-- 一、账号与人员域
-- =====================================================

-- 1. 系统用户表（登录账号：管理员/教师/学员/财务/教务）
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';

-- 2. 教师表（与 sys_user 一对一关联登录账号）
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师表';

-- 3. 学员表（与 sys_user 一对一关联登录账号）
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学员表';

-- =====================================================
-- 二、教学资源域（教室/课程类别/课程产品/班级/排课）
-- =====================================================

-- 4. 教室表
CREATE TABLE `classroom` (
  `classroom_id` int NOT NULL AUTO_INCREMENT COMMENT '教室ID',
  `classroom_name` varchar(50) NOT NULL COMMENT '教室名称',
  `capacity` int DEFAULT NULL COMMENT '容量',
  `room_type` varchar(50) DEFAULT NULL COMMENT '教室类型（钢琴教室/舞蹈教室等）',
  `status` tinyint DEFAULT '1' COMMENT '状态：1可用 0停用',
  PRIMARY KEY (`classroom_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教室表';

-- 5. 课程类别表
CREATE TABLE `course_category` (
  `category_id` int NOT NULL AUTO_INCREMENT COMMENT '类别ID',
  `category_name` varchar(50) NOT NULL COMMENT '类别名称（乐器/舞蹈/美术/声乐）',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程类别表';

-- 6. 课程产品表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程产品';

-- 6.1 教师-擅长课程关联表（多对多，替代旧 teacher.course_ids 逗号串，符合1NF）
CREATE TABLE `teacher_course` (
  `teacher_id` int NOT NULL COMMENT '教师ID',
  `course_id` int NOT NULL COMMENT '擅长课程ID',
  PRIMARY KEY (`teacher_id`,`course_id`),
  KEY `idx_tc_course` (`course_id`),
  CONSTRAINT `fk_tc_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`teacher_id`),
  CONSTRAINT `fk_tc_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师擅长课程关联表';

-- 7. 班级表（一门课程可开多个班）
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级表';

-- 8. 排课表
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
  KEY `idx_course_date` (`course_date`),
  KEY `idx_teacher_date` (`teacher_id`,`course_date`),
  KEY `idx_classroom_date` (`classroom_id`,`course_date`),
  UNIQUE KEY `uk_teacher_time` (`teacher_id`,`course_date`,`start_time`),
  UNIQUE KEY `uk_classroom_time` (`classroom_id`,`course_date`,`start_time`),
  UNIQUE KEY `uk_class_time` (`class_id`,`course_date`,`start_time`),
  CONSTRAINT `course_schedule_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `course_class` (`class_id`),
  CONSTRAINT `course_schedule_ibfk_2` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`teacher_id`),
  CONSTRAINT `course_schedule_ibfk_3` FOREIGN KEY (`classroom_id`) REFERENCES `classroom` (`classroom_id`),
  CONSTRAINT `chk_sched_status` CHECK ((`status` in (_utf8mb4'待上课',_utf8mb4'已完成',_utf8mb4'已取消')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='排课表';

-- =====================================================
-- 三、教学运行域（报名/考勤/作业）
-- =====================================================

-- 9. 报名表（学员-班级 多对多，同一学员同一班唯一）
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报名表';

-- 10. 考勤表（同一排课同一学员唯一，触发器回写 enrollment 统计）
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
  CONSTRAINT `chk_att_status` CHECK ((`status` in (_utf8mb4'出勤',_utf8mb4'迟到',_utf8mb4'缺勤',_utf8mb4'请假')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考勤表';

-- 11. 作业表
CREATE TABLE `homework` (
  `homework_id` int NOT NULL AUTO_INCREMENT COMMENT '作业ID',
  `class_id` int NOT NULL COMMENT '班级ID',
  `schedule_id` int DEFAULT NULL COMMENT '关联排课ID（可选）',
  `title` varchar(100) DEFAULT NULL COMMENT '作业标题',
  `content` text COMMENT '作业内容/学生答案',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '布置时间',
  `deadline` datetime DEFAULT NULL COMMENT '截止时间',
  PRIMARY KEY (`homework_id`),
  KEY `schedule_id` (`schedule_id`),
  KEY `idx_class` (`class_id`),
  CONSTRAINT `homework_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `course_class` (`class_id`),
  CONSTRAINT `homework_ibfk_2` FOREIGN KEY (`schedule_id`) REFERENCES `course_schedule` (`schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业表';

-- 12. 作业提交记录表
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
  `status` varchar(20) DEFAULT '未提交',
  PRIMARY KEY (`submission_id`),
  UNIQUE KEY `uk_homework_student` (`homework_id`,`student_id`),
  KEY `fk_sub_student` (`student_id`),
  CONSTRAINT `fk_sub_homework` FOREIGN KEY (`homework_id`) REFERENCES `homework` (`homework_id`),
  CONSTRAINT `fk_sub_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`),
  CONSTRAINT `chk_hs_status` CHECK ((`status` in (_utf8mb4'未提交',_utf8mb4'已提交',_utf8mb4'已批改')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业提交记录';

-- =====================================================
-- 四、考级/比赛域
-- =====================================================

-- 13. 考级比赛项目表（管理员发布的可报名项目）
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考级比赛项目表';

-- 14. 考级比赛报名表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考级比赛报名表';

-- =====================================================
-- 五、财务域
-- =====================================================

-- 16. 缴费表（含退款字段，支持班级缴费与考级缴费）
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
  CONSTRAINT `chk_pay_status` CHECK ((`status` in (_utf8mb4'已支付',_utf8mb4'退款中',_utf8mb4'已退款'))),
  CONSTRAINT `chk_pay_type` CHECK ((`pay_type` in (_utf8mb4'报名',_utf8mb4'续费',_utf8mb4'考级',_utf8mb4'比赛',_utf8mb4'退款',_utf8mb4'其他'))),
  CONSTRAINT `chk_pay_fk_consistency` CHECK (((`pay_type` = _utf8mb4'其他') OR ((`pay_type` = _utf8mb4'退款') and ((`enrollment_id` is not null) or (`reg_id` is not null))) OR ((`pay_type` in (_utf8mb4'报名',_utf8mb4'续费')) and (`enrollment_id` is not null) and (`student_id` is not null) and (`class_id` is not null) and (`reg_id` is null)) OR ((`pay_type` in (_utf8mb4'考级',_utf8mb4'比赛')) and (`reg_id` is not null) and (`student_id` is not null) and (`enrollment_id` is null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='缴费表';

-- =====================================================
-- 六、预约/申请/互动域
-- =====================================================

-- 17. 预约表（学员试听/补课预约）
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预约表';

-- 18. 游客试听预约表（门户未登录访客）
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='游客试听预约表';

-- 19. 建议投诉反馈表
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
  CONSTRAINT `fk_fb_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`),
  CONSTRAINT `fk_fb_reply_user` FOREIGN KEY (`reply_user_id`) REFERENCES `sys_user` (`user_id`),
  CONSTRAINT `chk_fb_status` CHECK ((`status` in (_utf8mb4'待处理',_utf8mb4'已回复'))),
  CONSTRAINT `chk_fb_type` CHECK ((`type` in (_utf8mb4'建议',_utf8mb4'投诉',_utf8mb4'咨询',_utf8mb4'报修')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='建议投诉反馈表';

-- 20. 学员评语表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学员评语表';

-- 21. 学员请假/缓考/补考申请表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学员请假/缓考/补考申请表';

-- 考勤表对请假申请的关联外键（attendance 先于本表创建，故在此补加）
ALTER TABLE `attendance`
  ADD CONSTRAINT `fk_att_request` FOREIGN KEY (`request_id`) REFERENCES `student_request` (`request_id`);

-- 22. 调课申请表（含教师交换排课）
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='调课申请表';

-- =====================================================
-- 七、消息/资讯/配置域
-- =====================================================

-- 23. 站内消息通知表
CREATE TABLE `message` (
  `message_id` int NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `receiver_user_id` int NOT NULL COMMENT '接收人sys_user.user_id',
  `msg_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '系统' COMMENT '系统/作业/申请/考级/调课/预约',
  `title` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
  `content` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内容',
  `biz_type` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联业务类型',
  `biz_id` int DEFAULT NULL COMMENT '关联业务ID',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '0未读 1已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_msg_receiver` (`receiver_user_id`,`is_read`),
  CONSTRAINT `fk_msg_receiver` FOREIGN KEY (`receiver_user_id`) REFERENCES `sys_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='站内消息通知';

-- 24. 资讯表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资讯表';

-- 25. 系统配置表（键值存储：学期、课节模板、机构信息等）
CREATE TABLE `system_config` (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` varchar(50) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值(JSON)',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';

-- =====================================================
-- 八、视图：班级报名人数与出勤率统计
-- =====================================================
CREATE VIEW `v_course_attendance_rate` AS
SELECT
    cc.class_id,
    cc.class_name,
    cat.category_name,
    (SELECT COUNT(*) FROM enrollment e
     WHERE e.class_id = cc.class_id AND e.status = '在读') AS enrolled_count,
    (SELECT COUNT(*) FROM course_schedule cs
     WHERE cs.class_id = cc.class_id AND cs.status = '已完成') AS finished_schedule_count,
    (SELECT COUNT(*) FROM attendance a
     JOIN course_schedule cs2 ON a.schedule_id = cs2.schedule_id
     WHERE cs2.class_id = cc.class_id) AS total_attendance_records,
    (SELECT COUNT(*) FROM attendance a
     JOIN course_schedule cs2 ON a.schedule_id = cs2.schedule_id
     WHERE cs2.class_id = cc.class_id AND a.status IN ('出勤','迟到')) AS attended_times,
    ROUND(
        IFNULL(
            (SELECT COUNT(*) FROM attendance a
             JOIN course_schedule cs2 ON a.schedule_id = cs2.schedule_id
             WHERE cs2.class_id = cc.class_id AND a.status IN ('出勤','迟到'))
            / NULLIF((SELECT COUNT(*) FROM attendance a
             JOIN course_schedule cs2 ON a.schedule_id = cs2.schedule_id
             WHERE cs2.class_id = cc.class_id), 0) * 100, 2
        ), 2) AS attendance_rate
FROM course_class cc
JOIN course_category cat ON cc.category_id = cat.category_id;

-- =====================================================
-- 九、触发器：考勤增/改/删后自动回写报名出勤统计
-- =====================================================
DELIMITER $$

DROP TRIGGER IF EXISTS `trg_attendance_after_insert`$$
CREATE TRIGGER `trg_attendance_after_insert` AFTER INSERT ON `attendance` FOR EACH ROW
BEGIN
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
END$$

DROP TRIGGER IF EXISTS `trg_attendance_after_update`$$
CREATE TRIGGER `trg_attendance_after_update` AFTER UPDATE ON `attendance` FOR EACH ROW
BEGIN
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
END$$

DROP TRIGGER IF EXISTS `trg_attendance_after_delete`$$
CREATE TRIGGER `trg_attendance_after_delete` AFTER DELETE ON `attendance` FOR EACH ROW
BEGIN
    DECLARE v_class_id INT;
    SELECT class_id INTO v_class_id FROM course_schedule WHERE schedule_id = OLD.schedule_id;
    IF OLD.status IN ('出勤','迟到') THEN
        UPDATE enrollment SET attended_count = GREATEST(attended_count - 1, 0)
        WHERE student_id = OLD.student_id AND class_id = v_class_id AND status = '在读';
    ELSEIF OLD.status IN ('缺勤','请假') THEN
        UPDATE enrollment SET absent_count = GREATEST(absent_count - 1, 0)
        WHERE student_id = OLD.student_id AND class_id = v_class_id AND status = '在读';
    END IF;
END$$

-- =====================================================
-- 十、存储过程：查询某学员某年度课程与考勤记录
-- =====================================================
DROP PROCEDURE IF EXISTS `sp_student_annual_records`$$
CREATE PROCEDURE `sp_student_annual_records`(
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
END$$

DELIMITER ;

-- =====================================================
-- 十一、知识库语义检索 / 跨库关联 / Agent 多会话（migrate_kb_semantic.sql 同步）
-- =====================================================

-- 知识条目向量表（语义检索，冷启动免重算）
CREATE TABLE `kb_doc_vector` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `kb_scope` varchar(10) NOT NULL COMMENT '端别：ADMIN/TEACHER/STUDENT',
  `kb_id` int NOT NULL COMMENT '知识条目ID',
  `owner_id` int DEFAULT NULL COMMENT '归属人ID',
  `dim` int NOT NULL DEFAULT '256' COMMENT '向量维度',
  `model` varchar(60) NOT NULL DEFAULT 'local-hash-256' COMMENT '向量化模型标识',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '文本指纹（增量刷新判断）',
  `vec` mediumtext NOT NULL COMMENT '向量（逗号分隔 float，已 L2 归一化）',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '刷新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vec_doc` (`kb_scope`,`kb_id`),
  KEY `idx_vec_scope_owner` (`kb_scope`,`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识条目向量表（语义检索）';

-- 知识库关联映射表（ref 级关系）
CREATE TABLE `kb_relation` (
  `relation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `src_ref` varchar(100) NOT NULL COMMENT '源业务引用（如 schedule:12）',
  `dst_ref` varchar(100) NOT NULL COMMENT '目标业务引用（如 class:3）',
  `rel_type` varchar(30) NOT NULL COMMENT '关系类型',
  `src_label` varchar(60) DEFAULT NULL COMMENT '源实体名称',
  `dst_label` varchar(60) DEFAULT NULL COMMENT '目标实体名称',
  `weight` decimal(5,2) NOT NULL DEFAULT '1.00' COMMENT '关联强度',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (`relation_id`),
  UNIQUE KEY `uk_relation` (`src_ref`,`dst_ref`,`rel_type`),
  KEY `idx_rel_src` (`src_ref`),
  KEY `idx_rel_dst` (`dst_ref`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库关联映射表';

-- Agent 会话表（多段对话）
CREATE TABLE `agent_session` (
  `session_id` varchar(32) NOT NULL COMMENT '会话ID',
  `user_id` int NOT NULL COMMENT '归属用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '归属账号',
  `title` varchar(60) DEFAULT NULL COMMENT '会话标题',
  `msg_count` int NOT NULL DEFAULT '0' COMMENT '消息条数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
  PRIMARY KEY (`session_id`),
  KEY `idx_agent_sess_user` (`user_id`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent会话表';

-- Agent 会话消息表（上下文记忆持久化）
CREATE TABLE `agent_message` (
  `msg_id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` varchar(32) NOT NULL COMMENT '会话ID',
  `role` varchar(12) NOT NULL COMMENT '角色：user/assistant',
  `content` text NOT NULL COMMENT '消息内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`msg_id`),
  KEY `idx_agent_msg_session` (`session_id`,`msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent会话消息表';

-- =====================================================
-- 十二、最小运行默认配置（导入 seed_clean.sql 后会被演示配置覆盖）
-- =====================================================
INSERT IGNORE INTO system_config (config_key, config_value) VALUES
('semesters', '["2026春季","2026秋季"]'),
('organization', '{"name":"","address":"","phone":"","email":"","introduction":"","logoUrl":""}');

SET FOREIGN_KEY_CHECKS = 1;
