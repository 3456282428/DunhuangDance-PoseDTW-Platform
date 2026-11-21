package com.art.service;

import com.art.entity.ExamProject;
import com.art.entity.ExamRegistration;
import java.util.List;
import java.util.Map;

/**
 * @author liukang&&heyan
 */
public interface ExamModuleService {
    List<Map<String,Object>> listProjects(String activityType, String category, String status, String keyword);
    void createProject(ExamProject p);
    void updateProject(ExamProject p);
    void deleteProject(Integer projectId);

    List<Map<String,Object>> listRegistrations(Integer projectId, Integer studentId, String status, String activityType);
    void register(ExamRegistration form);
    Map<String,Object> lookupStudent(String studentNo);
    void recommend(String studentNo, Integer classId, Integer projectId, String comment);
    void audit(Integer regId, Boolean pass, String remark);
    void pay(Integer regId, String payMethod);
    void arrange(Integer regId);
    void enterResult(Integer regId, String result, String award, String certificateNo);
    void cancel(Integer regId);
    Map<String,Object> stats();
}
