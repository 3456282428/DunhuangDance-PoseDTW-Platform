package com.art.service;

import com.art.dto.StudentRequestDTO;

import java.util.List;

/**
 * @author liukang&&heyan
 */
public interface StudentRequestService {
    void submit(StudentRequestDTO dto);
    List<StudentRequestDTO> listMine();
    List<StudentRequestDTO> listForTeacher();
    void handle(Integer requestId, String action, String remark);
}
