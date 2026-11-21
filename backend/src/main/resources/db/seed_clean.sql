-- 干净演示数据（基准日 2026-08-29，账号密码均为 123456）
SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS=0;

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

SET FOREIGN_KEY_CHECKS=1;

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
(1,'T001',2,'张老师','男','1988-03-12','13800000012','zhang@example.com','某城市','张父','13800000013','钢琴','本科','某音乐学院','高级教师','全职',12,120,2000,'资深钢琴教师，擅长少儿启蒙教学。','2020-09-01',1),
(2,'T002',3,'李老师','女','1992-07-05','13800000014','li@example.com','某城市','李母','13800000015','舞蹈','硕士','某舞蹈学院','一级教师','全职',8,110,1800,'中国舞专业，教学风格活泼细致。','2022-03-01',1),
(3,'T003',4,'王老师','男','1990-11-20','13800000016','wang@example.com','某城市','王父','13800000017','美术','本科','某美术学院','中级教师','兼职',6,100,0,'创意美术教师，注重孩子想象力培养。','2023-06-01',1);


INSERT INTO student (student_id,student_no,user_id,name,gender,birth_date,parent_phone,email,enroll_date,status) VALUES
(1,'20240101',10,'小明','男','2014-05-10','13800000018','stu1@example.com','2024-09-01',1),
(2,'20240102',11,'小红','女','2014-08-22','13800000019','stu2@example.com','2024-09-01',1),
(3,'20240103',12,'小刚','男','2013-02-14','13800000020','stu3@example.com','2024-09-01',1),
(4,'20240104',13,'小丽','女','2015-11-03','13800000021','stu4@example.com','2024-09-01',1),
(5,'20240105',14,'小美','女','2014-01-19','13800000022','stu5@example.com','2024-09-01',1),
(6,'20240106',15,'小强','男','2013-09-30','13800000023','stu6@example.com','2024-09-01',1);

INSERT INTO classroom (classroom_id,classroom_name,capacity,room_type,status) VALUES
(1,'钢琴教室1',8,'钢琴教室',1),
(2,'钢琴教室2',8,'钢琴教室',1),
(3,'舞蹈教室',15,'舞蹈教室',1),
(4,'美术教室',12,'美术教室',1),
(5,'声乐教室',10,'声乐教室',1);

INSERT INTO course_category (category_id,category_name,description) VALUES
(1,'乐器','钢琴、吉他、声乐等器乐类课程'),
(2,'舞蹈','中国舞、街舞等舞蹈类课程'),
(3,'美术','创意美术、素描等美术类课程'),
(4,'声乐','少儿声乐演唱类课程');

INSERT INTO course (course_id,course_no,course_name,category,age_group,level,total_hours,lesson_duration,original_price,price,intro,status,sort_order) VALUES
(1,'C001','钢琴启蒙','乐器','4-6岁','启蒙',32,45,1999,1599,'通过趣味小曲目建立正确手型与节奏感。',1,1),
(2,'C002','钢琴提高','乐器','7-12岁','提高',48,45,2999,2599,'巩固指法与乐理，提升曲目表现力。',1,2),
(3,'C003','中国舞启蒙','舞蹈','4-8岁','启蒙',36,60,1799,1499,'基本功、身韵与儿童舞蹈组合训练。',1,3),
(4,'C004','创意美术','美术','5-10岁','启蒙',32,60,1599,1299,'多元材料创作，激发想象力与审美。',1,4),
(5,'C005','少儿声乐','声乐','6-12岁','启蒙',32,45,1699,1399,'科学发声、音准与少儿歌曲演唱。',1,5),
(6,'C006','吉他入门','乐器','8-15岁','入门',24,45,1399,1099,'民谣吉他弹唱入门课程。',0,6);

INSERT INTO teacher_course (teacher_id,course_id) VALUES
(1,1),(1,2),(2,3),(2,5),(3,4);

