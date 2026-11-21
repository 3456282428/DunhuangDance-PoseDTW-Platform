package com.art.service.impl;

import com.art.common.MobileValidator;
import com.art.entity.CourseClass;
import com.art.entity.CourseSchedule;
import com.art.entity.Enrollment;
import com.art.entity.Teacher;
import com.art.mapper.ClassroomMapper;
import com.art.mapper.CourseCategoryMapper;
import com.art.mapper.CourseClassMapper;
import com.art.mapper.CourseScheduleMapper;
import com.art.mapper.EnrollmentMapper;
import com.art.mapper.TeacherMapper;
import com.art.service.TeacherProfileService;
import com.art.util.SecurityUtil;
import com.art.vo.ClassVO;
import com.art.vo.TeacherProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class TeacherProfileServiceImpl implements TeacherProfileService {

    private final TeacherMapper teacherMapper;
    private final CourseClassMapper classMapper;
    private final CourseCategoryMapper categoryMapper;
    private final ClassroomMapper classroomMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CourseScheduleMapper scheduleMapper;

    @Override
    public TeacherProfileVO getMyProfile() {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        TeacherProfileVO vo = new TeacherProfileVO();
        if (teacherId == null) return vo;
        Teacher t = teacherMapper.selectById(teacherId);
        if (t == null) return vo;
        vo.setTeacherId(t.getTeacherId());
        vo.setTeacherNo(t.getTeacherNo());
        vo.setName(t.getName());
        vo.setGender(t.getGender());
        vo.setPhone(t.getPhone());
        vo.setMajor(t.getMajor());
        vo.setHireDate(t.getHireDate());
        vo.setStatus(t.getStatus());
        vo.setClassCount(getMyClasses().size());
        vo.setTotalHours(getTotalHours());
        return vo;
    }

    @Override
    public void updateProfile(TeacherProfileVO profileVO) {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        if (teacherId == null) return;
        Teacher t = teacherMapper.selectById(teacherId);
        if (t == null) return;
        if (profileVO.getPhone() != null) {
            MobileValidator.require(profileVO.getPhone(), "联系电话");
            t.setPhone(profileVO.getPhone());
        }
        if (profileVO.getGender() != null) t.setGender(profileVO.getGender());
        if (profileVO.getMajor() != null) t.setMajor(profileVO.getMajor());
        teacherMapper.update(t);
    }

    @Override
    public List<ClassVO> getMyClasses() {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        List<ClassVO> result = new ArrayList<>();
        if (teacherId == null) return result;
        for (CourseClass cc : classMapper.selectAll()) {
            if (!teacherId.equals(cc.getTeacherId())) continue;
            ClassVO vo = new ClassVO();
            vo.setClassId(cc.getClassId());
            vo.setClassName(cc.getClassName());
            vo.setCategoryName(categoryMapper.selectById(cc.getCategoryId()) != null
                    ? categoryMapper.selectById(cc.getCategoryId()).getCategoryName() : null);
            vo.setTeacherName(teacherMapper.selectById(cc.getTeacherId()) != null
                    ? teacherMapper.selectById(cc.getTeacherId()).getName() : null);
            vo.setClassroomName(classroomMapper.selectById(cc.getClassroomId()) != null
                    ? classroomMapper.selectById(cc.getClassroomId()).getClassroomName() : null);
            vo.setSemester(cc.getSemester());
            vo.setStartDate(cc.getStartDate());
            vo.setEndDate(cc.getEndDate());
            vo.setTotalHours(cc.getTotalHours());
            vo.setTuition(cc.getTuition());
            vo.setMaxStudents(cc.getMaxStudents());
            vo.setCurrentStudents(enrollmentMapper.selectByClassId(cc.getClassId()).size());
            vo.setStatus(cc.getStatus());
            result.add(vo);
        }
        return result;
    }

    @Override
    public Long getTotalHours() {
        Integer teacherId = SecurityUtil.getCurrentTeacherId();
        if (teacherId == null) return 0L;
        double sum = 0;
        for (CourseSchedule cs : scheduleMapper.selectByConditions(null, null, teacherId, null, null)) {
            if (cs.getHours() != null) sum += cs.getHours();
        }
        return (long) sum;
    }
}
