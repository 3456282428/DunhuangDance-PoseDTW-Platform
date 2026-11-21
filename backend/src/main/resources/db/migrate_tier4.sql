-- ============================================================
-- 第4档结构优化迁移脚本
-- #5 payment 多态外键：CHECK 由"必填"升级为"必填+分支互斥+退款继承业务键"
-- #6 请假申请与考勤硬关联：attendance 增加 request_id 外键并回填历史
-- ============================================================
USE art_training;

-- ========== #5 payment 互斥约束 ==========
ALTER TABLE `payment` DROP CHECK `chk_pay_fk_consistency`;

ALTER TABLE `payment` ADD CONSTRAINT `chk_pay_fk_consistency` CHECK (
    (`pay_type` = _utf8mb4'其他')
    OR ((`pay_type` = _utf8mb4'退款')
        AND ((`enrollment_id` IS NOT NULL) OR (`reg_id` IS NOT NULL)))
    OR ((`pay_type` IN (_utf8mb4'报名',_utf8mb4'续费'))
        AND (`enrollment_id` IS NOT NULL)
        AND (`student_id` IS NOT NULL)
        AND (`class_id` IS NOT NULL)
        AND (`reg_id` IS NULL))
    OR ((`pay_type` IN (_utf8mb4'考级',_utf8mb4'比赛'))
        AND (`reg_id` IS NOT NULL)
        AND (`student_id` IS NOT NULL)
        AND (`enrollment_id` IS NULL))
);

-- ========== #6 attendance.request_id 关联请假申请 ==========
ALTER TABLE `attendance`
  ADD COLUMN `request_id` int DEFAULT NULL COMMENT '关联请假申请 student_request.request_id（请假批准自动登记时回填）' AFTER `student_id`,
  ADD KEY `idx_att_request` (`request_id`),
  ADD CONSTRAINT `fk_att_request` FOREIGN KEY (`request_id`) REFERENCES `student_request` (`request_id`);

-- 历史回填：已批准请假申请当天的"请假"考勤
UPDATE `attendance` a
JOIN `course_schedule` cs ON cs.schedule_id = a.schedule_id
JOIN `student_request` r ON r.student_id = a.student_id
                         AND r.request_type = '请假'
                         AND r.status = '已批准'
                         AND r.request_date = cs.course_date
SET a.request_id = r.request_id
WHERE a.status = '请假';
