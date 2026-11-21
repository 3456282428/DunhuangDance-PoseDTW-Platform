-- ============================================================
-- 敦煌主题干净演示数据（基准日 2026-10-20 周二，账号密码均为 123456）
-- 清空范围：全部业务表 + 知识库业务表 + Agent 会话 + 访问日志
-- 保留范围：AI 模型管理相关（ai_model / ai_model_version / ai_model_metric
--           / ai_model_kb_binding / ai_invoke_log / ai_agent_log）
-- ============================================================
SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS=0;

-- ---------- 业务核心表 ----------
TRUNCATE TABLE message;
TRUNCATE TABLE reschedule_request;
TRUNCATE TABLE student_comment;
TRUNCATE TABLE student_request;
TRUNCATE TABLE feedback;
TRUNCATE TABLE appointment;
TRUNCATE TABLE visitor_booking;
TRUNCATE TABLE payment;
TRUNCATE TABLE teacher_course;
TRUNCATE TABLE exam_registration;
TRUNCATE TABLE exam_project;
TRUNCATE TABLE homework_submission;
TRUNCATE TABLE homework;
TRUNCATE TABLE attendance;
TRUNCATE TABLE enrollment;
TRUNCATE TABLE course_schedule;
TRUNCATE TABLE course_class;
TRUNCATE TABLE course;
TRUNCATE TABLE course_category;
TRUNCATE TABLE classroom;
TRUNCATE TABLE student;
TRUNCATE TABLE teacher;
TRUNCATE TABLE sys_user;
TRUNCATE TABLE news;
TRUNCATE TABLE system_config;

-- ---------- 知识库业务表（三端文档 + 关系图谱 + 归集辅助） ----------
TRUNCATE TABLE kb_relation;
TRUNCATE TABLE kb_entry_model;
TRUNCATE TABLE kb_doc_vector;
TRUNCATE TABLE kb_admin_doc;
TRUNCATE TABLE kb_teacher_doc;
TRUNCATE TABLE kb_student_doc;
TRUNCATE TABLE kb_model_resource;
TRUNCATE TABLE kb_backup;
TRUNCATE TABLE kb_operation_log;

-- ---------- Agent 会话运行数据（非模型管理配置） ----------
TRUNCATE TABLE agent_message;
TRUNCATE TABLE agent_session;

-- ---------- 访问/操作日志 ----------
TRUNCATE TABLE api_access_log;
TRUNCATE TABLE dashboard_operation_log;

-- 注意：ai_model / ai_model_version / ai_model_metric / ai_model_kb_binding
--       / ai_invoke_log / ai_agent_log 六张 AI 模型管理相关表【保留不清】。

SET FOREIGN_KEY_CHECKS=1;

-- ============================================================
-- 1. 用户 / 教师 / 学员
-- ============================================================
INSERT INTO sys_user (user_id,username,password,role,real_name,phone,status) VALUES
(1,'admin','$2b$10$jjnp4KP/9fmHS9JPO8BHu.1ofkz7aj755T7YsfzL0YP2Qq7KS7Nd2','ADMIN','系统管理员','13800000001',1),
(2,'T001','$2b$10$jjnp4KP/9fmHS9JPO8BHu.1ofkz7aj755T7YsfzL0YP2Qq7KS7Nd2','TEACHER','张老师','13800000002',1),
(3,'T002','$2b$10$jjnp4KP/9fmHS9JPO8BHu.1ofkz7aj755T7YsfzL0YP2Qq7KS7Nd2','TEACHER','李老师','13800000003',1),
(4,'T003','$2b$10$jjnp4KP/9fmHS9JPO8BHu.1ofkz7aj755T7YsfzL0YP2Qq7KS7Nd2','TEACHER','王老师','13800000004',1),
(10,'20240101','$2b$10$jjnp4KP/9fmHS9JPO8BHu.1ofkz7aj755T7YsfzL0YP2Qq7KS7Nd2','STUDENT','小明','13800000005',1),
(11,'20240102','$2b$10$jjnp4KP/9fmHS9JPO8BHu.1ofkz7aj755T7YsfzL0YP2Qq7KS7Nd2','STUDENT','小红','13800000006',1),
(12,'20240103','$2b$10$jjnp4KP/9fmHS9JPO8BHu.1ofkz7aj755T7YsfzL0YP2Qq7KS7Nd2','STUDENT','小刚','13800000007',1),
(13,'20240104','$2b$10$jjnp4KP/9fmHS9JPO8BHu.1ofkz7aj755T7YsfzL0YP2Qq7KS7Nd2','STUDENT','小丽','13800000008',1),
(14,'20240105','$2b$10$jjnp4KP/9fmHS9JPO8BHu.1ofkz7aj755T7YsfzL0YP2Qq7KS7Nd2','STUDENT','小美','13800000009',1),
(15,'20240106','$2b$10$jjnp4KP/9fmHS9JPO8BHu.1ofkz7aj755T7YsfzL0YP2Qq7KS7Nd2','STUDENT','小强','13800000010',1),
(20,'visitor1','$2b$10$jjnp4KP/9fmHS9JPO8BHu.1ofkz7aj755T7YsfzL0YP2Qq7KS7Nd2','VISITOR','试听游客','13800000011',1);

INSERT INTO teacher (teacher_id,teacher_no,user_id,name,gender,birth_date,phone,email,address,emergency_contact,emergency_phone,major,education,school,title,employ_type,teach_years,hourly_rate,base_salary,intro,hire_date,status) VALUES
(1,'T001',2,'张老师','女','1988-03-12','13800000012','zhang@example.com','某城市','张父','13800000013','舞蹈','本科','某舞蹈学院','高级教师','全职',12,120,2000,'资深敦煌舞教师，主攻飞天舞与反弹琵琶经典舞姿教学，曾赴敦煌研究院研修。','2020-09-01',1),
(2,'T002',3,'李老师','女','1992-07-05','13800000014','li@example.com','某城市','李母','13800000015','舞蹈','硕士','某舞蹈学院','一级教师','全职',8,110,1800,'中国舞专业，擅长敦煌长绸舞与乐舞吟唱，教学风格活泼细致。','2022-03-01',1),
(3,'T003',4,'王老师','男','1990-11-20','13800000016','wang@example.com','某城市','王父','13800000017','美术','本科','某美术学院','中级教师','兼职',6,100,0,'美术教师，专注敦煌壁画临摹与儿童美育，注重线条与矿物色彩启蒙。','2023-06-01',1);

INSERT INTO student (student_id,student_no,user_id,name,gender,birth_date,parent_phone,email,enroll_date,status) VALUES
(1,'20240101',10,'小明','男','2014-05-10','13800000018','stu1@example.com','2024-09-01',1),
(2,'20240102',11,'小红','女','2014-08-22','13800000019','stu2@example.com','2024-09-01',1),
(3,'20240103',12,'小刚','男','2013-02-14','13800000020','stu3@example.com','2024-09-01',1),
(4,'20240104',13,'小丽','女','2015-11-03','13800000021','stu4@example.com','2024-09-01',1),
(5,'20240105',14,'小美','女','2014-01-19','13800000022','stu5@example.com','2024-09-01',1),
(6,'20240106',15,'小强','男','2013-09-30','13800000023','stu6@example.com','2024-09-01',1);

-- ============================================================
-- 2. 教室 / 分类 / 课程
-- ============================================================
INSERT INTO classroom (classroom_id,classroom_name,capacity,room_type,status) VALUES
(1,'敦煌舞教室1',15,'舞蹈教室',1),
(2,'敦煌舞教室2',15,'舞蹈教室',1),
(3,'长绸排练厅',12,'舞蹈教室',1),
(4,'壁画临摹教室',12,'美术教室',1),
(5,'乐舞教室',10,'声乐教室',1);

