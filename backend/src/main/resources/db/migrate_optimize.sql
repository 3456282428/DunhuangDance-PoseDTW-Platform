-- =====================================================================
-- 系统完备性优化 增量迁移脚本（在已有 art_training 库上执行，可重复执行）
-- 内容：
--   1. 统一状态词表并修正历史漂移数据
--   2. payment 增加考级关联、退款信息列；exam_competition 关联报名记录
--   3. reschedule_request 增加审批留痕列
--   4. 1:1 关系与业务编号唯一约束
--   5. 状态取值 CHECK 约束
--   6. 新增站内消息表 message
--   7. 按缴费流水重算 enrollment.total_paid
-- 作者：liukang&&heyan
-- =====================================================================
USE art_training;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 历史漂移数据归一化（以代码实际使用词表为准）
-- ---------------------------------------------------------------------
UPDATE appointment          SET status='已接受' WHERE status='已确认';
UPDATE student_request      SET status='待处理' WHERE status='待审批';
UPDATE student_request      SET status='已批准' WHERE status='已通过';
UPDATE reschedule_request   SET status='待审核' WHERE status='待审批';

-- ---------------------------------------------------------------------
-- 2. 结构补列（幂等：先删再加）
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_add_column;
DELIMITER $$
CREATE PROCEDURE p_add_column(IN p_table VARCHAR(64), IN p_col VARCHAR(64), IN p_ddl VARCHAR(500))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=p_table AND COLUMN_NAME=p_col) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
        PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;
END$$
DELIMITER ;

CALL p_add_column('payment','reg_id',
  "`reg_id` INT NULL COMMENT '关联考级/比赛报名ID(exam_registration)' AFTER `enrollment_id`");
CALL p_add_column('payment','refund_time',
  "`refund_time` DATETIME NULL COMMENT '退款时间' AFTER `status`");
CALL p_add_column('payment','refund_operator_id',
  "`refund_operator_id` INT NULL COMMENT '退款操作人sys_user.user_id' AFTER `refund_time`");
CALL p_add_column('exam_competition','reg_id',
  "`reg_id` INT NULL COMMENT '关联考级报名记录exam_registration.reg_id'");
CALL p_add_column('reschedule_request','handle_time',
  "`handle_time` DATETIME NULL COMMENT '审批时间' AFTER `apply_time`");
CALL p_add_column('reschedule_request','handle_remark',
  "`handle_remark` VARCHAR(255) NULL COMMENT '审批意见' AFTER `handle_time`");
CALL p_add_column('reschedule_request','handler_id',
  "`handler_id` INT NULL COMMENT '审批人sys_user.user_id' AFTER `handle_remark`");
CALL p_add_column('reschedule_request','request_type',
  "`request_type` VARCHAR(10) NOT NULL DEFAULT '调课' COMMENT '调课/交换' AFTER `schedule_id`");
CALL p_add_column('reschedule_request','target_schedule_id',
  "`target_schedule_id` INT NULL COMMENT '交换目标排课ID' AFTER `request_type`");
DROP PROCEDURE p_add_column;

-- 补外键（幂等）
DROP PROCEDURE IF EXISTS p_add_fk;
DELIMITER $$
CREATE PROCEDURE p_add_fk(IN p_table VARCHAR(64), IN p_name VARCHAR(64), IN p_sql VARCHAR(500))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
                   WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME=p_table AND CONSTRAINT_NAME=p_name) THEN
        SET @sql = p_sql; PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;
END$$
DELIMITER ;
CALL p_add_fk('payment','fk_pay_reg',
  'ALTER TABLE payment ADD CONSTRAINT fk_pay_reg FOREIGN KEY (reg_id) REFERENCES exam_registration(reg_id)');
CALL p_add_fk('payment','fk_pay_refund_op',
  'ALTER TABLE payment ADD CONSTRAINT fk_pay_refund_op FOREIGN KEY (refund_operator_id) REFERENCES sys_user(user_id)');
CALL p_add_fk('exam_competition','fk_ec_reg',
  'ALTER TABLE exam_competition ADD CONSTRAINT fk_ec_reg FOREIGN KEY (reg_id) REFERENCES exam_registration(reg_id)');
CALL p_add_fk('exam_registration','fk_examreg_class',
  'ALTER TABLE exam_registration ADD CONSTRAINT fk_examreg_class FOREIGN KEY (class_id) REFERENCES course_class(class_id)');
CALL p_add_fk('exam_registration','fk_examreg_teacher',
  'ALTER TABLE exam_registration ADD CONSTRAINT fk_examreg_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id)');
CALL p_add_fk('reschedule_request','fk_rr_handler',
  'ALTER TABLE reschedule_request ADD CONSTRAINT fk_rr_handler FOREIGN KEY (handler_id) REFERENCES sys_user(user_id)');
CALL p_add_fk('reschedule_request','fk_rr_target',
  'ALTER TABLE reschedule_request ADD CONSTRAINT fk_rr_target FOREIGN KEY (target_schedule_id) REFERENCES course_schedule(schedule_id)');
DROP PROCEDURE p_add_fk;

-- payment.reg_id 索引
DROP PROCEDURE IF EXISTS p_add_index;
DELIMITER $$
CREATE PROCEDURE p_add_index(IN p_table VARCHAR(64), IN p_idx VARCHAR(64), IN p_sql VARCHAR(500))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=p_table AND INDEX_NAME=p_idx) THEN
        SET @sql = p_sql; PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;
