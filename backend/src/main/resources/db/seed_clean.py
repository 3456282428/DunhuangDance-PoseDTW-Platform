# -*- coding: utf-8 -*-
"""清空并重建一套覆盖全流程的干净演示数据（基准日 2026-08-29）。"""
import os
import pymysql

HASH = "$2b$10$jjnp4KP/9fmHS9JPO8BHu.1ofkz7aj755T7YsfzL0YP2Qq7KS7Nd2"  # 演示密码 123456 的 BCrypt 值（仅用于本地演示数据）
stmts = []
def add(sql): stmts.append(sql)
def esc(v):
    if v is None: return "NULL"
    if isinstance(v, (int, float)): return str(v)
    return "'" + str(v).replace("'", "''") + "'"
def insert(table, cols, rows):
    add(f"INSERT INTO {table} ({','.join(cols)}) VALUES\n" +
        ",\n".join("(" + ",".join(esc(v) for v in r) + ")" for r in rows) + ";")

TABLES = ["message","reschedule_request","student_comment","student_request","feedback","appointment",
          "visitor_booking","payment","exam_competition","exam_registration","exam_project",
          "homework_submission","homework","attendance","enrollment","course_schedule","course_class",
          "course","course_category","classroom","student","teacher","sys_user","news","system_config"]

add("SET FOREIGN_KEY_CHECKS=0;")
for t in TABLES: add(f"TRUNCATE TABLE {t};")
add("SET FOREIGN_KEY_CHECKS=1;")

# ---------------- 账号 ----------------
users = [
 (1,"admin",HASH,"ADMIN","系统管理员","13800000001",1),
 (2,"T001",HASH,"TEACHER","张老师","13800000002",1),
 (3,"T002",HASH,"TEACHER","李老师","13800000003",1),
 (4,"T003",HASH,"TEACHER","王老师","13800000004",1),
]
for i,sid in enumerate(range(10,16)):
    names=["小明","小红","小刚","小丽","小美","小强"]; phones=[f"1380000{i:04d}" for i in range(1,7)]
    users.append((sid,f"2024010{i+1}",HASH,"STUDENT",names[i],phones[i],1))
users.append((20,"visitor1",HASH,"VISITOR","试听游客","13800000005",1))
insert("sys_user",["user_id","username","password","role","real_name","phone","status"],users)

# ---------------- 教师 ----------------
teachers=[
 (1,"T001",2,"张老师","男","1988-03-12","13800000006","zhang@example.com","滨州市滨城区","张父","13800000007","钢琴","本科","中央音乐学院","高级教师","全职",12,"1,2",120,2000,"资深钢琴教师，擅长少儿启蒙教学。","2020-09-01",1),
 (2,"T002",3,"李老师","女","1992-07-05","13800000008","li@example.com","滨州市滨城区","李母","13800000009","舞蹈","硕士","北京舞蹈学院","一级教师","全职",8,"3,5",110,1800,"中国舞专业，教学风格活泼细致。","2022-03-01",1),
 (3,"T003",4,"王老师","男","1990-11-20","13800000010","wang@example.com","滨州市滨城区","王父","13800000011","美术","本科","中国美术学院","中级教师","兼职",6,"4",100,0,"创意美术教师，注重孩子想象力培养。","2023-06-01",1),
]
insert("teacher",["teacher_id","teacher_no","user_id","name","gender","birth_date","phone","email","address","emergency_contact","emergency_phone","major","education","school","title","employ_type","teach_years","course_ids","hourly_rate","base_salary","intro","hire_date","status"],teachers)

# ---------------- 学员 ----------------
snames=["小明","小红","小刚","小丽","小美","小强"]; genders=["男","女","男","女","女","男"]
births=["2014-05-10","2014-08-22","2013-02-14","2015-11-03","2014-01-19","2013-09-30"]
students=[]
for i in range(6):
    students.append((i+1,f"2024010{i+1}",10+i,snames[i],genders[i],births[i],f"1380000{i+1:04d}",f"stu{i+1}@example.com","2024-09-01",1))