INSERT INTO course_category (category_id,category_name,description) VALUES
(1,'敦煌舞','以敦煌壁画乐舞为蓝本的特色舞蹈课程'),
(2,'舞蹈','中国舞、长绸舞等综合舞蹈类课程'),
(3,'美术','敦煌壁画临摹与创意美术类课程'),
(4,'乐舞','敦煌曲谱吟唱与乐舞律动类课程');

INSERT INTO course (course_id,course_no,course_name,category,age_group,level,total_hours,lesson_duration,original_price,price,intro,status,sort_order) VALUES
(1,'C001','飞天舞启蒙','敦煌舞','4-8岁','启蒙',36,45,1999,1499,'以敦煌壁画飞天形象为蓝本，学习基本手姿与三道弯体态。',1,1),
(2,'C002','反弹琵琶','敦煌舞','7-12岁','提高',48,45,2999,2599,'学习反弹琵琶经典舞姿，融合踏步、转身与手位组合。',1,2),
(3,'C003','敦煌长绸舞','舞蹈','6-12岁','提高',36,60,2399,2199,'执绸、抛绸、绕绸技法与气息身韵综合训练。',1,3),
(4,'C004','敦煌壁画临摹','美术','5-12岁','启蒙',32,60,2199,1799,'临摹莫高窟经典壁画线描与赋色，感受矿物颜料之美。',1,4),
(5,'C005','敦煌乐舞吟唱','乐舞','6-12岁','启蒙',32,45,1699,1399,'敦煌曲谱吟唱与乐舞律动，唱跳结合感受丝路乐音。',1,5),
(6,'C006','九色鹿故事舞','敦煌舞','4-8岁','启蒙',24,45,1699,1399,'以九色鹿本生故事为线索的儿童情境舞剧课程。',0,6);

INSERT INTO teacher_course (teacher_id,course_id) VALUES
(1,1),(1,2),(2,3),(2,5),(3,4);

INSERT INTO course_class (class_id,course_id,class_name,category_id,teacher_id,classroom_id,semester,start_date,end_date,total_hours,lesson_duration,tuition,max_students,status,age_group,level,description) VALUES
(1,1,'飞天舞启蒙一班',1,1,1,'2026秋季','2026-09-14','2026-12-31',36,45,1499,8,1,'4-8岁','启蒙','飞天舞启蒙阶段班级'),
(2,2,'反弹琵琶提高班',1,1,2,'2026秋季','2026-09-14','2026-12-31',48,45,2599,15,1,'7-12岁','提高','反弹琵琶提高班级'),
(3,3,'长绸舞一班',2,2,3,'2026秋季','2026-09-14','2026-12-31',36,60,2199,12,1,'6-12岁','提高','敦煌长绸舞班级'),
(4,4,'壁画临摹班',3,3,4,'2026秋季','2026-09-14','2026-12-31',32,60,1799,12,1,'5-12岁','启蒙','敦煌壁画临摹班级'),
(5,5,'乐舞吟唱班',4,2,5,'2026秋季','2026-09-14','2026-12-31',32,45,1399,10,1,'6-12岁','启蒙','敦煌乐舞吟唱班级');

-- ============================================================
-- 3. 排课（基准日 2026-10-20 周二；今日待上课 3 节：班1/班2/班4）
-- ============================================================
INSERT INTO course_schedule (schedule_id,class_id,teacher_id,classroom_id,course_date,start_time,end_time,hours,status,remark) VALUES
-- 班1 飞天舞启蒙一班（周二/四 08:00-08:45）
(1,1,1,1,'2026-10-06','08:00:00','08:45:00','0.75','已完成',NULL),
(2,1,1,1,'2026-10-08','08:00:00','08:45:00','0.75','已完成',NULL),
(3,1,1,1,'2026-10-13','08:00:00','08:45:00','0.75','已完成',NULL),
(4,1,1,1,'2026-10-15','08:00:00','08:45:00','0.75','已完成',NULL),
(5,1,1,1,'2026-10-20','08:00:00','08:45:00','0.75','待上课',NULL),
(6,1,1,1,'2026-10-22','08:00:00','08:45:00','0.75','待上课',NULL),
(7,1,1,1,'2026-10-27','08:00:00','08:45:00','0.75','待上课',NULL),
(8,1,1,1,'2026-10-29','08:00:00','08:45:00','0.75','待上课',NULL),
-- 班2 反弹琵琶提高班（周二/四 09:00-09:45）
(9,2,1,2,'2026-10-06','09:00:00','09:45:00','0.75','已完成',NULL),
(10,2,1,2,'2026-10-08','09:00:00','09:45:00','0.75','已完成',NULL),
(11,2,1,2,'2026-10-13','09:00:00','09:45:00','0.75','已完成',NULL),
(12,2,1,2,'2026-10-15','09:00:00','09:45:00','0.75','已完成',NULL),
(13,2,1,2,'2026-10-20','09:00:00','09:45:00','0.75','待上课',NULL),
(14,2,1,2,'2026-10-22','09:00:00','09:45:00','0.75','待上课',NULL),
(15,2,1,2,'2026-10-27','09:00:00','09:45:00','0.75','待上课',NULL),
(16,2,1,2,'2026-10-29','09:00:00','09:45:00','0.75','待上课',NULL),
-- 班3 长绸舞一班（周一/三/五 10:00-11:00）
(17,3,2,3,'2026-10-05','10:00:00','11:00:00','1.0','已完成',NULL),
(18,3,2,3,'2026-10-07','10:00:00','11:00:00','1.0','已完成',NULL),
(19,3,2,3,'2026-10-09','10:00:00','11:00:00','1.0','已完成',NULL),
(20,3,2,3,'2026-10-12','10:00:00','11:00:00','1.0','已完成',NULL),
(21,3,2,3,'2026-10-14','10:00:00','11:00:00','1.0','已完成',NULL),
(22,3,2,3,'2026-10-16','10:00:00','11:00:00','1.0','已完成',NULL),
(23,3,2,3,'2026-10-19','10:00:00','11:00:00','1.0','已完成',NULL),
(24,3,2,3,'2026-10-21','10:00:00','11:00:00','1.0','待上课',NULL),
(25,3,2,3,'2026-10-23','10:00:00','11:00:00','1.0','待上课',NULL),
(26,3,2,3,'2026-10-26','10:00:00','11:00:00','1.0','待上课',NULL),
(27,3,2,3,'2026-10-28','10:00:00','11:00:00','1.0','待上课',NULL),
(28,3,2,3,'2026-10-30','10:00:00','11:00:00','1.0','待上课',NULL),
-- 班4 壁画临摹班（周二/六 14:00-15:00）
(29,4,3,4,'2026-10-06','14:00:00','15:00:00','1.0','已完成',NULL),
(30,4,3,4,'2026-10-10','14:00:00','15:00:00','1.0','已完成',NULL),
(31,4,3,4,'2026-10-13','14:00:00','15:00:00','1.0','已完成',NULL),
(32,4,3,4,'2026-10-17','14:00:00','15:00:00','1.0','已完成',NULL),
(33,4,3,4,'2026-10-20','14:00:00','15:00:00','1.0','待上课',NULL),
(34,4,3,4,'2026-10-24','14:00:00','15:00:00','1.0','待上课',NULL),
(35,4,3,4,'2026-10-27','14:00:00','15:00:00','1.0','待上课',NULL),
(36,4,3,4,'2026-10-31','14:00:00','15:00:00','1.0','待上课',NULL),
-- 班5 乐舞吟唱班（周五 16:00-16:45）
(37,5,2,5,'2026-10-09','16:00:00','16:45:00','0.75','已完成',NULL),
(38,5,2,5,'2026-10-16','16:00:00','16:45:00','0.75','已完成',NULL),
(39,5,2,5,'2026-10-23','16:00:00','16:45:00','0.75','待上课',NULL),
(40,5,2,5,'2026-10-30','16:00:00','16:45:00','0.75','待上课',NULL);