INSERT INTO course_class (class_id,course_id,class_name,category_id,teacher_id,classroom_id,semester,start_date,end_date,total_hours,lesson_duration,tuition,max_students,status,age_group,level,description) VALUES
(1,1,'钢琴启蒙一班',1,1,1,'2026秋季','2026-08-24','2026-12-31',32,45,1599,8,1,'4-6岁','启蒙','钢琴启蒙阶段班级'),
(2,2,'钢琴提高班',1,1,2,'2026秋季','2026-08-24','2026-12-31',48,45,2599,8,1,'7-12岁','提高','钢琴提高阶段班级'),
(3,3,'少儿舞蹈一班',2,2,3,'2026秋季','2026-08-22','2026-12-31',36,60,1499,12,1,'4-8岁','启蒙','中国舞启蒙班级'),
(4,4,'创意美术班',3,3,4,'2026秋季','2026-08-22','2026-12-31',32,60,1299,10,1,'5-10岁','启蒙','创意美术班级'),
(5,5,'声乐兴趣班',4,2,5,'2026秋季','2026-08-21','2026-12-31',32,45,1399,10,1,'6-12岁','启蒙','少儿声乐班级');

INSERT INTO course_schedule (schedule_id,class_id,teacher_id,classroom_id,course_date,start_time,end_time,hours,status,remark) VALUES
(1,1,1,1,'2026-08-24','08:00:00','08:45:00','0.75','已完成',NULL),
(2,1,1,1,'2026-08-26','08:00:00','08:45:00','0.75','已完成',NULL),
(3,1,1,1,'2026-08-28','08:00:00','08:45:00','0.75','已完成',NULL),
(4,1,1,1,'2026-08-29','08:00:00','08:45:00','0.75','待上课',NULL),
(5,1,1,1,'2026-08-31','08:00:00','08:45:00','0.75','待上课',NULL),
(6,1,1,1,'2026-09-02','08:00:00','08:45:00','0.75','待上课',NULL),
(7,1,1,1,'2026-09-04','08:00:00','08:45:00','0.75','待上课',NULL),
(8,2,1,2,'2026-08-24','09:00:00','09:45:00','0.75','已完成',NULL),
(9,2,1,2,'2026-08-26','09:00:00','09:45:00','0.75','已完成',NULL),
(10,2,1,2,'2026-08-28','09:00:00','09:45:00','0.75','已完成',NULL),
(11,2,1,2,'2026-08-29','09:00:00','09:45:00','0.75','待上课',NULL),
(12,2,1,2,'2026-08-31','09:00:00','09:45:00','0.75','待上课',NULL),
(13,2,1,2,'2026-09-02','09:00:00','09:45:00','0.75','待上课',NULL),
(14,2,1,2,'2026-09-04','09:00:00','09:45:00','0.75','待上课',NULL),
(15,3,2,3,'2026-08-22','10:00:00','11:00:00','1.0','已完成',NULL),
(16,3,2,3,'2026-08-25','10:00:00','11:00:00','1.0','已完成',NULL),
(17,3,2,3,'2026-08-27','10:00:00','11:00:00','1.0','已完成',NULL),
(18,3,2,3,'2026-08-29','10:00:00','11:00:00','1.0','待上课',NULL),
(19,3,2,3,'2026-09-01','10:00:00','11:00:00','1.0','待上课',NULL),
(20,3,2,3,'2026-09-03','10:00:00','11:00:00','1.0','待上课',NULL),
(21,3,2,3,'2026-09-05','10:00:00','11:00:00','1.0','待上课',NULL),
(22,4,3,4,'2026-08-22','14:00:00','15:00:00','1.0','已完成',NULL),
(23,4,3,4,'2026-08-26','14:00:00','15:00:00','1.0','已完成',NULL),
(24,4,3,4,'2026-08-29','14:00:00','15:00:00','1.0','待上课',NULL),
(25,4,3,4,'2026-09-02','14:00:00','15:00:00','1.0','待上课',NULL),
(26,4,3,4,'2026-09-05','14:00:00','15:00:00','1.0','待上课',NULL),
(27,4,3,4,'2026-09-09','14:00:00','15:00:00','1.0','待上课',NULL),
(28,5,2,5,'2026-08-21','16:00:00','16:45:00','0.75','已完成',NULL),
(29,5,2,5,'2026-08-28','16:00:00','16:45:00','0.75','已完成',NULL),
(30,5,2,5,'2026-09-04','16:00:00','16:45:00','0.75','待上课',NULL);

