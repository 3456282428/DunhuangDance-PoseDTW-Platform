package com.art.controller.teacher;

import com.art.common.Result;
import com.art.service.TeacherStudentProgressService;
import com.art.vo.StudentProgressDetailVO;
import com.art.vo.StudentProgressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师端 - 学员进度跟踪
 * 查看学员出勤、作业、考级记录，评估学习进度
  * @author liukang&&heyan
 */
@RestController
@RequestMapping("/api/teacher/student-progress")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherStudentController {

    private final TeacherStudentProgressService progressService;

    /**
     * 获取某个班级所有学员的简要进度
     */
    @GetMapping("/class/{classId}")
    public Result<List<StudentProgressVO>> getClassStudentsProgress(@PathVariable Integer classId) {
        return Result.success(progressService.getClassStudentsProgress(classId));
    }

    /**
     * 获取某个学员的详细学习进度
     */
    @GetMapping("/student/{studentId}")
    public Result<StudentProgressDetailVO> getStudentProgressDetail(@PathVariable Integer studentId,
                                                                    @RequestParam(required = false) Integer classId) {
        return Result.success(progressService.getStudentProgressDetail(studentId, classId));
    }

    /**
     * 添加教师评语/学习建议
     */
    @PostMapping("/student/{studentId}/comment")
    public Result<Void> addProgressComment(@PathVariable Integer studentId,
                                           @RequestParam Integer classId,
                                           @RequestParam String comment) {
        progressService.addProgressComment(studentId, classId, comment);
        return Result.success();
    }
}