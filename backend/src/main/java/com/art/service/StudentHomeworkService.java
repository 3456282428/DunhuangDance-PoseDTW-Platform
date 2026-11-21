package com.art.service;

import com.art.common.PageResult;
import com.art.dto.SubmitHomeworkDTO;
import com.art.vo.StudentHomeworkVO;

/**
 * @author liukang&&heyan
 */
public interface StudentHomeworkService {
    PageResult<StudentHomeworkVO> pageMyHomeworks(Integer pageNum, Integer pageSize, String status);
    void submitHomework(Integer homeworkId, SubmitHomeworkDTO submitDTO);
    StudentHomeworkVO getHomeworkDetail(Integer homeworkId);
}
