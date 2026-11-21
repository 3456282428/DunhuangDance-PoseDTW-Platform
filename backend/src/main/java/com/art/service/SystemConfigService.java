package com.art.service;

import com.art.entity.Classroom;
import java.util.List;
import java.util.Map;

/**
 * @author liukang&&heyan
 */
public interface SystemConfigService {
    List<Classroom> listClassrooms();
    void addClassroom(Classroom classroom);
    void updateClassroom(Classroom classroom);
    void deleteClassroom(Integer classroomId);
    List<String> listSemesters();
    void addSemester(String semester);
    Map<String, Object> getTimeTemplate();
    void updateTimeTemplate(Map<String, Object> template);

    /** 读取通用配置（自动解析 JSON） */
    Object getConfig(String key);

    /** 保存通用配置（任意对象序列化为 JSON） */
    void saveConfig(String key, Object value);
}