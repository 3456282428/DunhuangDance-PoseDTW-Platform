package com.art.service.impl;

import com.art.common.BusinessException;
import com.art.entity.KbBackup;
import com.art.entity.KbDoc;
import com.art.entity.KbOperationLog;
import com.art.mapper.KbAdminMapper;
import com.art.mapper.KbBackupMapper;
import com.art.mapper.KbOperationLogMapper;
import com.art.mapper.KbStudentMapper;
import com.art.mapper.KbTeacherMapper;
import com.art.service.KnowledgeBackupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库备份实现：JSON快照落盘 + 恢复前安全备份
  * @author liukang&&heyan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBackupServiceImpl implements KnowledgeBackupService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String BACKUP_DIR = "kb_backup";

    private final KbAdminMapper adminMapper;
    private final KbTeacherMapper teacherMapper;
    private final KbStudentMapper studentMapper;
    private final KbBackupMapper backupMapper;
    private final KbOperationLogMapper opLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public KbBackup createBackup(String scope, String backupType, String operator) {
        checkBackupScope(scope);
        List<String> tables = tablesOf(scope);

        // 收集数据
        Map<String, List<KbDoc>> data = new LinkedHashMap<>();
        int count = 0;
        for (String table : tables) {
            List<KbDoc> rows = rowsOf(table);
            data.put(table, rows);
            count += rows.size();
        }

        // 写快照文件
        String fileName = "kb_backup_" + LocalDateTime.now().format(TS) + "_" + scope + ".json";
        String relativePath = BACKUP_DIR + File.separator + fileName;
        try {
            File dir = new File(System.getProperty("user.dir") + File.separator + "uploads" + File.separator + BACKUP_DIR);
            if (!dir.exists()) dir.mkdirs();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("scope", scope);
            payload.put("createTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            payload.put("tables", data);
            Files.write(new File(dir, fileName).toPath(),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload));
        } catch (Exception e) {
            throw new BusinessException("备份文件写入失败：" + e.getMessage());
        }

        KbBackup backup = new KbBackup();
        backup.setBackupName("知识库备份[" + scope + "] " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        backup.setBackupType(backupType);
        backup.setTableScope(scope);
        backup.setItemCount(count);
        backup.setBackupFile(relativePath);
        backup.setCreatedBy(operator);
        backup.setCreateTime(LocalDateTime.now());
        backupMapper.insert(backup);

        opLog(scope, null, "BACKUP", "创建知识库备份（范围" + scope + "，条目" + count + "）");
        return backup;
    }

    @Override
    public List<KbBackup> listBackups() {
        return backupMapper.selectAll();
    }

    @Override
    public Map<String, Object> restoreBackup(Integer backupId, String operator) {
        KbBackup backup = backupMapper.selectById(backupId);
        if (backup == null) throw new BusinessException("备份记录不存在");

        File file = new File(System.getProperty("user.dir") + File.separator + "uploads" + File.separator + backup.getBackupFile());
        if (!file.exists()) throw new BusinessException("备份文件已丢失：" + backup.getBackupFile());

        // 恢复前安全备份当前数据
        KbBackup safety = createBackup(backup.getTableScope(), "RESTORE", operator);

        Map<String, Object> result = new LinkedHashMap<>();
        int restored = 0;
        try {
            Map<?, ?> payload = objectMapper.readValue(file, Map.class);
            Map<?, ?> tables = (Map<?, ?>) payload.get("tables");
            for (String table : tablesOf(backup.getTableScope())) {
                List<?> rows = (List<?>) tables.get(table);
                if (rows == null) continue;
                // 清空后按快照重建
                deleteAllOf(table);
                for (Object row : rows) {
                    KbDoc doc = objectMapper.convertValue(row, KbDoc.class);
                    insertOf(table, doc);
                    restored++;
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("备份恢复失败：" + e.getMessage());
        }

        opLog(backup.getTableScope(), null, "RESTORE", "从备份【" + backup.getBackupName() + "】恢复" + restored + "条条目");
        result.put("restoredCount", restored);
        result.put("safetyBackupId", safety.getBackupId());
        return result;
    }

    private void checkBackupScope(String scope) {
        if (!SCOPE_ALL.equals(scope) && !"ADMIN".equals(scope) && !"TEACHER".equals(scope) && !"STUDENT".equals(scope)) {
            throw new BusinessException("非法的备份范围：" + scope);
        }
    }

    private List<String> tablesOf(String scope) {
        List<String> tables = new ArrayList<>();
        if ("ADMIN".equals(scope) || SCOPE_ALL.equals(scope)) tables.add("kb_admin_doc");
        if ("TEACHER".equals(scope) || SCOPE_ALL.equals(scope)) tables.add("kb_teacher_doc");
        if ("STUDENT".equals(scope) || SCOPE_ALL.equals(scope)) tables.add("kb_student_doc");
        return tables;
    }

    private List<KbDoc> rowsOf(String table) {
        return switch (table) {
            case "kb_admin_doc" -> adminMapper.selectPage(null, null, null);
            case "kb_teacher_doc" -> teacherMapper.selectPage(null, null, null, null);
            case "kb_student_doc" -> studentMapper.selectPage(null, null, null, null);
            default -> List.of();
        };
    }

    private void deleteAllOf(String table) {
        switch (table) {
            case "kb_admin_doc" -> adminMapper.deleteAll();
            case "kb_teacher_doc" -> teacherMapper.deleteAll();
            case "kb_student_doc" -> studentMapper.deleteAll();
        }
    }

    private void insertOf(String table, KbDoc doc) {
        switch (table) {
            case "kb_admin_doc" -> adminMapper.insert(doc);
            case "kb_teacher_doc" -> teacherMapper.insert(doc);
            case "kb_student_doc" -> studentMapper.insert(doc);
        }
    }

    private void opLog(String scope, Integer kbId, String opType, String detail) {
        KbOperationLog logEntry = new KbOperationLog();
        logEntry.setKbScope(scope);
        logEntry.setKbId(kbId);
        logEntry.setOpType(opType);
        logEntry.setOpUsername(operator() != null ? operator() : "SYSTEM");
        logEntry.setOpDetail(detail);
        logEntry.setOpTime(LocalDateTime.now());
        opLogMapper.insert(logEntry);
    }

    private String operator() {
        String username = com.art.util.SecurityUtil.getCurrentUsername();
        return username != null ? username : "SYSTEM";
    }
}
