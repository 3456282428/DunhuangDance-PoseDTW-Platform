package com.art.service.impl;

import com.art.entity.CourseClass;
import com.art.entity.CourseSchedule;
import com.art.entity.Homework;
import com.art.entity.HomeworkSubmission;
import com.art.mapper.ClassroomMapper;
import com.art.mapper.CourseCategoryMapper;
import com.art.mapper.CourseClassMapper;
import com.art.mapper.CourseScheduleMapper;
import com.art.mapper.EnrollmentMapper;
import com.art.mapper.HomeworkMapper;
import com.art.mapper.HomeworkSubmissionMapper;
import com.art.mapper.TeacherMapper;
import com.art.service.TeacherDashboardService;
import com.art.util.SecurityUtil;
import com.art.vo.CourseScheduleVO;
import com.art.vo.TodoItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class TeacherDashboardServiceImpl implements TeacherDashboardService {

    private final CourseScheduleMapper scheduleMapper;
    private final CourseClassMapper classMapper;
    private final TeacherMapper teacherMapper;
    private final ClassroomMapper classroomMapper;
    private final CourseCategoryMapper categoryMapper;
    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final EnrollmentMapper enrollmentMapper;

    @Override
    public Map<String, Object> getDashboardSummary() {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        Map<String, Object> result = new LinkedHashMap<>();
        int todayCount = 0;
        for (CourseSchedule cs : scheduleMapper.selectByConditions(null, null, teacherId, null, null)) {
            if (cs.getCourseDate() != null && cs.getCourseDate().equals(LocalDate.now())) todayCount++;
        }
        result.put("todayCourseCount", todayCount);
        result.put("classCount", (int) classMapper.selectAll().stream()
                .filter(cc -> teacherId != null && teacherId.equals(cc.getTeacherId())).count());
        result.put("todaySchedule", getTodaySchedule());
        result.put("todoCount", getTodos().size());
        result.put("totalHours", scheduleMapper.selectByConditions(null, null, teacherId, null, null)
                .stream().mapToDouble(cs -> cs.getHours() == null ? 0 : cs.getHours()).sum());

        // 作业相关统计（与作业管理模块同步）
        java.util.Set<Integer> myClassIds = new java.util.HashSet<>();
        for (CourseClass cc : classMapper.selectAll()) {
            if (teacherId != null && teacherId.equals(cc.getTeacherId())) myClassIds.add(cc.getClassId());
        }
        int myHomeworkCount = 0;
        int pendingGrade = 0;
        int gradedCount = 0;
        for (Integer cid : myClassIds) {
            for (Homework h : homeworkMapper.selectByConditions(cid, null)) {
                myHomeworkCount++;
                for (HomeworkSubmission s : submissionMapper.selectByHomeworkId(h.getHomeworkId())) {
                    if ("已提交".equals(s.getStatus())) pendingGrade++;
                    else if ("已批改".equals(s.getStatus())) gradedCount++;
                }
            }
        }
        result.put("homeworkCount", myHomeworkCount);
        result.put("pendingGradeCount", pendingGrade);
        result.put("gradedCount", gradedCount);
        return result;
    }

    @Override
    public List<CourseScheduleVO> getTodaySchedule() {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        List<CourseScheduleVO> result = new ArrayList<>();
        if (teacherId == null) return result;
        for (CourseSchedule cs : scheduleMapper.selectByConditions(null, null, teacherId, null, null)) {
            if (cs.getCourseDate() == null || !cs.getCourseDate().equals(LocalDate.now())) continue;
            result.add(toVO(cs));
        }
        return result;
    }

    @Override
    public List<TodoItemVO> getTodos() {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        List<TodoItemVO> result = new ArrayList<>();
        if (teacherId == null) return result;
        for (HomeworkSubmission sub : submissionMapper.selectByStatus("已提交")) {
            Homework h = homeworkMapper.selectById(sub.getHomeworkId());
            if (h == null) continue;
            CourseClass cc = h.getClassId() != null ? classMapper.selectById(h.getClassId()) : null;
            if (cc == null || !teacherId.equals(cc.getTeacherId())) continue;
            TodoItemVO todo = new TodoItemVO();
            todo.setId(sub.getSubmissionId());
            todo.setType("作业批改");
            todo.setTitle(h.getTitle());
            todo.setDescription("待批改的作业");
            todo.setStatus("待处理");
            todo.setCreateTime(sub.getSubmitTime() != null ? sub.getSubmitTime().toString() : null);
            result.add(todo);
        }
        return result;
    }

    private CourseScheduleVO toVO(CourseSchedule cs) {
        CourseScheduleVO vo = new CourseScheduleVO();
        vo.setScheduleId(cs.getScheduleId());
        vo.setClassId(cs.getClassId());
        vo.setTeacherId(cs.getTeacherId());
        vo.setClassroomId(cs.getClassroomId());
        vo.setCourseDate(cs.getCourseDate());
        vo.setStartTime(cs.getStartTime());
        vo.setEndTime(cs.getEndTime());
        vo.setHours(cs.getHours());
        vo.setStatus(cs.getStatus());
        vo.setRemark(cs.getRemark());
        CourseClass cc = cs.getClassId() != null ? classMapper.selectById(cs.getClassId()) : null;
        if (cc != null) {
            vo.setClassName(cc.getClassName());
            vo.setCategoryName(cc.getCategoryId() != null && categoryMapper.selectById(cc.getCategoryId()) != null
                    ? categoryMapper.selectById(cc.getCategoryId()).getCategoryName() : null);
            vo.setTeacherName(teacherMapper.selectById(cc.getTeacherId()) != null
                    ? teacherMapper.selectById(cc.getTeacherId()).getName() : null);
        }
        vo.setClassroomName(cs.getClassroomId() != null && classroomMapper.selectById(cs.getClassroomId()) != null
                ? classroomMapper.selectById(cs.getClassroomId()).getClassroomName() : null);
        return vo;
    }
}