-- ============================================================
-- 4. 报名 / 考勤
-- ============================================================
INSERT INTO enrollment (enrollment_id,student_id,class_id,enroll_date,tuition_amount,status) VALUES
(1,1,1,'2026-09-10',1499,'在读'),
(2,2,1,'2026-09-10',1499,'在读'),
(3,6,1,'2026-09-11',1499,'退学'),
(4,2,2,'2026-09-09',2599,'在读'),
(5,3,2,'2026-09-09',2599,'在读'),
(6,4,2,'2026-09-12',2599,'在读'),
(7,1,3,'2026-09-08',2199,'在读'),
(8,5,3,'2026-09-08',2199,'在读'),
(9,3,4,'2026-09-05',1799,'在读'),
(10,5,4,'2026-09-07',1799,'在读'),
(11,6,5,'2026-09-06',1399,'在读');

INSERT INTO attendance (attendance_id,schedule_id,student_id,status,record_time,remark) VALUES
-- 班1（学生1、2）
(1,1,1,'出勤','2026-10-06 08:40:00',NULL),
(2,1,2,'出勤','2026-10-06 08:40:00',NULL),
(3,2,1,'请假','2026-10-08 08:40:00',NULL),
(4,2,2,'迟到','2026-10-08 08:52:00',NULL),
(5,3,1,'迟到','2026-10-13 08:55:00',NULL),
(6,3,2,'出勤','2026-10-13 08:40:00',NULL),
(7,4,1,'出勤','2026-10-15 08:40:00',NULL),
(8,4,2,'出勤','2026-10-15 08:40:00',NULL),
-- 班2（学生2、3、4）
(9,9,2,'出勤','2026-10-06 09:40:00',NULL),
(10,9,3,'出勤','2026-10-06 09:40:00',NULL),
(11,9,4,'迟到','2026-10-06 09:50:00',NULL),
(12,10,2,'迟到','2026-10-08 09:52:00',NULL),
(13,10,3,'迟到','2026-10-08 09:55:00',NULL),
(14,10,4,'出勤','2026-10-08 09:40:00',NULL),
(15,11,2,'迟到','2026-10-13 09:50:00',NULL),
(16,11,3,'出勤','2026-10-13 09:40:00',NULL),
(17,11,4,'缺勤','2026-10-13 09:40:00',NULL),
(18,12,2,'出勤','2026-10-15 09:40:00',NULL),
(19,12,3,'出勤','2026-10-15 09:40:00',NULL),
(20,12,4,'出勤','2026-10-15 09:40:00',NULL),
-- 班3（学生1、5）
(21,17,1,'出勤','2026-10-05 10:55:00',NULL),
(22,17,5,'出勤','2026-10-05 10:55:00',NULL),
(23,18,1,'出勤','2026-10-07 10:55:00',NULL),
(24,18,5,'迟到','2026-10-07 11:05:00',NULL),
(25,19,1,'迟到','2026-10-09 11:03:00',NULL),
(26,19,5,'出勤','2026-10-09 10:55:00',NULL),
(27,20,1,'出勤','2026-10-12 10:55:00',NULL),
(28,20,5,'出勤','2026-10-12 10:55:00',NULL),
(29,21,1,'出勤','2026-10-14 10:55:00',NULL),
(30,21,5,'出勤','2026-10-14 10:55:00',NULL),
(31,22,1,'出勤','2026-10-16 10:55:00',NULL),
(32,22,5,'出勤','2026-10-16 10:55:00',NULL),
(33,23,1,'出勤','2026-10-19 10:55:00',NULL),
(34,23,5,'出勤','2026-10-19 10:55:00',NULL),
-- 班4（学生3、5）
(35,29,3,'出勤','2026-10-06 14:55:00',NULL),
(36,29,5,'出勤','2026-10-06 14:55:00',NULL),
(37,30,3,'出勤','2026-10-10 14:55:00',NULL),
(38,30,5,'请假','2026-10-10 14:55:00',NULL),
(39,31,3,'迟到','2026-10-13 15:04:00',NULL),
(40,31,5,'出勤','2026-10-13 14:55:00',NULL),
(41,32,3,'出勤','2026-10-17 14:55:00',NULL),
(42,32,5,'出勤','2026-10-17 14:55:00',NULL),
-- 班5（学生6）
(43,37,6,'出勤','2026-10-09 16:40:00',NULL),
(44,38,6,'出勤','2026-10-16 16:40:00',NULL);

-- 出勤统计与考勤明细对齐（迟到计入出勤，请假/缺勤计入缺席）
UPDATE enrollment SET attended_count=3, absent_count=1 WHERE student_id=1 AND class_id=1;
UPDATE enrollment SET attended_count=4, absent_count=0 WHERE student_id=2 AND class_id=1;
UPDATE enrollment SET attended_count=4, absent_count=0 WHERE student_id=2 AND class_id=2;
UPDATE enrollment SET attended_count=5, absent_count=0 WHERE student_id=3 AND class_id=2;
UPDATE enrollment SET attended_count=3, absent_count=1 WHERE student_id=4 AND class_id=2;
UPDATE enrollment SET attended_count=7, absent_count=0 WHERE student_id=1 AND class_id=3;
UPDATE enrollment SET attended_count=7, absent_count=0 WHERE student_id=5 AND class_id=3;
UPDATE enrollment SET attended_count=4, absent_count=0 WHERE student_id=3 AND class_id=4;
UPDATE enrollment SET attended_count=3, absent_count=1 WHERE student_id=5 AND class_id=4;
UPDATE enrollment SET attended_count=2, absent_count=0 WHERE student_id=6 AND class_id=5;

-- ============================================================
-- 5. 作业 / 提交（含 AI 评分样本，报告 JSON 与前端解析结构一致）
-- ============================================================
INSERT INTO homework (homework_id,class_id,schedule_id,title,content,create_time,deadline) VALUES
(1,1,NULL,'飞天舞启蒙-手姿三十六式练习','练习「风吹柳」「垂柳」手姿各20次，跟音乐完成手姿组合并录制视频','2026-10-17 18:00:00','2026-10-31 20:00:00'),
(2,2,NULL,'反弹琵琶-持琴踏步组合','对镜练习持琵琶手位与踏步转身，录制正面完整视频','2026-10-17 18:05:00','2026-10-30 20:00:00'),
(3,3,NULL,'长绸舞-云手绕绸组合','练习云手绕绸与碎步圆场，跟音乐录制完整组合','2026-10-18 09:00:00','2026-10-31 20:00:00'),
(4,4,NULL,'壁画临摹-第320窟飞天线描','完成第320窟飞天局部线描一幅，拍照上传','2026-10-18 15:00:00','2026-10-28 18:00:00');

