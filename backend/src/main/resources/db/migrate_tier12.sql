-- ============================================================
-- 第1、2档结构优化迁移脚本（幂等保护由执行流程保证）
-- 第1档：#1 course 外键 / #9 排课唯一约束 / #5 payment 一致性 CHECK
-- 第2档：#2 course.category_id 统一类别口径 / #7 回复人/发布人改 user_id 外键
-- 原则：保留旧文本列，新增权威 ID 列，业务行为不变
-- ============================================================
USE art_training;

-- ========== 第1档 ==========
-- #1 course_class.course_id 补外键并收紧为 NOT NULL（已验证零孤儿）
ALTER TABLE course_class
    ADD CONSTRAINT fk_class_course FOREIGN KEY (course_id) REFERENCES course(course_id),
    MODIFY COLUMN course_id INT NOT NULL COMMENT '所属课程ID';

-- #9 排课三重防重唯一约束（教师/教室/班级 同日同时段唯一，已验证零冲突）
ALTER TABLE course_schedule
    ADD UNIQUE KEY uk_teacher_time (teacher_id, course_date, start_time),
    ADD UNIQUE KEY uk_classroom_time (classroom_id, course_date, start_time),
    ADD UNIQUE KEY uk_class_time (class_id, course_date, start_time);

-- #5 payment 按 pay_type 保证对应外键非空；退款/其他豁免
ALTER TABLE payment
    ADD CONSTRAINT chk_pay_fk_consistency CHECK (
        pay_type IN ('退款','其他')
        OR (pay_type IN ('报名','续费')
            AND enrollment_id IS NOT NULL AND student_id IS NOT NULL AND class_id IS NOT NULL)
        OR (pay_type IN ('考级','比赛')
            AND reg_id IS NOT NULL AND student_id IS NOT NULL)
    );

-- ========== 第2档 ==========
-- #2 course 增加 category_id 外键，按类别名回填（已验证全部可匹配）
ALTER TABLE course ADD COLUMN category_id INT NULL COMMENT '类别ID' AFTER category;
UPDATE course c
JOIN course_category cc ON c.category = cc.category_name
SET c.category_id = cc.category_id;
ALTER TABLE course
    ADD CONSTRAINT fk_course_category FOREIGN KEY (category_id) REFERENCES course_category(category_id);

-- #7 feedback 回复人改为 user_id 外键（保留 reply_by 文本列兼容历史展示）
ALTER TABLE feedback ADD COLUMN reply_user_id INT NULL COMMENT '回复人用户ID' AFTER reply_by;
UPDATE feedback f JOIN sys_user u ON f.reply_by = u.real_name SET f.reply_user_id = u.user_id;
ALTER TABLE feedback
    ADD CONSTRAINT fk_fb_reply_user FOREIGN KEY (reply_user_id) REFERENCES sys_user(user_id);

-- #7 news 发布人改为 user_id 外键（保留 publisher 文本列兼容历史展示）
ALTER TABLE news ADD COLUMN publisher_id INT NULL COMMENT '发布人用户ID' AFTER publisher;
UPDATE news n JOIN sys_user u ON n.publisher = u.real_name SET n.publisher_id = u.user_id;
ALTER TABLE news
    ADD CONSTRAINT fk_news_publisher FOREIGN KEY (publisher_id) REFERENCES sys_user(user_id);