insert("student",["student_id","student_no","user_id","name","gender","birth_date","parent_phone","email","enroll_date","status"],students)

# ---------------- 教室 ----------------
rooms=[(1,"钢琴教室1",8,"钢琴教室",1),(2,"钢琴教室2",8,"钢琴教室",1),(3,"舞蹈教室",15,"舞蹈教室",1),(4,"美术教室",12,"美术教室",1),(5,"声乐教室",10,"声乐教室",1)]
insert("classroom",["classroom_id","classroom_name","capacity","room_type","status"],rooms)

# ---------------- 课程类别 / 课程产品 ----------------
cats=[(1,"乐器","钢琴、吉他、声乐等器乐类课程"),(2,"舞蹈","中国舞、街舞等舞蹈类课程"),(3,"美术","创意美术、素描等美术类课程"),(4,"声乐","少儿声乐演唱类课程")]
insert("course_category",["category_id","category_name","description"],cats)
courses=[
 (1,"C001","钢琴启蒙","乐器","4-6岁","启蒙",32,45,1999,1599,"通过趣味小曲目建立正确手型与节奏感。",1,1),
 (2,"C002","钢琴提高","乐器","7-12岁","提高",48,45,2999,2599,"巩固指法与乐理，提升曲目表现力。",1,2),
 (3,"C003","中国舞启蒙","舞蹈","4-8岁","启蒙",36,60,1799,1499,"基本功、身韵与儿童舞蹈组合训练。",1,3),
 (4,"C004","创意美术","美术","5-10岁","启蒙",32,60,1599,1299,"多元材料创作，激发想象力与审美。",1,4),
 (5,"C005","少儿声乐","声乐","6-12岁","启蒙",32,45,1699,1399,"科学发声、音准与少儿歌曲演唱。",1,5),
 (6,"C006","吉他入门","乐器","8-15岁","入门",24,45,1399,1099,"民谣吉他弹唱入门课程。",0,6),
]
insert("course",["course_id","course_no","course_name","category","age_group","level","total_hours","lesson_duration","original_price","price","intro","status","sort_order"],courses)

# ---------------- 班级 ----------------
classes=[
 (1,1,"钢琴启蒙一班",1,1,1,"2026秋季","2026-08-24","2026-12-31",32,45,1599,8,1,"4-6岁","启蒙","钢琴启蒙阶段班级"),
 (2,2,"钢琴提高班",1,1,2,"2026秋季","2026-08-24","2026-12-31",48,45,2599,8,1,"7-12岁","提高","钢琴提高阶段班级"),
 (3,3,"少儿舞蹈一班",2,2,3,"2026秋季","2026-08-22","2026-12-31",36,60,1499,12,1,"4-8岁","启蒙","中国舞启蒙班级"),
 (4,4,"创意美术班",3,3,4,"2026秋季","2026-08-22","2026-12-31",32,60,1299,10,1,"5-10岁","启蒙","创意美术班级"),
 (5,5,"声乐兴趣班",4,2,5,"2026秋季","2026-08-21","2026-12-31",32,45,1399,10,1,"6-12岁","启蒙","少儿声乐班级"),
]
insert("course_class",["class_id","course_id","class_name","category_id","teacher_id","classroom_id","semester","start_date","end_date","total_hours","lesson_duration","tuition","max_students","status","age_group","level","description"],classes)

# ---------------- 排课（已完成 / 今天 / 未来） ----------------
# (class,teacher,room,date,start,end,status)
sched=[]
def srow(cid,tid,rid,d,st,et,status):
    h={"08:00:00":"0.75","09:00:00":"0.75","10:00:00":"1.0","14:00:00":"1.0","16:00:00":"0.75"}[st]
    sched.append((cid,tid,rid,d,st,et,h,status,None))