END$$
DELIMITER ;
CALL p_add_index('payment','idx_pay_reg','CREATE INDEX idx_pay_reg ON payment(reg_id)');
CALL p_add_index('reschedule_request','idx_rr_status','CREATE INDEX idx_rr_status ON reschedule_request(status)');

-- ---------------------------------------------------------------------
-- 3. 1:1 关系与业务编号唯一约束（NULL 可重复，不影响未绑账号档案）
-- ---------------------------------------------------------------------
CALL p_add_index('teacher','uk_teacher_user','CREATE UNIQUE INDEX uk_teacher_user ON teacher(user_id)');
CALL p_add_index('student','uk_student_user','CREATE UNIQUE INDEX uk_student_user ON student(user_id)');
CALL p_add_index('teacher','uk_teacher_no','CREATE UNIQUE INDEX uk_teacher_no ON teacher(teacher_no)');
CALL p_add_index('student','uk_student_no','CREATE UNIQUE INDEX uk_student_no ON student(student_no)');
DROP PROCEDURE p_add_index;

-- ---------------------------------------------------------------------
-- 4. 状态/枚举 CHECK 约束（幂等）
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_add_check;
DELIMITER $$
CREATE PROCEDURE p_add_check(IN p_table VARCHAR(64), IN p_name VARCHAR(64), IN p_expr VARCHAR(500))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
                   WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME=p_table
                     AND CONSTRAINT_NAME=p_name AND CONSTRAINT_TYPE='CHECK') THEN
        SET @sql = CONCAT('ALTER TABLE `',p_table,'` ADD CONSTRAINT ',p_name,' CHECK (',p_expr,')');
        PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;
END$$
DELIMITER ;
CALL p_add_check('enrollment','chk_enroll_status',     "status IN ('在读','退学','结课')");
CALL p_add_check('course_schedule','chk_sched_status',  "status IN ('待上课','已完成','已取消')");
CALL p_add_check('attendance','chk_att_status',         "status IN ('出勤','迟到','缺勤','请假')");
CALL p_add_check('payment','chk_pay_status',            "status IN ('已支付','退款中','已退款')");
CALL p_add_check('payment','chk_pay_type',              "pay_type IN ('报名','续费','考级','比赛','退款','其他')");
CALL p_add_check('feedback','chk_fb_type',              "type IN ('建议','投诉','咨询','报修')");
CALL p_add_check('feedback','chk_fb_status',            "status IN ('待处理','已回复')");
CALL p_add_check('student_request','chk_sr_status',     "status IN ('待处理','已批准','已拒绝')");
CALL p_add_check('student_request','chk_sr_type',       "request_type IN ('请假','缓考','补考','休学','其他')");
CALL p_add_check('appointment','chk_app_status',        "status IN ('待确认','已接受','已拒绝','已取消')");
CALL p_add_check('homework_submission','chk_hs_status', "status IN ('未提交','已提交','已批改')");
CALL p_add_check('exam_registration','chk_er_status',   "status IN ('待审核','已确认','已驳回','已缴费','已完成','已取消')");
CALL p_add_check('visitor_booking','chk_vb_status',     "status IN ('待确认','已确认','已取消')");
CALL p_add_check('reschedule_request','chk_rr_status',  "status IN ('待审核','已通过','已拒绝')");
DROP PROCEDURE p_add_check;

-- ---------------------------------------------------------------------
-- 5. 站内消息表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS message (
    message_id        INT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    receiver_user_id  INT NOT NULL COMMENT '接收人sys_user.user_id',
    msg_type          VARCHAR(20) NOT NULL DEFAULT '系统' COMMENT '系统/作业/申请/考级/调课/预约',
    title             VARCHAR(100) NOT NULL COMMENT '标题',
    content           VARCHAR(500) COMMENT '内容',
    biz_type          VARCHAR(30) COMMENT '关联业务类型',
    biz_id            INT COMMENT '关联业务ID',
    is_read           TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    create_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_msg_receiver FOREIGN KEY (receiver_user_id) REFERENCES sys_user(user_id),
    INDEX idx_msg_receiver (receiver_user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='站内消息通知';

-- ---------------------------------------------------------------------
-- 6. 按 payment 流水重算 enrollment.total_paid（报名/续费/考级/比赛为正，退款为负）
-- ---------------------------------------------------------------------
UPDATE enrollment e
LEFT JOIN (
    SELECT enrollment_id, COALESCE(SUM(amount),0) AS pay_sum
    FROM payment
    WHERE enrollment_id IS NOT NULL AND status IN ('已支付','已退款')
    GROUP BY enrollment_id
) p ON p.enrollment_id = e.enrollment_id
SET e.total_paid = COALESCE(p.pay_sum, 0);

-- ---------------------------------------------------------------------
-- 7. 回填 course_class.category_id（按课程 category 名称匹配类别表）
-- ---------------------------------------------------------------------
UPDATE course_class cc
JOIN course c ON cc.course_id = c.course_id
JOIN course_category cat ON cat.category_name = c.category
SET cc.category_id = cat.category_id
WHERE cc.category_id IS NULL;