INSERT INTO homework_submission (submission_id,homework_id,student_id,content,attachment_url,attachment_name,submit_time,score,teacher_comment,ai_score,ai_report,ai_status,ai_scored_at,status) VALUES
(1,1,1,'手姿组合视频已录制',NULL,NULL,'2026-10-18 19:00:00',91,'手姿舒展，三道弯体态标准',88.5,
 '{"scores":{"total":88.5,"dtwDistance":4.2,"frames":512,"teacherFrames":512,"studentFrames":510},"parts":{"head":90.0,"shoulder":89.5,"arm":87.0,"torso":91.0,"hip":86.5,"leg":88.0},"dims":{"rhythm":88.0,"accuracy":89.0,"expressiveness":86.5},"rhythm":{"available":true,"bpm":96,"hitRate":0.82,"avgOffsetMs":36,"score":85.2,"globalOffsetMs":40,"segmentOffsetsMs":[35,42,38]},"semantic":{"available":true,"analyzedAt":"2026-10-18 20:16:00","model":"doubao-seed-2-0-mini","teacherAnalysis":"动作组合完整，手姿到位，节奏踩点稳定。","studentAnalysis":"整体完成度高，个别转身略快，建议放慢半拍。","rhythmConsistency":"良好","suggestions":["转身前先稳住重心","手姿开度再大一些","跟节拍器分段练习"]}}',
 'SUCCESS','2026-10-18 20:15:30','已批改'),
(2,1,2,'已练习40分钟',NULL,NULL,'2026-10-18 20:10:00',NULL,NULL,NULL,NULL,NULL,NULL,'已提交'),
(3,2,2,'踏步组合练习完成',NULL,NULL,'2026-10-18 18:30:00',86,'琵琶手位稳定，转身重心再稳一些',83.2,
 '{"scores":{"total":83.2,"dtwDistance":5.6,"frames":480,"teacherFrames":512,"studentFrames":478},"parts":{"head":85.0,"shoulder":84.0,"arm":82.5,"torso":86.0,"hip":80.5,"leg":83.0},"dims":{"rhythm":84.0,"accuracy":83.5,"expressiveness":81.0},"rhythm":{"available":true,"bpm":92,"hitRate":0.74,"avgOffsetMs":48,"score":80.6,"globalOffsetMs":55,"segmentOffsetsMs":[50,58,44]},"semantic":{"available":false}}',
 'SUCCESS','2026-10-18 21:02:10','已批改'),
(4,2,3,'每天坚持练习',NULL,NULL,'2026-10-18 19:20:00',88,'踏步节奏准确，继续提速',90.1,
 '{"scores":{"total":90.1,"dtwDistance":3.8,"frames":505,"teacherFrames":512,"studentFrames":503},"parts":{"head":91.0,"shoulder":90.5,"arm":89.0,"torso":92.0,"hip":88.5,"leg":90.0},"dims":{"rhythm":90.5,"accuracy":90.0,"expressiveness":89.5},"rhythm":{"available":true,"bpm":96,"hitRate":0.86,"avgOffsetMs":32,"score":88.4,"globalOffsetMs":35,"segmentOffsetsMs":[30,38,33]},"semantic":{"available":false}}',
 'SUCCESS','2026-10-18 21:30:45','已批改'),
(5,2,4,'',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'未提交'),
(6,3,1,'绕绸组合视频',NULL,NULL,'2026-10-19 11:00:00',92,'绸花饱满，圆场流畅',91.4,
 '{"scores":{"total":91.4,"dtwDistance":3.5,"frames":600,"teacherFrames":600,"studentFrames":598},"parts":{"head":92.0,"shoulder":91.5,"arm":90.0,"torso":93.0,"hip":90.5,"leg":91.0},"dims":{"rhythm":92.0,"accuracy":91.0,"expressiveness":90.5},"rhythm":{"available":true,"bpm":88,"hitRate":0.88,"avgOffsetMs":30,"score":90.2,"globalOffsetMs":28,"segmentOffsetsMs":[25,32,30]},"semantic":{"available":false}}',
 'SUCCESS','2026-10-19 12:10:00','已批改'),
(7,3,5,'',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'未提交'),
(8,4,5,'线描作品已上传',NULL,NULL,'2026-10-19 17:30:00',NULL,NULL,NULL,NULL,NULL,NULL,'已提交');

-- ============================================================
-- 6. 考级 / 比赛 / 报名 / 缴费
-- ============================================================
INSERT INTO exam_project (project_id,name,activity_type,category,organizer,signup_start,signup_end,exam_date,exam_start,exam_end,exam_room,location,fee,requirement,award_desc,materials,status) VALUES
(1,'敦煌舞等级考试（1-3级）','考级','敦煌舞','中国敦煌舞专业考级委员会','2026-09-01','2026-10-10','2026-10-18','09:00','10:30','第一考场','艺术中心剧场',260,'敦煌舞一级动作组合一套，需上传身份证明','合格/良好/优秀','身份证、一寸照片','已结束'),
(2,'「飞天杯」敦煌舞艺术展演','比赛','敦煌舞','省舞蹈家协会','2026-09-15','2026-11-10','2026-12-19','14:00','15:30','大剧院A厅','敦煌大剧院',180,'3分钟以内敦煌舞独舞/群舞节目','一/二/三等奖','节目音乐、报名表','报名中'),
(3,'敦煌壁画临摹作品展','比赛','美术','市青少年活动中心','2026-09-25','2026-11-15','2026-11-28','10:00','11:00','美术展厅','青少年活动中心美术展厅',0,'提交敦煌壁画主题临摹或再创作一幅','入选/优秀作品','作品电子稿','报名中');