for d in ["2026-08-24","2026-08-26","2026-08-28"]: srow(1,1,1,d,"08:00:00","08:45:00","已完成")
for d in ["2026-08-29","2026-08-31","2026-09-02","2026-09-04"]: srow(1,1,1,d,"08:00:00","08:45:00","待上课")
for d in ["2026-08-24","2026-08-26","2026-08-28"]: srow(2,1,2,d,"09:00:00","09:45:00","已完成")
for d in ["2026-08-29","2026-08-31","2026-09-02","2026-09-04"]: srow(2,1,2,d,"09:00:00","09:45:00","待上课")
for d in ["2026-08-22","2026-08-25","2026-08-27"]: srow(3,2,3,d,"10:00:00","11:00:00","已完成")
for d in ["2026-08-29","2026-09-01","2026-09-03","2026-09-05"]: srow(3,2,3,d,"10:00:00","11:00:00","待上课")
for d in ["2026-08-22","2026-08-26"]: srow(4,3,4,d,"14:00:00","15:00:00","已完成")
for d in ["2026-08-29","2026-09-02","2026-09-05","2026-09-09"]: srow(4,3,4,d,"14:00:00","15:00:00","待上课")
for d in ["2026-08-21","2026-08-28"]: srow(5,2,5,d,"16:00:00","16:45:00","已完成")
srow(5,2,5,"2026-09-04","16:00:00","16:45:00","待上课")
sched_rows=[(i+1,)+r for i,r in enumerate(sched)]
insert("course_schedule",["schedule_id","class_id","teacher_id","classroom_id","course_date","start_time","end_time","hours","status","remark"],sched_rows)
SCHED={}
for r in sched_rows: SCHED[(r[1],r[4])]=r[0]  # (class,date)->schedule_id

# ---------------- 报名（在读/结课/退学，含多课学员） ----------------
enr=[
 (1,1,1,"2026-08-20",1599,"在读"),(2,2,1,"2026-08-20",1599,"在读"),(3,6,1,"2026-08-21",1599,"退学"),
 (4,2,2,"2026-08-19",2599,"在读"),(5,3,2,"2026-08-19",2599,"在读"),(6,4,2,"2026-08-22",2599,"在读"),
 (7,1,3,"2026-08-18",1499,"在读"),(8,5,3,"2026-08-18",1499,"在读"),
 (9,3,4,"2026-08-15",1299,"结课"),(10,5,4,"2026-08-17",1299,"在读"),
 (11,6,5,"2026-08-16",1399,"在读"),
]
insert("enrollment",["enrollment_id","student_id","class_id","enroll_date","tuition_amount","status"],enr)
ACTIVE={1:[1,2],2:[2,3,4],3:[1,5],4:[5],5:[6]}  # 各班在读学员
DONE_DATES={1:["2026-08-24","2026-08-26","2026-08-28"],2:["2026-08-24","2026-08-26","2026-08-28"],
            3:["2026-08-22","2026-08-25","2026-08-27"],4:["2026-08-22","2026-08-26"],5:["2026-08-21","2026-08-28"]}

# ---------------- 考勤（覆盖出勤/迟到/缺勤/请假） ----------------
# 指定特例：学员1 在班级1 于 8/26 请假（与已批准请假申请对应）
special={(1,1,"2026-08-26"):"请假",(2,2,"2026-08-26"):"迟到",(4,2,"2026-08-28"):"缺勤",
         (5,3,"2026-08-27"):"迟到",(6,5,"2026-08-28"):"出勤",(5,4,"2026-08-26"):"出勤"}
cycle=["出勤","出勤","迟到","出勤","缺勤","出勤","出勤"]
att=[]; aid=0
for cid,ss in ACTIVE.items():
    for di,d in enumerate(DONE_DATES[cid]):
        for si,sid in enumerate(ss):
            st=special.get((sid,cid,d)) or cycle[(di+si)%len(cycle)]
            aid+=1
            att.append((aid,SCHED[(cid,d)],sid,st,f"{d} 19:00:00",None))