INSERT INTO enrollment (enrollment_id,student_id,class_id,enroll_date,tuition_amount,status) VALUES
(1,1,1,'2026-08-20',1599,'在读'),
(2,2,1,'2026-08-20',1599,'在读'),
(3,6,1,'2026-08-21',1599,'退学'),
(4,2,2,'2026-08-19',2599,'在读'),
(5,3,2,'2026-08-19',2599,'在读'),
(6,4,2,'2026-08-22',2599,'在读'),
(7,1,3,'2026-08-18',1499,'在读'),
(8,5,3,'2026-08-18',1499,'在读'),
(9,3,4,'2026-08-15',1299,'结课'),
(10,5,4,'2026-08-17',1299,'在读'),
(11,6,5,'2026-08-16',1399,'在读');

INSERT INTO attendance (attendance_id,schedule_id,student_id,status,record_time,remark) VALUES
(1,1,1,'出勤','2026-08-24 19:00:00',NULL),
(2,1,2,'出勤','2026-08-24 19:00:00',NULL),
(3,2,1,'请假','2026-08-26 19:00:00',NULL),
(4,2,2,'迟到','2026-08-26 19:00:00',NULL),
(5,3,1,'迟到','2026-08-28 19:00:00',NULL),
(6,3,2,'出勤','2026-08-28 19:00:00',NULL),
(7,8,2,'出勤','2026-08-24 19:00:00',NULL),
(8,8,3,'出勤','2026-08-24 19:00:00',NULL),
(9,8,4,'迟到','2026-08-24 19:00:00',NULL),
(10,9,2,'迟到','2026-08-26 19:00:00',NULL),
(11,9,3,'迟到','2026-08-26 19:00:00',NULL),
(12,9,4,'出勤','2026-08-26 19:00:00',NULL),
(13,10,2,'迟到','2026-08-28 19:00:00',NULL),
(14,10,3,'出勤','2026-08-28 19:00:00',NULL),
(15,10,4,'缺勤','2026-08-28 19:00:00',NULL),
(16,15,1,'出勤','2026-08-22 19:00:00',NULL),
(17,15,5,'出勤','2026-08-22 19:00:00',NULL),
(18,16,1,'出勤','2026-08-25 19:00:00',NULL),
(19,16,5,'迟到','2026-08-25 19:00:00',NULL),
(20,17,1,'迟到','2026-08-27 19:00:00',NULL),
(21,17,5,'迟到','2026-08-27 19:00:00',NULL),
(22,22,5,'出勤','2026-08-22 19:00:00',NULL),
(23,23,5,'出勤','2026-08-26 19:00:00',NULL),
(24,28,6,'出勤','2026-08-21 19:00:00',NULL),
(25,29,6,'出勤','2026-08-28 19:00:00',NULL);


UPDATE enrollment SET attended_count=2, absent_count=1 WHERE student_id=1 AND class_id=1;

UPDATE enrollment SET attended_count=3, absent_count=0 WHERE student_id=2 AND class_id=1;

UPDATE enrollment SET attended_count=3, absent_count=0 WHERE student_id=2 AND class_id=2;

UPDATE enrollment SET attended_count=3, absent_count=0 WHERE student_id=3 AND class_id=2;

UPDATE enrollment SET attended_count=2, absent_count=1 WHERE student_id=4 AND class_id=2;

UPDATE enrollment SET attended_count=3, absent_count=0 WHERE student_id=1 AND class_id=3;

UPDATE enrollment SET attended_count=3, absent_count=0 WHERE student_id=5 AND class_id=3;

UPDATE enrollment SET attended_count=2, absent_count=0 WHERE student_id=5 AND class_id=4;

UPDATE enrollment SET attended_count=2, absent_count=0 WHERE student_id=6 AND class_id=5;

INSERT INTO homework (homework_id,class_id,schedule_id,title,content,create_time,deadline) VALUES
(1,1,NULL,'钢琴启蒙-手型练习','保持正确手型，练习《钟声响了》并录制视频','2026-08-25 18:00:00','2026-09-06 20:00:00'),
(2,2,NULL,'钢琴提高-C大调音阶','节拍器60速度，上下行各练习10遍','2026-08-25 18:05:00','2026-09-04 20:00:00'),
(3,3,NULL,'舞蹈-基本功组合','练习勾绷脚与前抬腿组合，跟音乐录制','2026-08-26 09:00:00','2026-09-08 20:00:00'),
(4,4,NULL,'创意美术-我的家庭','用任意材料创作一幅家庭主题画并拍照上传','2026-08-26 15:00:00','2026-09-10 18:00:00');

