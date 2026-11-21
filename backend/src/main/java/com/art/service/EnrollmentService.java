package com.art.service;

import com.art.common.PageResult;
import com.art.dto.EnrollmentDTO;
import com.art.entity.Enrollment;

/**
 * @author liukang&&heyan
 */
public interface EnrollmentService {
    PageResult<Enrollment> pageEnrollments(Integer pageNum, Integer pageSize, String studentName, Integer classId, String status);
    void createEnrollment(EnrollmentDTO enrollmentDTO);
    void updateStatus(Integer enrollmentId, String status);
    void delete(Integer enrollmentId);

    /** 按考勤明细与缴费流水重算全部报名的冗余计数，返回修正条数 */
    int recalcAllStats();
}