insert("attendance",["attendance_id","schedule_id","student_id","status","record_time","remark"],att)

# 回写 enrollment 出勤统计（脚本计算）
enr_stat={}
for a in att:
    schedule_id=a[1]; sid=a[2]; st=a[3]
    cid=[c for c,d in DONE_DATES.items() if schedule_id in [SCHED[(c,x)] for x in d]][0]
    k=(sid,cid); enr_stat.setdefault(k,[0,0])
    if st in ("出勤","迟到"): enr_stat[k][0]+=1
    if st in ("缺勤","请假"): enr_stat[k][1]+=1
for (sid,cid),(atd,abs_) in enr_stat.items():
    add(f"UPDATE enrollment SET attended_count={atd}, absent_count={abs_} WHERE student_id={sid} AND class_id={cid};")

# ---------------- 作业与提交 ----------------
hws=[
 (1,1,None,"钢琴启蒙-手型练习","保持正确手型，练习《钟声响了》并录制视频","2026-08-25 18:00:00","2026-09-06 20:00:00"),
 (2,2,None,"钢琴提高-C大调音阶","节拍器60速度，上下行各练习10遍","2026-08-25 18:05:00","2026-09-04 20:00:00"),
 (3,3,None,"舞蹈-基本功组合","练习勾绷脚与前抬腿组合，跟音乐录制","2026-08-26 09:00:00","2026-09-08 20:00:00"),
 (4,4,None,"创意美术-我的家庭","用任意材料创作一幅家庭主题画并拍照上传","2026-08-26 15:00:00","2026-09-10 18:00:00"),
]
insert("homework",["homework_id","class_id","schedule_id","title","content","create_time","deadline"],hws)
subs=[
 (1,1,1,"已按要求完成，视频已录制",None,None,"2026-08-26 19:00:00",90,"手型标准，继续保持","已批改"),
 (2,1,2,"已练习30分钟",None,None,"2026-08-27 20:10:00",None,None,"已提交"),
 (3,2,2,"音阶练习完成",None,None,"2026-08-25 18:30:00",85,"音阶较稳，注意指法","已批改"),
 (4,2,3,"每天坚持练习",None,None,"2026-08-26 19:20:00",88,"节奏准确，继续提速","已批改"),
 (5,2,4,"",None,None,None,None,None,"未提交"),
 (6,3,1,"组合练习视频",None,None,"2026-08-27 11:00:00",92,"动作规范，表现力好","已批改"),
 (7,3,5,"",None,None,None,None,None,"未提交"),
 (8,4,5,"作品照片已上传",None,None,"2026-08-28 17:30:00",None,None,"已提交"),
]
insert("homework_submission",["submission_id","homework_id","student_id","content","attachment_url","attachment_name","submit_time","score","teacher_comment","status"],subs)

# ---------------- 考级/比赛项目 ----------------
projects=[
 (1,"中国音乐学院钢琴考级","考级","乐器","中国音乐学院","2026-08-01","2026-09-20","2026-10-12","09:00","10:30","第一考场","滨州市文化艺术中心",200,"三级曲目一首，需上传身份证明","合格/良好/优秀","身份证、一寸照片","报名中"),
 (2,"少儿舞蹈大赛","比赛","舞蹈","滨州市舞蹈家协会","2026-08-01","2026-09-30","2026-10-18","14:00","15:30","大剧院A厅","滨州市大剧院",150,"3分钟以内独舞/群舞节目","一/二/三等奖","节目音乐、报名表","报名中"),
 (3,"创意美术作品展","比赛","美术","市青少年活动中心","2026-08-10","2026-10-10","2026-10-25","10:00","11:00","美术展厅","美术展厅",0,"提交原创主题画作一幅","入选/优秀作品","作品电子稿","报名中"),
]
insert("exam_project",["project_id","name","activity_type","category","organizer","signup_start","signup_end","exam_date","exam_start","exam_end","exam_room","location","fee","requirement","award_desc","materials","status"],projects)