INSERT INTO homework_submission (submission_id,homework_id,student_id,content,attachment_url,attachment_name,submit_time,score,teacher_comment,status) VALUES
(1,1,1,'已按要求完成，视频已录制',NULL,NULL,'2026-08-26 19:00:00',90,'手型标准，继续保持','已批改'),
(2,1,2,'已练习30分钟',NULL,NULL,'2026-08-27 20:10:00',NULL,NULL,'已提交'),
(3,2,2,'音阶练习完成',NULL,NULL,'2026-08-25 18:30:00',85,'音阶较稳，注意指法','已批改'),
(4,2,3,'每天坚持练习',NULL,NULL,'2026-08-26 19:20:00',88,'节奏准确，继续提速','已批改'),
(5,2,4,'',NULL,NULL,NULL,NULL,NULL,'未提交'),
(6,3,1,'组合练习视频',NULL,NULL,'2026-08-27 11:00:00',92,'动作规范，表现力好','已批改'),
(7,3,5,'',NULL,NULL,NULL,NULL,NULL,'未提交'),
(8,4,5,'作品照片已上传',NULL,NULL,'2026-08-28 17:30:00',NULL,NULL,'已提交');

INSERT INTO exam_project (project_id,name,activity_type,category,organizer,signup_start,signup_end,exam_date,exam_start,exam_end,exam_room,location,fee,requirement,award_desc,materials,status) VALUES
(1,'某音乐学院钢琴考级','考级','乐器','某音乐学院','2026-08-01','2026-09-20','2026-10-12','09:00','10:30','第一考场','某城市艺术中心',200,'三级曲目一首，需上传身份证明','合格/良好/优秀','身份证、一寸照片','报名中'),
(2,'少儿舞蹈大赛','比赛','舞蹈','滨州市舞蹈家协会','2026-08-01','2026-09-30','2026-10-18','14:00','15:30','大剧院A厅','某城市大礼堂',150,'3分钟以内独舞/群舞节目','一/二/三等奖','节目音乐、报名表','报名中'),
(3,'创意美术作品展','比赛','美术','市青少年活动中心','2026-08-10','2026-10-10','2026-10-25','10:00','11:00','美术展厅','某城市美术展厅',0,'提交原创主题画作一幅','入选/优秀作品','作品电子稿','报名中');

