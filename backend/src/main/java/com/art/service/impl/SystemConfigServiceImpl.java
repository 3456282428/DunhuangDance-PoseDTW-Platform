package com.art.service.impl;

import com.art.entity.Classroom;
import com.art.common.BusinessException;
import com.art.entity.SystemConfig;
import com.art.mapper.ClassroomMapper;
import com.art.mapper.SystemConfigMapper;
import com.art.service.SystemConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * @author liukang&&heyan
 */
public class SystemConfigServiceImpl implements SystemConfigService {

    private final ClassroomMapper classroomMapper;
    private final SystemConfigMapper configMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<Classroom> listClassrooms() {
        return classroomMapper.selectAll();
    }

    @Override
    public void addClassroom(Classroom classroom) {
        classroomMapper.insert(classroom);
    }

    @Override
    public void updateClassroom(Classroom classroom) {
        classroomMapper.update(classroom);
    }

    @Override
    public void deleteClassroom(Integer classroomId) {
        classroomMapper.deleteById(classroomId);
    }

    @Override
    public List<String> listSemesters() {
        SystemConfig cfg = configMapper.selectByKey("semesters");
        if (cfg == null || cfg.getConfigValue() == null || cfg.getConfigValue().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(cfg.getConfigValue(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void addSemester(String semester) {
        List<String> list = new ArrayList<>(listSemesters());
        if (!list.contains(semester)) list.add(semester);
        SystemConfig cfg = new SystemConfig();
        cfg.setConfigKey("semesters");
        try {
            cfg.setConfigValue(objectMapper.writeValueAsString(list));
        } catch (Exception e) {
            cfg.setConfigValue("[]");
        }
        configMapper.upsert(cfg);
    }

    @Override
    public Map<String, Object> getTimeTemplate() {
        SystemConfig cfg = configMapper.selectByKey("time_template");
        if (cfg == null || cfg.getConfigValue() == null || cfg.getConfigValue().isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(cfg.getConfigValue(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    @Override
    public void updateTimeTemplate(Map<String, Object> template) {
        SystemConfig cfg = new SystemConfig();
        cfg.setConfigKey("time_template");
        try {
            cfg.setConfigValue(objectMapper.writeValueAsString(template));
        } catch (Exception e) {
            cfg.setConfigValue("{}");
        }
        configMapper.upsert(cfg);
    }

    private static final java.util.Set<String> ALLOWED_KEYS = java.util.Set.of(
            "organization", "semesters", "time_template", "payment", "notify", "portal");

    @Override
    public Object getConfig(String key) {
        SystemConfig cfg = configMapper.selectByKey(key);
        if (cfg == null || cfg.getConfigValue() == null || cfg.getConfigValue().isEmpty()) {
            return null;
        }
        String v = cfg.getConfigValue();
        try {
            return objectMapper.readValue(v, Object.class);
        } catch (Exception e) {
            return v;
        }
    }

    @Override
    public void saveConfig(String key, Object value) {
        if (!ALLOWED_KEYS.contains(key)) {
            throw new BusinessException("不允许的配置项：" + key);
        }
        SystemConfig cfg = new SystemConfig();
        cfg.setConfigKey(key);
        try {
            cfg.setConfigValue(objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            cfg.setConfigValue("{}");
        }
        configMapper.upsert(cfg);
    }
}