# ---------------- 考级报名（覆盖全部状态） ----------------
regs=[
 (1,1,1,1,"小明","男","2014-05-10",None,"13800000012",None,1,"基本功扎实，建议报考三级","已完成","审核通过","2026-10-12 09:00-10:30","第一考场","01","通过","良好","CCSA-2026-001","2026-08-15 10:00:00"),
 (2,1,2,1,"小红","女",None,None,"13800000013",None,1,"课堂表现稳定","已缴费","审核通过","2026-10-12 09:00-10:30","第一考场","02",None,None,None,"2026-08-16 10:00:00"),
 (3,2,5,3,"小美","女","2014-01-19",None,"13800000014",None,2,"舞蹈表现力强","已确认","审核通过",None,None,None,None,None,None,"2026-08-18 10:00:00"),
 (4,2,3,None,"小刚","男","2013-02-14",None,"13800000015",None,None,None,"待审核",None,None,None,None,None,None,None,"2026-08-22 10:00:00"),
 (5,3,1,4,"小明","男","2014-05-10",None,"13800000016",None,None,None,"已驳回","材料不全，请补充作品电子稿",None,None,None,None,None,None,"2026-08-20 10:00:00"),
 (6,3,5,4,"小美","女","2014-01-19",None,"13800000017",None,None,None,"待审核",None,None,None,None,None,None,None,"2026-08-23 10:00:00"),
]
insert("exam_registration",["reg_id","project_id","student_id","class_id","real_name","gender","birth_date","id_card","phone","material_url","teacher_id","teacher_comment","status","audit_remark","exam_time","exam_room","seat_no","result","award","certificate_no","create_time"],regs)

# 成果台账
insert("exam_competition",["activity_id","student_id","class_id","activity_type","activity_name","activity_date","result","award","reg_id"],
 [(1,1,1,"考级","中国音乐学院钢琴考级","2026-10-12","通过","良好",1)])

# ---------------- 缴费（报名/续费/考级/比赛 + 已退款） ----------------
pays=[
 (1,1,1,1,None,"2026-08-20 10:30:00",1599,"报名","微信",1,"钢琴启蒙一班报名费","已支付",None,None),
 (2,2,1,2,None,"2026-08-20 11:00:00",1599,"报名","支付宝",1,"钢琴启蒙一班报名费","已支付",None,None),
 (3,2,2,4,None,"2026-08-19 09:30:00",2599,"报名","微信",1,"钢琴提高班报名费","已支付",None,None),
 (4,3,2,5,None,"2026-08-19 10:00:00",2599,"报名","现金",1,"钢琴提高班报名费","已支付",None,None),
 (5,4,2,6,None,"2026-08-22 14:00:00",2599,"报名","刷卡",1,"钢琴提高班报名费","已支付",None,None),
 (6,1,3,7,None,"2026-08-18 15:00:00",1499,"报名","微信",1,"少儿舞蹈一班报名费","已支付",None,None),
 (7,5,3,8,None,"2026-08-18 15:30:00",1499,"报名","支付宝",1,"少儿舞蹈一班报名费","已支付",None,None),
 (8,5,4,10,None,"2026-08-17 16:00:00",1299,"报名","微信",1,"创意美术班报名费","已支付",None,None),
 (9,6,5,11,None,"2026-08-16 10:00:00",1399,"报名","现金",1,"声乐兴趣班报名费","已支付",None,None),
 (10,2,2,4,None,"2026-08-25 10:00:00",200,"续费","微信",1,"钢琴提高班课时续费","已支付",None,None),
 (11,1,None,None,1,"2026-08-17 09:00:00",200,"考级","微信",1,"钢琴考级报名费","已支付",None,None),
 (12,2,None,None,2,"2026-08-19 09:00:00",200,"考级","支付宝",1,"钢琴考级报名费","已支付",None,None),
 (13,5,3,8,None,"2026-08-21 10:00:00",1499,"报名","微信",1,"重复缴费，已退款","已退款","2026-08-23 10:00:00",1),
]
insert("payment",["payment_id","student_id","class_id","enrollment_id","reg_id","pay_date","amount","pay_type","pay_method","operator_id","remark","status","refund_time","refund_operator_id"],pays)
# 报名累计缴费回写
add("UPDATE enrollment e SET total_paid=(SELECT COALESCE(SUM(p.amount),0) FROM payment p WHERE p.enrollment_id=e.enrollment_id AND p.status='已支付');")

