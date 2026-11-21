-- ============================================================
-- 第3档结构优化迁移脚本
-- #3 teacher.course_ids 逗号串 -> teacher_course 关联表（符合1NF）
-- #4 删除冗余台账 exam_competition，成果统一由 exam_registration JOIN exam_project 得出
-- ============================================================
USE art_training;

-- ========== #3 教师-课程 多对多关联表 ==========
CREATE TABLE IF NOT EXISTS `teacher_course` (
  `teacher_id` int NOT NULL COMMENT '教师ID',
  `course_id`  int NOT NULL COMMENT '擅长课程ID',
  PRIMARY KEY (`teacher_id`,`course_id`),
  KEY `idx_tc_course` (`course_id`),
  CONSTRAINT `fk_tc_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`teacher_id`),
  CONSTRAINT `fk_tc_course`  FOREIGN KEY (`course_id`)  REFERENCES `course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师擅长课程关联表';

-- 逗号串拆行迁移（MySQL 8 递归 CTE）
INSERT INTO `teacher_course`(`teacher_id`,`course_id`)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n < 20)
SELECT t.teacher_id,
       CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(t.course_ids,',',s.n),',',-1) AS UNSIGNED) AS course_id
FROM `teacher` t
JOIN seq s ON s.n <= 1 + LENGTH(t.course_ids) - LENGTH(REPLACE(t.course_ids,',',''))
WHERE t.course_ids IS NOT NULL AND t.course_ids <> ''
  AND SUBSTRING_INDEX(SUBSTRING_INDEX(t.course_ids,',',s.n),',',-1) REGEXP '^[0-9]+$';

-- 迁移核对后删除违反1NF的旧列
ALTER TABLE `teacher` DROP COLUMN `course_ids`;

-- ========== #4 删除冗余成果台账 ==========
-- exam_competition 的 student_id/class_id/activity_name/activity_type/result/award
-- 全部可由 exam_registration JOIN exam_project 得到，成果查询改走关联视图
DROP TABLE IF EXISTS `exam_competition`;