INSERT INTO exam_registration (reg_id,project_id,student_id,class_id,real_name,gender,birth_date,id_card,phone,material_url,teacher_id,teacher_comment,status,audit_remark,exam_time,exam_room,seat_no,result,award,certificate_no,create_time) VALUES
(1,1,1,1,'小明','男','2014-05-10',NULL,'13800000024',NULL,1,'基本功扎实，建议报考三级','已完成','审核通过','2026-10-12 09:00-10:30','第一考场','01','通过','良好','CCSA-2026-001','2026-08-15 10:00:00'),
(2,1,2,1,'小红','女',NULL,NULL,'13800000025',NULL,1,'课堂表现稳定','已缴费','审核通过','2026-10-12 09:00-10:30','第一考场','02',NULL,NULL,NULL,'2026-08-16 10:00:00'),
(3,2,5,3,'小美','女','2014-01-19',NULL,'13800000026',NULL,2,'舞蹈表现力强','已确认','审核通过',NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-18 10:00:00'),
(4,2,3,NULL,'小刚','男','2013-02-14',NULL,'13800000027',NULL,NULL,NULL,'待审核',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-22 10:00:00'),
(5,3,1,4,'小明','男','2014-05-10',NULL,'13800000028',NULL,NULL,NULL,'已驳回','材料不全，请补充作品电子稿',NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-20 10:00:00'),
(6,3,5,4,'小美','女','2014-01-19',NULL,'13800000029',NULL,NULL,NULL,'待审核',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-23 10:00:00');

INSERT INTO payment (payment_id,student_id,class_id,enrollment_id,reg_id,pay_date,amount,pay_type,pay_method,operator_id,remark,status,refund_time,refund_operator_id) VALUES
(1,1,1,1,NULL,'2026-08-20 10:30:00',1599,'报名','微信',1,'钢琴启蒙一班报名费','已支付',NULL,NULL),
(2,2,1,2,NULL,'2026-08-20 11:00:00',1599,'报名','支付宝',1,'钢琴启蒙一班报名费','已支付',NULL,NULL),
(3,2,2,4,NULL,'2026-08-19 09:30:00',2599,'报名','微信',1,'钢琴提高班报名费','已支付',NULL,NULL),
(4,3,2,5,NULL,'2026-08-19 10:00:00',2599,'报名','现金',1,'钢琴提高班报名费','已支付',NULL,NULL),
(5,4,2,6,NULL,'2026-08-22 14:00:00',2599,'报名','刷卡',1,'钢琴提高班报名费','已支付',NULL,NULL),
(6,1,3,7,NULL,'2026-08-18 15:00:00',1499,'报名','微信',1,'少儿舞蹈一班报名费','已支付',NULL,NULL),
(7,5,3,8,NULL,'2026-08-18 15:30:00',1499,'报名','支付宝',1,'少儿舞蹈一班报名费','已支付',NULL,NULL),
(8,5,4,10,NULL,'2026-08-17 16:00:00',1299,'报名','微信',1,'创意美术班报名费','已支付',NULL,NULL),
(9,6,5,11,NULL,'2026-08-16 10:00:00',1399,'报名','现金',1,'声乐兴趣班报名费','已支付',NULL,NULL),
(10,2,2,4,NULL,'2026-08-25 10:00:00',200,'续费','微信',1,'钢琴提高班课时续费','已支付',NULL,NULL),
(11,1,NULL,NULL,1,'2026-08-17 09:00:00',200,'考级','微信',1,'钢琴考级报名费','已支付',NULL,NULL),
(12,2,NULL,NULL,2,'2026-08-19 09:00:00',200,'考级','支付宝',1,'钢琴考级报名费','已支付',NULL,NULL),
(13,5,3,8,NULL,'2026-08-21 10:00:00',1499,'报名','微信',1,'重复缴费，已退款','已退款','2026-08-23 10:00:00',1),
(14,5,3,8,NULL,'2026-08-23 10:00:00',-1499,'退款','微信',1,'重复缴费退款冲账','已退款','2026-08-23 10:00:00',1);

UPDATE enrollment e SET total_paid=(SELECT COALESCE(SUM(p.amount),0) FROM payment p WHERE p.enrollment_id=e.enrollment_id AND p.status='已支付');

INSERT INTO visitor_booking (booking_id,name,phone,class_id,teacher_id,preferred_time,remark,status,create_time) VALUES
(1,'试听家长张','13800000030',1,1,'2026-09-05 10:00:00','想了解钢琴启蒙课','待确认','2026-08-27 09:00:00'),
(2,'试听家长李','13800000031',3,2,'2026-09-06 14:30:00','舞蹈零基础试听','已确认','2026-08-26 09:00:00'),
(3,'试听家长王','13800000032',4,3,'2026-09-03 15:00:00','喜欢画画，想试听美术课','已取消','2026-08-25 09:00:00');

INSERT INTO appointment (appointment_id,student_id,teacher_id,class_id,appointment_type,appointment_time,status,remark) VALUES
(1,1,1,1,'一对一辅导','2026-09-02 16:00:00','待确认','课后答疑，请教手型问题'),
(2,5,2,3,'补课','2026-09-01 15:00:00','已接受','补上一节请假的舞蹈课');

INSERT INTO feedback (feedback_id,student_id,content,type,create_time,status,reply_content,reply_time,reply_by) VALUES
(1,1,'希望增加周末练琴时段','建议','2026-08-25 12:00:00','已回复','已反馈给教务，将统筹安排练琴时段','2026-08-26 09:00:00','系统管理员'),
(2,3,'舞蹈教室把杆有点松动，麻烦检修','报修','2026-08-27 12:00:00','待处理',NULL,NULL,NULL),
(3,5,'想咨询美术画材是否需要自备','咨询','2026-08-28 12:00:00','待处理',NULL,NULL,NULL);

INSERT INTO student_request (request_id,student_id,teacher_id,class_id,request_type,request_date,reason,status,reply_remark,create_time,handle_time) VALUES
(1,1,1,1,'请假','2026-08-26','家中有事，申请请假一次','已批准','同意，注意回看课堂录像','2026-08-25 20:00:00','2026-08-25 21:00:00'),
(2,3,1,2,'补考','2026-08-20','上次模拟测试未参加，申请补考','已批准','同意，安排在下周课后','2026-08-19 20:00:00','2026-08-20 09:00:00'),
(3,4,1,2,'请假','2026-09-02','当天有其他安排，申请请假','待处理',NULL,'2026-08-28 20:00:00',NULL),
(4,6,2,5,'休学','2026-09-01','身体原因申请休学一学期','待处理',NULL,'2026-08-28 21:00:00',NULL);

-- 请假考勤与已批准请假申请对齐
UPDATE attendance SET request_id=1 WHERE attendance_id=3;

INSERT INTO student_comment (comment_id,student_id,class_id,teacher_id,content,create_time) VALUES
(1,2,1,1,'课堂专注，手型进步明显，继续保持。','2026-08-28 18:00:00'),
(2,3,1,2,'节奏感好，注意高抬指的独立性练习。','2026-08-28 18:05:00');

INSERT INTO reschedule_request (request_id,schedule_id,target_schedule_id,new_date,new_start_time,new_end_time,reason,status,handle_remark,apply_time,handler_id,handle_time) VALUES
(1,5,NULL,'2026-09-03','18:00:00','18:45:00','当天临时有会议，申请调到晚间','待审核',NULL,NULL,NULL,'2026-08-28 10:00:00'),
(2,8,NULL,'2026-08-23','09:00:00','09:45:00','配合教室调整提前一天上课','已通过','同意调课','2026-08-22 15:00:00',1,'2026-08-22 16:00:00'),
(3,19,NULL,'2026-09-08','10:00:00','11:00:00','申请顺延一周','已拒绝','当周课程进度紧张，不同意顺延',NULL,1,'2026-08-27 16:00:00');

INSERT INTO message (message_id,receiver_user_id,msg_type,title,content,biz_type,biz_id,is_read,create_time) VALUES
(1,10,'作业','作业已批改','钢琴启蒙-手型练习已批改：90分','homework_submission',1,1,'2026-08-26 19:30:00'),
(2,10,'考级','考级/比赛成绩已发布','某音乐学院钢琴考级结果：通过（良好）','exam_registration',1,1,'2026-08-27 10:00:00'),
(3,11,'考级','考级/比赛报名已通过','钢琴考级报名审核通过，请及时缴费','exam_registration',2,0,'2026-08-17 09:00:00'),
(4,11,'作业','作业已批改','C大调音阶已批改：85分','homework_submission',3,1,'2026-08-25 19:00:00'),
(5,2,'申请','有新的请假申请','小丽提交了一条请假申请，请处理','student_request',3,0,'2026-08-28 20:05:00'),
(6,14,'作业','作业已批改','舞蹈基本功组合已批改：92分','homework_submission',6,1,'2026-08-27 11:30:00'),
(7,10,'预约','您的预约待确认','一对一辅导预约等待老师确认','appointment',1,0,'2026-08-28 18:00:00');

INSERT INTO news (news_id,title,content,publisher,publish_time,view_count) VALUES
(1,'2026秋季学期开课通知','秋季学期已于8月下旬陆续开课，请各位学员按课表时间提前10分钟到校上课。','系统管理员','2026-08-20 09:00:00',128),
(2,'国庆考级集训营报名开始','针对10月钢琴考级与舞蹈大赛，中心开设考前集训营，欢迎到前台咨询报名。','系统管理员','2026-08-26 09:00:00',56);

INSERT INTO system_config (config_key,config_value) VALUES
('org_name','星海艺术培训中心'),
('org_phone','0543-8888888'),
('org_address','某城市（柳州市城中区东环大道268号）'),
('org_intro','星海艺术培训中心专注少儿艺术教育，开设乐器、舞蹈、美术、声乐课程，师资力量雄厚。'),
('logo_url',''),
('banner_images',''),
('org_wechat','xh-art2026');
