-- =====================================================================
-- 手机号统一为 11 位中国大陆手机号：数据库层 CHECK 约束（MySQL 8.0，可重复执行）
-- 适用库：art_training
-- 作者：liukang&&heyan
-- =====================================================================
USE art_training;

-- 1. 先核查存量数据是否全部合规（不合规需先人工订正再加约束）
SELECT 'sys_user'        AS tbl, phone AS phone FROM sys_user
UNION ALL SELECT 'teacher',           phone FROM teacher
UNION ALL SELECT 'teacher_emergency', emergency_phone FROM teacher
UNION ALL SELECT 'student_parent',    parent_phone FROM student
UNION ALL SELECT 'visitor_booking',   phone FROM visitor_booking
UNION ALL SELECT 'exam_registration', phone FROM exam_registration;

-- 2. 幂等删除旧约束的存储过程（MySQL 8.0 不支持 DROP CHECK IF EXISTS）
DROP PROCEDURE IF EXISTS p_drop_check;
DELIMITER $$
CREATE PROCEDURE p_drop_check(IN p_table VARCHAR(64), IN p_check VARCHAR(64))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
               WHERE CONSTRAINT_SCHEMA = DATABASE()
                 AND TABLE_NAME = p_table AND CONSTRAINT_NAME = p_check) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` DROP CHECK `', p_check, '`');
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 3. 加 CHECK 约束（允许为空；非空时必须 11 位、1 开头第二位 3-9）
CALL p_drop_check('sys_user', 'chk_sys_user_phone');
ALTER TABLE sys_user
    ADD CONSTRAINT chk_sys_user_phone CHECK (phone IS NULL OR phone REGEXP '^1[3-9][0-9]{9}$');

CALL p_drop_check('teacher', 'chk_teacher_phone');
CALL p_drop_check('teacher', 'chk_teacher_emergency_phone');
ALTER TABLE teacher
    ADD CONSTRAINT chk_teacher_phone CHECK (phone IS NULL OR phone REGEXP '^1[3-9][0-9]{9}$'),
    ADD CONSTRAINT chk_teacher_emergency_phone CHECK (emergency_phone IS NULL OR emergency_phone REGEXP '^1[3-9][0-9]{9}$');

CALL p_drop_check('student', 'chk_student_parent_phone');
ALTER TABLE student
    ADD CONSTRAINT chk_student_parent_phone CHECK (parent_phone IS NULL OR parent_phone REGEXP '^1[3-9][0-9]{9}$');

CALL p_drop_check('visitor_booking', 'chk_visitor_phone');
ALTER TABLE visitor_booking
    ADD CONSTRAINT chk_visitor_phone CHECK (phone IS NULL OR phone REGEXP '^1[3-9][0-9]{9}$');

CALL p_drop_check('exam_registration', 'chk_exam_reg_phone');
ALTER TABLE exam_registration
    ADD CONSTRAINT chk_exam_reg_phone CHECK (phone IS NULL OR phone REGEXP '^1[3-9][0-9]{9}$');

DROP PROCEDURE IF EXISTS p_drop_check;