# ---------------- 试听预约 ----------------
vbs=[
 (1,"试听家长张","13800000018",1,1,"2026-09-05 10:00:00","想了解钢琴启蒙课","待确认","2026-08-27 09:00:00"),
 (2,"试听家长李","13800000019",3,2,"2026-09-06 14:30:00","舞蹈零基础试听","已确认","2026-08-26 09:00:00"),
 (3,"试听家长王","13800000020",4,3,"2026-09-03 15:00:00","喜欢画画，想试听美术课","已取消","2026-08-25 09:00:00"),
]
insert("visitor_booking",["booking_id","name","phone","class_id","teacher_id","preferred_time","remark","status","create_time"],vbs)

# ---------------- 学员预约 ----------------
apps=[
 (1,1,1,1,"一对一辅导","2026-09-02 16:00:00","待确认","课后答疑，请教手型问题"),
 (2,5,2,3,"补课","2026-09-01 15:00:00","已接受","补上一节请假的舞蹈课"),
]
insert("appointment",["appointment_id","student_id","teacher_id","class_id","appointment_type","appointment_time","status","remark"],apps)

# ---------------- 反馈 ----------------
fbs=[
 (1,1,"希望增加周末练琴时段","建议","2026-08-25 12:00:00","已回复","已反馈给教务，将统筹安排练琴时段","2026-08-26 09:00:00","系统管理员"),
 (2,3,"舞蹈教室把杆有点松动，麻烦检修","报修","2026-08-27 12:00:00","待处理",None,None,None),
 (3,5,"想咨询美术画材是否需要自备","咨询","2026-08-28 12:00:00","待处理",None,None,None),
]
insert("feedback",["feedback_id","student_id","content","type","create_time","status","reply_content","reply_time","reply_by"],fbs)

# ---------------- 学员申请 ----------------
reqs=[
 (1,1,1,1,"请假","2026-08-26","家中有事，申请请假一次","已批准","同意，注意回看课堂录像","2026-08-25 20:00:00","2026-08-25 21:00:00"),
 (2,3,1,2,"补考","2026-08-20","上次模拟测试未参加，申请补考","已批准","同意，安排在下周课后","2026-08-19 20:00:00","2026-08-20 09:00:00"),
 (3,4,1,2,"请假","2026-09-02","当天有其他安排，申请请假","待处理",None,"2026-08-28 20:00:00",None),
 (4,6,2,5,"休学","2026-09-01","身体原因申请休学一学期","待处理",None,"2026-08-28 21:00:00",None),
]
insert("student_request",["request_id","student_id","teacher_id","class_id","request_type","request_date","reason","status","reply_remark","create_time","handle_time"],reqs)

# ---------------- 学员评语 ----------------
cmts=[
 (1,2,1,1,"课堂专注，手型进步明显，继续保持。","2026-08-28 18:00:00"),
 (2,3,1,2,"节奏感好，注意高抬指的独立性练习。","2026-08-28 18:05:00"),
]
insert("student_comment",["comment_id","student_id","class_id","teacher_id","content","create_time"],cmts)

