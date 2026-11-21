package com.art.service;

import com.art.dto.ScheduleDTO;
import com.art.entity.CourseSchedule;
import com.art.vo.CourseScheduleVO;
import com.art.vo.RescheduleRequestVO;
import java.time.LocalDate;
import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface ScheduleService {
    List<CourseScheduleVO> listSchedules(LocalDate startDate, LocalDate endDate, Integer teacherId, Integer classroomId);
    int createSchedule(ScheduleDTO scheduleDTO);
    void updateSchedule(Integer scheduleId, ScheduleDTO scheduleDTO);
    void cancelSchedule(Integer scheduleId);
    CourseSchedule getById(Integer scheduleId);
    List<CourseSchedule> listByTeacher(Integer teacherId, LocalDate startDate, LocalDate endDate);

    /** 调课申请：管理端列表 / 通过（回写排课）/ 拒绝 */
    List<RescheduleRequestVO> listRescheduleRequests(String status);

    /** 管理端发起调课申请（进入待审核流程，审批通过后才回写排课），返回申请ID */
    Integer applyReschedule(Integer scheduleId, LocalDate newDate,
                            java.time.LocalTime newStartTime, java.time.LocalTime newEndTime, String reason);

    void approveReschedule(Integer requestId, String remark);
    void rejectReschedule(Integer requestId, String remark);
}
