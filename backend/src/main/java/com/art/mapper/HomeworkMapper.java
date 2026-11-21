package com.art.mapper;

import com.art.entity.Homework;
import com.art.vo.StudentHomeworkVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @author liukang&&heyan
 */
public interface HomeworkMapper {
    Homework selectById(@Param("homeworkId") Integer homeworkId);
    int insert(Homework homework);
    int update(Homework homework);
    int deleteById(@Param("homeworkId") Integer homeworkId);
    List<Homework> selectByClassId(@Param("classId") Integer classId);
    List<Homework> selectByConditions(@Param("classId") Integer classId,
                                      @Param("scheduleId") Integer scheduleId);
    /** 查询某学员在其所在班级的全部作业题目及其提交状态 */
    List<StudentHomeworkVO> selectStudentHomeworks(@Param("studentId") Integer studentId);
    /** 某班级作业题目总数 */
    int countByClassId(@Param("classId") Integer classId);
}
