package com.art.service;

import com.art.common.PageResult;
import com.art.vo.HomeworkVO;

/**
 * @author liukang&&heyan
 */
public interface HomeworkService {
    PageResult<HomeworkVO> pageHomeworks(Integer pageNum, Integer pageSize, Integer classId, String status);
    void delete(Integer homeworkId);
}