# ---------------- 调课申请（待审核/已通过/已拒绝） ----------------
rrs=[
 (1,SCHED[(1,"2026-08-31")],None,"2026-09-03","18:00:00","18:45:00","当天临时有会议，申请调到晚间","待审核",None,None,None,"2026-08-28 10:00:00"),
 (2,SCHED[(2,"2026-08-24")],None,"2026-08-23","09:00:00","09:45:00","配合教室调整提前一天上课","已通过","同意调课","2026-08-22 15:00:00",1,"2026-08-22 16:00:00"),
 (3,SCHED[(3,"2026-09-01")],None,"2026-09-08","10:00:00","11:00:00","申请顺延一周","已拒绝","当周课程进度紧张，不同意顺延",None,1,"2026-08-27 16:00:00"),
]
insert("reschedule_request",["request_id","schedule_id","target_schedule_id","new_date","new_start_time","new_end_time","reason","status","handle_remark","apply_time","handler_id","handle_time"],
 [(r[0],r[1],r[2],r[3],r[4],r[5],r[6],r[7],r[8],r[9],r[10],r[11]) for r in rrs])

# ---------------- 站内消息 ----------------
msgs=[
 (1,10,"作业","作业已批改","钢琴启蒙-手型练习已批改：90分","homework_submission",1,1,"2026-08-26 19:30:00"),
 (2,10,"考级","考级/比赛成绩已发布","中国音乐学院钢琴考级结果：通过（良好）","exam_registration",1,1,"2026-08-27 10:00:00"),
 (3,11,"考级","考级/比赛报名已通过","钢琴考级报名审核通过，请及时缴费","exam_registration",2,0,"2026-08-17 09:00:00"),
 (4,11,"作业","作业已批改","C大调音阶已批改：85分","homework_submission",3,1,"2026-08-25 19:00:00"),
 (5,2,"申请","有新的请假申请","小丽提交了一条请假申请，请处理","student_request",3,0,"2026-08-28 20:05:00"),
 (6,14,"作业","作业已批改","舞蹈基本功组合已批改：92分","homework_submission",6,1,"2026-08-27 11:30:00"),
 (7,10,"预约","您的预约待确认","一对一辅导预约等待老师确认","appointment",1,0,"2026-08-28 18:00:00"),
]
insert("message",["message_id","receiver_user_id","msg_type","title","content","biz_type","biz_id","is_read","create_time"],msgs)

# ---------------- 资讯 ----------------
news=[
 (1,"2026秋季学期开课通知","秋季学期已于8月下旬陆续开课，请各位学员按课表时间提前10分钟到校上课。","系统管理员","2026-08-20 09:00:00",128),
 (2,"国庆考级集训营报名开始","针对10月钢琴考级与舞蹈大赛，中心开设考前集训营，欢迎到前台咨询报名。","系统管理员","2026-08-26 09:00:00",56),
]
insert("news",["news_id","title","content","publisher","publish_time","view_count"],news)

# ---------------- 系统配置 ----------------
cfgs=[
 ("org_name","星海艺术培训中心"),("org_phone","0543-8888888"),("org_address","山东省滨州市黄河五路88号"),
 ("org_intro","星海艺术培训中心专注少儿艺术教育，开设乐器、舞蹈、美术、声乐课程，师资力量雄厚。"),
 ("logo_url",""),("banner_images",""),("org_wechat","xh-art2026"),
]
insert("system_config",["config_key","config_value"],cfgs)

sql="\n\n".join(stmts)
conn = pymysql.connect(
    host=os.environ.get("DB_HOST", "localhost"),
    port=int(os.environ.get("DB_PORT", "3306")),
    user=os.environ.get("DB_USERNAME", "root"),
    password=os.environ.get("DB_PASSWORD", ""),
    database=os.environ.get("DB_NAME", "art_training"),
    charset="utf8mb4",
    autocommit=True,
)
cur=conn.cursor()
for s in stmts:
    cur.execute(s)
print("seed executed, statements:",len(stmts))
conn.close()
out=os.path.join(os.path.dirname(os.path.abspath(__file__)), "seed_clean.sql")
open(out,"w",encoding="utf-8").write("-- 干净演示数据（基准日 2026-08-29，账号密码均为 123456，仅限本地演示）\nSET NAMES utf8mb4;\n\n"+sql+"\n")
print("sql written:",out)