INSERT INTO exam_registration (reg_id,project_id,student_id,class_id,real_name,gender,birth_date,id_card,phone,material_url,teacher_id,teacher_comment,status,audit_remark,exam_time,exam_room,seat_no,result,award,certificate_no,create_time) VALUES
(1,1,1,1,'小明','男','2014-05-10',NULL,'13800000024',NULL,1,'手姿舒展，建议报考一级','已完成','审核通过','2026-10-18 09:00-10:30','第一考场','01','通过','良好','DHA-2026-001','2026-09-20 10:00:00'),
(2,1,2,1,'小红','女',NULL,NULL,'13800000025',NULL,1,'课堂表现稳定','已缴费','审核通过','2026-10-18 09:00-10:30','第一考场','02',NULL,NULL,NULL,'2026-09-21 10:00:00'),
(3,2,5,3,'小美','女','2014-01-19',NULL,'13800000026',NULL,2,'长绸舞表现力强','已确认','审核通过',NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-08 10:00:00'),
(4,2,3,NULL,'小刚','男','2013-02-14',NULL,'13800000027',NULL,NULL,NULL,'待审核',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-12 10:00:00'),
(5,3,1,4,'小明','男','2014-05-10',NULL,'13800000028',NULL,NULL,NULL,'已驳回','材料不全，请补充作品电子稿',NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-10 10:00:00'),
(6,3,5,4,'小美','女','2014-01-19',NULL,'13800000029',NULL,NULL,NULL,'待审核',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-13 10:00:00');

INSERT INTO payment (payment_id,student_id,class_id,enrollment_id,reg_id,pay_date,amount,pay_type,pay_method,operator_id,remark,status,refund_time,refund_operator_id) VALUES
(1,1,1,1,NULL,'2026-09-10 10:30:00',1499,'报名','微信',1,'飞天舞启蒙一班报名费','已支付',NULL,NULL),
(2,2,1,2,NULL,'2026-09-10 11:00:00',1499,'报名','支付宝',1,'飞天舞启蒙一班报名费','已支付',NULL,NULL),
(3,2,2,4,NULL,'2026-09-09 09:30:00',2599,'报名','微信',1,'反弹琵琶提高班报名费','已支付',NULL,NULL),
(4,3,2,5,NULL,'2026-09-09 10:00:00',2599,'报名','现金',1,'反弹琵琶提高班报名费','已支付',NULL,NULL),
(5,4,2,6,NULL,'2026-09-12 14:00:00',2599,'报名','刷卡',1,'反弹琵琶提高班报名费','已支付',NULL,NULL),
(6,1,3,7,NULL,'2026-09-08 15:00:00',2199,'报名','微信',1,'长绸舞一班报名费','已支付',NULL,NULL),
(7,5,3,8,NULL,'2026-09-08 15:30:00',2199,'报名','支付宝',1,'长绸舞一班报名费','已支付',NULL,NULL),
(8,5,4,10,NULL,'2026-09-07 16:00:00',1799,'报名','微信',1,'壁画临摹班报名费','已支付',NULL,NULL),
(9,6,5,11,NULL,'2026-09-06 10:00:00',1399,'报名','现金',1,'乐舞吟唱班报名费','已支付',NULL,NULL),
(10,2,2,4,NULL,'2026-10-15 10:00:00',200,'续费','微信',1,'反弹琵琶提高班课时续费','已支付',NULL,NULL),
(11,1,NULL,NULL,1,'2026-09-20 09:00:00',260,'考级','微信',1,'敦煌舞等级考试报名费','已支付',NULL,NULL),
(12,2,NULL,NULL,2,'2026-09-21 09:00:00',260,'考级','支付宝',1,'敦煌舞等级考试报名费','已支付',NULL,NULL),
(13,5,3,8,NULL,'2026-09-11 10:00:00',2199,'报名','微信',1,'重复缴费，已退款','已退款','2026-09-13 10:00:00',1),
(14,5,3,8,NULL,'2026-09-13 10:00:00',-2199,'退款','微信',1,'重复缴费退款冲账','已退款','2026-09-13 10:00:00',1);

UPDATE enrollment e SET total_paid=(SELECT COALESCE(SUM(p.amount),0) FROM payment p WHERE p.enrollment_id=e.enrollment_id AND p.status='已支付');

-- ============================================================
-- 7. 试听 / 预约 / 反馈 / 申请 / 点评 / 调课 / 消息
-- ============================================================
INSERT INTO visitor_booking (booking_id,name,phone,class_id,teacher_id,preferred_time,remark,status,create_time) VALUES
(1,'试听家长张','13800000030',1,1,'2026-10-24 10:00:00','想了解敦煌舞启蒙课','待确认','2026-10-17 09:00:00'),
(2,'试听家长李','13800000031',3,2,'2026-10-25 14:30:00','孩子喜欢敦煌飞天舞，零基础试听','已确认','2026-10-16 09:00:00'),
(3,'试听家长王','13800000032',4,3,'2026-10-22 15:00:00','喜欢壁画临摹，想试听美术课','已取消','2026-10-15 09:00:00');

INSERT INTO appointment (appointment_id,student_id,teacher_id,class_id,appointment_type,appointment_time,status,remark) VALUES
(1,1,1,1,'一对一辅导','2026-10-22 16:00:00','待确认','课后答疑，请教反弹琵琶手位问题'),
(2,5,2,3,'补课','2026-10-21 15:00:00','已接受','补上一节请假的长绸舞课');

INSERT INTO feedback (feedback_id,student_id,content,type,create_time,status,reply_content,reply_time,reply_by) VALUES
(1,1,'希望增加周末敦煌舞练功时段','建议','2026-10-15 12:00:00','已回复','已反馈给教务，将统筹安排练功房时段','2026-10-16 09:00:00','系统管理员'),
(2,3,'长绸排练厅把杆有点松动，麻烦检修','报修','2026-10-17 12:00:00','待处理',NULL,NULL,NULL),
(3,5,'想咨询临摹用的宣纸和颜料是否需要自备','咨询','2026-10-18 12:00:00','待处理',NULL,NULL,NULL);

INSERT INTO student_request (request_id,student_id,teacher_id,class_id,request_type,request_date,reason,status,reply_remark,create_time,handle_time) VALUES
(1,1,1,1,'请假','2026-10-08','家中有事，申请请假一次','已批准','同意，注意回看课堂录像','2026-10-07 20:00:00','2026-10-07 21:00:00'),
(2,4,1,2,'请假','2026-10-22','当天有其他安排，申请请假','待处理',NULL,'2026-10-19 20:00:00',NULL),
(3,6,2,5,'休学','2026-11-01','身体原因申请休学一学期','待处理',NULL,'2026-10-19 21:00:00',NULL);

-- 请假考勤与已批准请假申请对齐
UPDATE attendance SET request_id=1 WHERE attendance_id=3;

INSERT INTO student_comment (comment_id,student_id,class_id,teacher_id,content,create_time) VALUES
(1,2,1,1,'课堂专注，手姿三道弯体态进步明显，继续保持。','2026-10-18 18:00:00'),
(2,3,1,2,'节奏感好，注意绕绸时手腕的灵活性练习。','2026-10-18 18:05:00');

INSERT INTO reschedule_request (request_id,schedule_id,target_schedule_id,new_date,new_start_time,new_end_time,reason,status,handle_remark,apply_time,handler_id,handle_time) VALUES
(1,5,NULL,'2026-10-23','18:00:00','18:45:00','当天临时有活动，申请调到晚间','待审核',NULL,NULL,NULL,'2026-10-19 10:00:00'),
(2,13,NULL,'2026-10-21','09:00:00','09:45:00','因教室调整顺延一天上课','已通过','同意调课','2026-10-18 15:00:00',1,'2026-10-18 16:00:00'),
(3,24,NULL,'2026-11-02','10:00:00','11:00:00','申请顺延一周','已拒绝','当周课程进度紧张，不同意顺延',NULL,1,'2026-10-17 16:00:00');

INSERT INTO message (message_id,receiver_user_id,msg_type,title,content,biz_type,biz_id,is_read,create_time) VALUES
(1,10,'作业','作业已批改','飞天舞启蒙-手姿练习已批改：91分（AI评分88.5）','homework_submission',1,1,'2026-10-18 19:30:00'),
(2,10,'考级','考级/比赛成绩已发布','敦煌舞等级考试（1-3级）结果：通过（良好）','exam_registration',1,1,'2026-10-19 10:00:00'),
(3,11,'考级','考级/比赛报名已通过','敦煌舞等级考试报名审核通过，请及时缴费','exam_registration',2,0,'2026-09-21 09:00:00'),
(4,11,'作业','作业已批改','反弹琵琶-持琴踏步组合已批改：86分（AI评分83.2）','homework_submission',3,1,'2026-10-18 19:00:00'),
(5,2,'申请','有新的请假申请','小丽提交了一条请假申请，请处理','student_request',2,0,'2026-10-19 20:05:00'),
(6,14,'作业','作业已批改','长绸舞-云手绕绸组合已批改：92分（AI评分91.4）','homework_submission',6,1,'2026-10-19 12:30:00'),
(7,10,'预约','您的预约待确认','一对一辅导预约等待老师确认','appointment',1,0,'2026-10-19 18:00:00');

-- ============================================================
-- 8. 门户新闻 / 系统配置
-- ============================================================
INSERT INTO news (news_id,title,content,publisher,publish_time,view_count) VALUES
(1,'「飞天杯」敦煌舞艺术展演报名开启','2026「飞天杯」敦煌舞艺术展演现已开启报名，设独舞、群舞两个组别，优秀节目将推荐参加省级展演。欢迎学员到前台或联系教务老师报名。','系统管理员','2026-10-15 09:00:00',128),
(2,'敦煌舞等级考试成绩发布','本期敦煌舞等级考试（1-3级）已于10月18日结束，成绩与证书编号已可在学员端查询，恭喜通过考试的学员们。','系统管理员','2026-10-19 09:00:00',56);

INSERT INTO system_config (config_key,config_value) VALUES
('org_name','舞韵·敦煌舞蹈艺术培训中心'),
('org_phone','0543-8888888'),
('org_address','某城市（柳州市城中区东环大道268号）'),
('org_intro','舞韵·敦煌舞蹈艺术培训中心以敦煌舞为特色，开设飞天舞、反弹琵琶、长绸舞、壁画临摹、乐舞吟唱等课程，融合AI姿态评分技术，让传统文化浸润童年。'),
('logo_url',''),
('banner_images',''),
('org_wechat','wy-dunhuang2026');

-- ============================================================
-- 9. 知识库：三端敦煌舞知识文档
-- ============================================================
INSERT INTO kb_teacher_doc (title,category,content,summary,tags,source_type,owner_id,owner_name,status,view_count) VALUES
('敦煌舞教学法总纲','教学资料','敦煌舞教学遵循"形—神—劲—律"四层递进：先立形（手姿、体态三道弯），再传神（眼神与表情），后练劲（发力与控制），终成律（节奏与韵律）。低龄段以情境模仿为主，提高段加入呼吸与身韵训练。','敦煌舞教学四层递进总纲，含分段教学策略','敦煌舞;教学法;三道弯','MANUAL',2,'张老师',1,36),
('敦煌舞基本手姿三十六式图解','教学资料','手姿是敦煌舞的灵魂：合掌式、风吹柳、垂柳、提腕指、弯三翘等三十六式各有名称与发力要领。练习时以腕为轴、以指领势，配合 Fixed 眼随手走的规则，每天两组、每组20次。','三十六式基本手姿名称与练习要领','手姿;基本功;图解','MANUAL',2,'张老师',1,58),
('反弹琵琶舞姿训练要点','教学资料','反弹琵琶是敦煌舞标志性舞姿：持琴手位须"高不过眉、低不过腰"，踏步转身时以主力腿为轴心、动力腿吸抬控稳。常见问题是转身重心前倾，建议对镜分慢速练习。','反弹琵琶手位、踏步与转身训练要点','反弹琵琶;舞姿;踏步','MANUAL',2,'张老师',1,42),
('敦煌长绸舞技法与气息运用','教学资料','长绸舞讲究"绸随身走、气推绸行"：执绸以虎口持柄，抛绸借助小臂弹挑，绕绸以手腕带动。呼吸下沉则绸落、上提则绸起，配合碎步圆场形成连绵不断的绸花。','长绸舞执绸、抛收与气息配合技法','长绸舞;技法;气息','MANUAL',3,'李老师',1,31),
('AI 姿态评分标准说明（v2.6）','评分标准','AI 评分采用6身体部位（头/肩/臂/躯干/髋/腿）×3维度（节奏/准确/表现力）双层加权结构，DTW 时序对齐后按关节角度差异计分，节奏维度参考音乐节拍命中率。总分与教师评分误差需控制在3分以内。','AI评分双层结构与校准要求说明','AI评分;DTW;评分标准','MANUAL',2,'张老师',1,27),
('敦煌舞考级教材 1-3 级动作清单','考级大纲','一级：基本手姿八式+行礼组合；二级：三道弯体态组合+反弹琵琶静态造型；三级：飞天手姿组合+踏步转身连接。各级均需配乐完整展示，考官按形、神、劲、律四项打分。','敦煌舞考级1-3级动作与评分构成','考级;动作清单;敦煌舞','MANUAL',2,'张老师',1,19),
('低龄学员敦煌舞启蒙教学方案','教学资料','4-8岁启蒙段以故事化教学为主：用九色鹿、月牙泉等情境串联手姿与步伐练习，单次专注训练不超过15分钟，穿插游戏保持兴趣。每学期以小型展示代替考试。','低龄段情境化启蒙教学方案','启蒙;幼儿;情境教学','MANUAL',3,'李老师',1,22),
('敦煌壁画伎乐天姿态采撷','参考资料','莫高窟112窟反弹琵琶、320窟双飞天、257窟九色鹿本生等均为经典教学素材。取姿时注意壁画是静态瞬间，需结合对称补全还原动作过程，避免生硬模仿平面造型。','经典窟壁画姿态与教学取材要点','壁画;伎乐天;素材','MANUAL',2,'张老师',1,25),
('敦煌舞课堂常见伤病预防','安全规范','下腰、劈叉等软开度练习必须充分热身后进行；长绸舞注意周身空间避免甩绸伤人；反弹琵琶转身练习先徒手后持道具。课堂应配备急救包并掌握扭拉伤RICE处理流程。','课堂软开度与道具训练安全要点','安全;伤病预防;课堂管理','MANUAL',3,'李老师',1,14),
('DTW 对齐与节奏评分解读指南','评分标准','DTW（动态时间规整）用于将学员动作序列与教师标准对齐，解决快慢差异；节奏分由节拍命中率（±150ms容差）、平均偏差与速度一致性构成。解读报告时先看DTW距离，再分区看部位分。','DTW对齐原理与节奏分构成解读','DTW;节奏;报告解读','MANUAL',2,'张老师',1,33);

INSERT INTO kb_student_doc (title,category,content,summary,tags,source_type,owner_id,owner_name,status,view_count) VALUES
('飞天手姿入门：三道弯体态要领','学习资料','三道弯体态是敦煌舞的入门功课：头、肋、胯向三个方向错位形成S形曲线。照镜练习时想象自己是壁画上的飞天，手姿先摆静态造型，站稳后再加入呼吸。','三道弯体态与飞天手姿入门要领','手姿;体态;入门','MANUAL',10,'小明',1,66),
('反弹琵琶基本姿势练习口诀','学习资料','口诀："琵琶举过头，眼看指尖走，踏步稳如钟，转身腰上扭"。每天对镜练习持琴手位5分钟，再练踏步转身10次，速度由慢到快。','反弹琵琶练习口诀与每日练习量','反弹琵琶;口诀;每日练习','MANUAL',11,'小红',1,51),
('长绸舞入门：执绸与抛收技巧','学习资料','执绸时虎口握柄、手心留空；抛绸用手臂的弹力而不是蛮力，收绸要顺势接住绸头。先原地练抛收，熟练后再加碎步。','执绸手型与抛绸收绸技巧','长绸舞;执绸;抛收','MANUAL',14,'小美',1,39),
('敦煌舞呼吸与身韵基础','学习资料','敦煌舞的"韵"来自呼吸：起势吸气提沉，落势呼气放松。练习时把手放在小腹感受气息，配合慢音乐做提沉组合，呼吸顺了动作自然连贯。','呼吸提沉与身韵基础练习','呼吸;身韵;基础','MANUAL',10,'小明',1,47),
('九色鹿故事舞剧情与角色解析','学习资料','九色鹿故事出自莫高窟257窟壁画：善良的九色鹿救起落水之人，却被出卖。舞剧中要演出九色鹿的高贵与从容，落水者与国王的性格也要用肢体区分。','九色鹿剧情脉络与角色性格表现','九色鹿;舞剧;角色','MANUAL',15,'小强',1,28),
('课后拉伸与软开度自练指南','学习资料','课后拉伸遵循"静态保持30秒、不弹震"原则：压腿、下腰、开胯各两组。软开度进步慢不要急，每周进步一点即可，感觉刺痛要立即停止并告诉老师和家长。','课后安全拉伸与软开度自练方法','拉伸;软开度;安全','MANUAL',12,'小刚',1,35),
('AI 评分报告怎么看（学员版）','学习资料','AI评分报告里：总分是整体水平，六个部位分告诉你哪里最好、哪里要练；节奏曲线像心电图，红线是音乐节拍，橙线是你的动作节拍，两条线靠得越近节奏越好。','AI评分报告各图表含义的学员版解读','AI评分;报告;解读','MANUAL',10,'小明',1,44),
('敦煌舞服与妆容小百科','学习资料','飞天演出服常见飘带与披帛，穿脱时注意不要踩踏；舞台妆以花钿与斜红为特色，卸妆要用温和产品。平时训练穿练功服和软底鞋即可。','舞服穿脱与舞台妆容常识','舞服;妆容;演出','MANUAL',13,'小丽',1,21),
('每周家庭练习计划（初级）','学习资料','周一/三/五：手姿+体态各15分钟；周二/四：跟音乐完整组合一遍并录像；周末：回看自己视频与老师示范对比，把最不满意的一个动作发给老师。','每周练习安排与自查方法','练习计划;家庭作业;自查','MANUAL',11,'小红',1,53),
('考级小贴士：敦煌舞 1 级通关要点','学习资料','一级考组合+行礼：进场先鞠躬报曲目，音乐起再动；手姿名称要说得出来；结束谢礼不要抢拍。着装整洁、头发盘起，提前10分钟到场候考。','考级流程礼仪与一级考核要点','考级;礼仪;通关','MANUAL',15,'小强',1,26);

INSERT INTO kb_admin_doc (title,category,content,summary,tags,source_type,owner_id,owner_name,status,view_count) VALUES
('舞韵敦煌教学中心运营手册','规章制度','中心以敦煌舞为核心特色，课程体系分为启蒙（4-8岁）、提高（7-12岁）两段；教师实行课时+续费双轨考核，学员档案一人一档，评语与考勤需48小时内归档。','中心定位、课程体系与运营考核总则','运营;规章制度;总则','MANUAL',1,'系统管理员',1,18),
('教室使用与排课管理规范','规章制度','敦煌舞教室须提前5分钟开窗通风，把杆与镜面课前检查；临摹教室颜料统一保管，课后回收；调课须提前24小时通过系统提交，教室冲突由教务统一仲裁。','教室使用流程与排课冲突处理规范','排课;教室;管理规范','MANUAL',1,'系统管理员',1,12),
('招生咨询标准话术（敦煌舞方向）','运营资料','家长咨询以"传统文化+AI科学训练"双卖点切入：先介绍敦煌文化背景，再演示AI评分报告样例，最后邀约免费试听。禁忌：不得承诺考级通过率，不得贬低同行。','招生话术框架与合规红线','招生;话术;合规','MANUAL',1,'系统管理员',1,9),
('学员安全管理与应急预案','安全规范','软开度训练必须有保护陪同；家长接送实行刷卡签到签退；突发伤害按"停止训练—现场处置—通知家长—记录归档"四步处理，重大事件2小时内上报主管。','安全管理制度与应急处置流程','安全;应急;接送','MANUAL',1,'系统管理员',1,15),
('AI 评分服务运维手册','技术文档','AI评分服务部署于内网GPU服务器，Python评分流水线经Java后端代理调用；每日检查评分队列积压，失败任务自动重试一次后转人工。模型配置与调用日志见AI模型管理页。','AI评分服务架构与日常运维要点','AI评分;运维;故障处理','MANUAL',1,'系统管理员',1,11),
('知识库数据归集与备份规范','规章制度','业务数据（课程/作业/考勤）自动归集入知识库，归集失败不阻断主流程；知识库每周日全量备份，备份保留最近8份；删除知识文档须二次确认并留痕。','知识库归集机制与备份策略','知识库;备份;归集','MANUAL',1,'系统管理员',1,8),
('考级报名与证书发放流程','考级管理','考级报名截止后3日内完成资格初审；准考证考前一周发放；成绩发布后证书30日内到馆，前台核对签收后通知学员领取并拍照留档。','考级报名审核、准考证与证书流程','考级;证书;流程','MANUAL',1,'系统管理员',1,7),
('家长沟通与续费策略','运营资料','每学期两次成长反馈（期中/期末），结合AI评分报告给家长可视化进步曲线；续费窗口为结课前3周，老学员续费享95折，转介绍双方各赠2课时。','家长沟通节奏与续费转化策略','家长沟通;续费;转化','MANUAL',1,'系统管理员',1,10);

-- ============================================================
-- 10. 知识库条目-模型绑定（唯一键 kb_scope+kb_id：每条目绑定一个最优模型）
--     动作/评分类条目 → VISION 豆包（model_id=3），其余 → LLM（model_id=1）
-- ============================================================
INSERT INTO kb_entry_model (kb_scope,kb_id,model_id,match_score,match_rule,match_tags) VALUES
-- 教师端：手姿/反弹琵琶/长绸/评分/壁画/DTW 绑 VISION，其余绑 LLM
('teacher',1,1,0.9500,'关键词匹配','敦煌舞;教学法;三道弯'),
('teacher',2,3,0.9000,'语义相关','手姿;基本功'),
('teacher',3,3,0.9000,'语义相关','反弹琵琶;舞姿'),
('teacher',4,3,0.8800,'语义相关','长绸舞;技法'),
('teacher',5,3,0.9200,'语义相关','AI评分;DTW'),
('teacher',6,1,0.9500,'关键词匹配','考级;动作清单;敦煌舞'),
('teacher',7,1,0.9500,'关键词匹配','启蒙;幼儿;情境教学'),
('teacher',8,3,0.8500,'语义相关','壁画;伎乐天'),
('teacher',9,1,0.9500,'关键词匹配','安全;伤病预防;课堂管理'),
('teacher',10,3,0.9200,'语义相关','DTW;节奏'),
-- 学生端：手姿/口诀/执绸/AI报告 绑 VISION，其余绑 LLM
('student',1,3,0.8800,'语义相关','手姿;体态'),
('student',2,3,0.9000,'语义相关','反弹琵琶;口诀'),
('student',3,3,0.8800,'语义相关','长绸舞;执绸'),
('student',4,1,0.9500,'关键词匹配','呼吸;身韵;基础'),
('student',5,1,0.9500,'关键词匹配','九色鹿;舞剧;角色'),
('student',6,1,0.9500,'关键词匹配','拉伸;软开度;安全'),
('student',7,3,0.9200,'语义相关','AI评分;报告'),
('student',8,1,0.9500,'关键词匹配','舞服;妆容;演出'),
('student',9,1,0.9500,'关键词匹配','练习计划;家庭作业;自查'),
('student',10,1,0.9500,'关键词匹配','考级;礼仪;通关'),
-- 管理端全部绑 LLM
('admin',1,1,0.9500,'关键词匹配','运营;规章制度;总则'),
('admin',2,1,0.9500,'关键词匹配','排课;教室;管理规范'),
('admin',3,1,0.9500,'关键词匹配','招生;话术;合规'),
('admin',4,1,0.9500,'关键词匹配','安全;应急;接送'),
('admin',5,1,0.9200,'关键词匹配','AI评分;运维;故障处理'),
('admin',6,1,0.9500,'关键词匹配','知识库;备份;归集'),
('admin',7,1,0.9500,'关键词匹配','考级;证书;流程'),
('admin',8,1,0.9500,'关键词匹配','家长沟通;续费;转化');

-- ============================================================
-- 11. 知识库图谱关系（与业务数据对齐，格式同自动归集）
-- ============================================================
INSERT INTO kb_relation (src_ref,dst_ref,rel_type,rel_dimension,rel_source,src_label,dst_label,weight) VALUES
-- 课程-教师（授课）
('course:1','teacher:1','OF_TEACHER','TEACH','BUSINESS_FK','飞天舞启蒙','张老师',1.00),
('course:2','teacher:1','OF_TEACHER','TEACH','BUSINESS_FK','反弹琵琶','张老师',1.00),
('course:3','teacher:2','OF_TEACHER','TEACH','BUSINESS_FK','敦煌长绸舞','李老师',1.00),
('course:4','teacher:3','OF_TEACHER','TEACH','BUSINESS_FK','敦煌壁画临摹','王老师',1.00),
('course:5','teacher:2','OF_TEACHER','TEACH','BUSINESS_FK','敦煌乐舞吟唱','李老师',1.00),
-- 课程-分类（归属）
('course:1','course_category:1','BELONGS_TO','ORG','BUSINESS_FK','飞天舞启蒙','敦煌舞',1.00),
('course:2','course_category:1','BELONGS_TO','ORG','BUSINESS_FK','反弹琵琶','敦煌舞',1.00),
('course:6','course_category:1','BELONGS_TO','ORG','BUSINESS_FK','九色鹿故事舞','敦煌舞',1.00),
('course:3','course_category:2','BELONGS_TO','ORG','BUSINESS_FK','敦煌长绸舞','舞蹈',1.00),
('course:4','course_category:3','BELONGS_TO','ORG','BUSINESS_FK','敦煌壁画临摹','美术',1.00),
('course:5','course_category:4','BELONGS_TO','ORG','BUSINESS_FK','敦煌乐舞吟唱','乐舞',1.00),
-- 班级-课程（开设）/ 班级-教室（场地）
('course_class:1','course:1','OF_COURSE','TEACH','BUSINESS_FK','飞天舞启蒙一班','飞天舞启蒙',1.00),
('course_class:2','course:2','OF_COURSE','TEACH','BUSINESS_FK','反弹琵琶提高班','反弹琵琶',1.00),
('course_class:3','course:3','OF_COURSE','TEACH','BUSINESS_FK','长绸舞一班','敦煌长绸舞',1.00),
('course_class:4','course:4','OF_COURSE','TEACH','BUSINESS_FK','壁画临摹班','敦煌壁画临摹',1.00),
('course_class:5','course:5','OF_COURSE','TEACH','BUSINESS_FK','乐舞吟唱班','敦煌乐舞吟唱',1.00),
('course_class:1','classroom:1','IN_ROOM','ASSET','BUSINESS_FK','飞天舞启蒙一班','敦煌舞教室1',1.00),
('course_class:2','classroom:2','IN_ROOM','ASSET','BUSINESS_FK','反弹琵琶提高班','敦煌舞教室2',1.00),
('course_class:3','classroom:3','IN_ROOM','ASSET','BUSINESS_FK','长绸舞一班','长绸排练厅',1.00),
('course_class:4','classroom:4','IN_ROOM','ASSET','BUSINESS_FK','壁画临摹班','壁画临摹教室',1.00),
('course_class:5','classroom:5','IN_ROOM','ASSET','BUSINESS_FK','乐舞吟唱班','乐舞教室',1.00),
-- 报名（班级-学员）
('enrollment:1','student:1','OF_STUDENT','ORG','BUSINESS_FK','飞天舞启蒙一班报名','小明',1.00),
('enrollment:2','student:2','OF_STUDENT','ORG','BUSINESS_FK','飞天舞启蒙一班报名','小红',1.00),
('enrollment:4','student:2','OF_STUDENT','ORG','BUSINESS_FK','反弹琵琶提高班报名','小红',1.00),
('enrollment:5','student:3','OF_STUDENT','ORG','BUSINESS_FK','反弹琵琶提高班报名','小刚',1.00),
('enrollment:6','student:4','OF_STUDENT','ORG','BUSINESS_FK','反弹琵琶提高班报名','小丽',1.00),
('enrollment:7','student:1','OF_STUDENT','ORG','BUSINESS_FK','长绸舞一班报名','小明',1.00),
('enrollment:8','student:5','OF_STUDENT','ORG','BUSINESS_FK','长绸舞一班报名','小美',1.00),
('enrollment:9','student:3','OF_STUDENT','ORG','BUSINESS_FK','壁画临摹班报名','小刚',1.00),
('enrollment:10','student:5','OF_STUDENT','ORG','BUSINESS_FK','壁画临摹班报名','小美',1.00),
('enrollment:11','student:6','OF_STUDENT','ORG','BUSINESS_FK','乐舞吟唱班报名','小强',1.00),
-- 考级报名（项目-学员）
('exam_registration:1','exam_project:1','OF_PROJECT','TEACH','BUSINESS_FK','小明考级报名','敦煌舞等级考试（1-3级）',1.00),
('exam_registration:2','exam_project:1','OF_PROJECT','TEACH','BUSINESS_FK','小红考级报名','敦煌舞等级考试（1-3级）',1.00),
('exam_registration:3','exam_project:2','OF_PROJECT','TEACH','BUSINESS_FK','小美展演报名','「飞天杯」敦煌舞艺术展演',1.00),
('exam_registration:6','exam_project:3','OF_PROJECT','TEACH','BUSINESS_FK','小美作品展报名','敦煌壁画临摹作品展',1.00);

-- ============================================================
-- 12. 完整性校验
-- ============================================================
SELECT 'sys_user' t, COUNT(*) c FROM sys_user
UNION ALL SELECT 'teacher', COUNT(*) FROM teacher
UNION ALL SELECT 'student', COUNT(*) FROM student
UNION ALL SELECT 'classroom', COUNT(*) FROM classroom
UNION ALL SELECT 'course_category', COUNT(*) FROM course_category
UNION ALL SELECT 'course', COUNT(*) FROM course
UNION ALL SELECT 'course_class', COUNT(*) FROM course_class
UNION ALL SELECT 'course_schedule', COUNT(*) FROM course_schedule
UNION ALL SELECT 'enrollment', COUNT(*) FROM enrollment
UNION ALL SELECT 'attendance', COUNT(*) FROM attendance
UNION ALL SELECT 'homework', COUNT(*) FROM homework
UNION ALL SELECT 'homework_submission', COUNT(*) FROM homework_submission
UNION ALL SELECT 'ai_submission(已AI评分)', COUNT(*) FROM homework_submission WHERE ai_status='SUCCESS'
UNION ALL SELECT 'exam_project', COUNT(*) FROM exam_project
UNION ALL SELECT 'exam_registration', COUNT(*) FROM exam_registration
UNION ALL SELECT 'payment', COUNT(*) FROM payment
UNION ALL SELECT 'kb_teacher_doc', COUNT(*) FROM kb_teacher_doc
UNION ALL SELECT 'kb_student_doc', COUNT(*) FROM kb_student_doc
UNION ALL SELECT 'kb_admin_doc', COUNT(*) FROM kb_admin_doc
UNION ALL SELECT 'kb_entry_model', COUNT(*) FROM kb_entry_model
UNION ALL SELECT 'kb_relation', COUNT(*) FROM kb_relation
UNION ALL SELECT 'ai_model(保留)', COUNT(*) FROM ai_model
UNION ALL SELECT 'ai_invoke_log(保留)', COUNT(*) FROM ai_invoke_log;
