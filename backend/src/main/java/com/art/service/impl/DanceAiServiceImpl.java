package com.art.service.impl;

import com.art.common.BusinessException;
import com.art.entity.Homework;
import com.art.entity.HomeworkSubmission;
import com.art.mapper.HomeworkMapper;
import com.art.mapper.HomeworkSubmissionMapper;
import com.art.service.DanceAiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 舞蹈AI智能评分服务实现：调用Python FastAPI评分服务（/api/score、/api/result/{taskId}）
  * @author liukang&&heyan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DanceAiServiceImpl implements DanceAiService {

    private static final List<String> VIDEO_EXTS = List.of(".mp4", ".mov", ".webm", ".avi", ".mkv", ".wmv");

    /** 单线程异步评分执行器（静态，应用关闭时释放） */
    private static final ExecutorService AI_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "dance-ai-worker");
        t.setDaemon(true);
        return t;
    });

    /** 评分中任务的阶段进度（submissionId -> 进度0-100），完成或失败后清理 */
    private static final Map<Integer, Integer> AI_PROGRESS = new ConcurrentHashMap<>();
    /** 评分中任务的当前阶段文案 */
    private static final Map<Integer, String> AI_STAGE = new ConcurrentHashMap<>();

    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;

    @Value("${dance.api.url:http://127.0.0.1:8000}")
    private String danceApiUrl;

    @Value("${dance.api.timeout-seconds:600}")
    private long danceApiTimeoutSeconds;

    /** 评分服务未启动时用于SSH拉起的远程目标（如 user@host），为空表示不自动拉起 */
    @Value("${dance.api.ssh-target:}")
    private String danceApiSshTarget;

    /** SSH拉起的启动命令，按部署环境自行配置，默认使用当前用户家目录下的脚本 */
    @Value("${dance.api.ssh-start-command:bash ~/dance/start_dance_api.sh}")
    private String danceApiSshStartCommand;

    @PreDestroy
    public void shutdown() {
        AI_EXECUTOR.shutdown();
        shutdownRemoteService();
    }

    /** 应用关闭时停止远程评分服务（仅当配置了ssh-target且服务在运行），保持服务器端口按需开闭 */
    private void shutdownRemoteService() {
        if (danceApiSshTarget == null || danceApiSshTarget.isBlank()) return;
        if (!isServiceUp()) return;
        log.info("应用关闭，通过SSH停止远程AI评分服务: {}", danceApiSshTarget);
        try {
            Process p = new ProcessBuilder("ssh", "-o", "BatchMode=yes", "-o", "ConnectTimeout=10",
                    danceApiSshTarget, "pkill -f api_server.py")
                    .redirectErrorStream(true).start();
            p.waitFor(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("停止远程AI评分服务失败: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> triggerScore(Integer submissionId, boolean force) {
        HomeworkSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) throw new BusinessException("作业提交记录不存在");
        // 留存复用：已有完成的评分结果且非强制时直接返回，不重复评分
        if (!force && "已完成".equals(submission.getAiStatus()) && submission.getAiScore() != null) {
            Map<String, Object> cached = new LinkedHashMap<>();
            cached.put("submissionId", submissionId);
            cached.put("aiStatus", "已完成");
            cached.put("aiScore", submission.getAiScore());
            cached.put("message", "已存在AI评分结果，直接复用");
            return cached;
        }
        Homework homework = homeworkMapper.selectById(submission.getHomeworkId());
        if (homework == null || homework.getRefVideoUrl() == null || homework.getRefVideoUrl().isBlank()) {
            throw new BusinessException("该作业未上传教师参考视频，无法AI评分");
        }
        String studentVideoUrl = findVideoUrl(submission.getAttachmentUrl());
        if (studentVideoUrl == null) throw new BusinessException("未找到学生练习视频附件");
        if ("评分中".equals(submission.getAiStatus())) {
            throw new BusinessException("AI评分进行中，请勿重复提交");
        }

        HomeworkSubmission update = new HomeworkSubmission();
        update.setSubmissionId(submissionId);
        update.setAiStatus("评分中");
        submissionMapper.updateAiFields(update);

        String teacherVideoUrl = homework.getRefVideoUrl();
        AI_EXECUTOR.submit(() -> executeScore(submissionId, teacherVideoUrl, studentVideoUrl));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("submissionId", submissionId);
        result.put("aiStatus", "评分中");
        result.put("message", "AI评分已提交，请稍后查询结果");
        return result;
    }

    @Override
    public Map<String, Object> getAiResult(Integer submissionId) {
        HomeworkSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) throw new BusinessException("作业提交记录不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("submissionId", submissionId);
        result.put("homeworkId", submission.getHomeworkId());
        result.put("aiScore", submission.getAiScore());
        result.put("aiReport", submission.getAiReport());
        result.put("aiStatus", submission.getAiStatus());
        result.put("aiScoredAt", submission.getAiScoredAt());
        // 评分中时透出阶段进度供前端进度条展示
        result.put("aiProgress", AI_PROGRESS.getOrDefault(submissionId, 0));
        result.put("aiStage", AI_STAGE.get(submissionId));
        // 报告页对比视频所需：教师参考视频 + 学生练习视频原始地址
        Homework homework = homeworkMapper.selectById(submission.getHomeworkId());
        result.put("refVideoUrl", homework != null ? homework.getRefVideoUrl() : null);
        result.put("studentVideoUrl", findVideoUrl(submission.getAttachmentUrl()));
        return result;
    }

    /**
     * 异步评分：定位本地视频 → 提交评分任务 → 轮询结果 → 落库
     */
    private void executeScore(Integer submissionId, String teacherVideoUrl, String studentVideoUrl) {
        try {
            File teacherFile = resolveLocalFile(teacherVideoUrl);
            File studentFile = resolveLocalFile(studentVideoUrl);
            if (teacherFile == null || !teacherFile.exists()) throw new BusinessException("教师参考视频文件不存在");
            if (studentFile == null || !studentFile.exists()) throw new BusinessException("学生练习视频文件不存在");

            // 冷启动阶段也上报进度，避免前端长时间停在0%
            AI_PROGRESS.put(submissionId, 2);
            AI_STAGE.put(submissionId, "正在连接评分服务…");
            ensureServiceUp();

            AI_PROGRESS.put(submissionId, 6);
            AI_STAGE.put(submissionId, "正在上传视频…");
            RestTemplate restTemplate = buildRestTemplate();
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("teacher_video", new FileSystemResource(teacherFile));
            body.add("student_video", new FileSystemResource(studentFile));
            body.add("generate_video", "true");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            @SuppressWarnings("unchecked")
            Map<String, Object> submitResp = restTemplate.postForObject(danceApiUrl + "/api/score",
                    new HttpEntity<>(body, headers), Map.class);
            String taskId = submitResp != null && submitResp.get("task_id") != null
                    ? submitResp.get("task_id").toString() : null;
            if (taskId == null || taskId.isBlank()) throw new BusinessException("AI评分服务未返回任务ID");
            AI_PROGRESS.put(submissionId, 10);
            AI_STAGE.put(submissionId, "评分任务已受理，模型加载中…");

            ObjectMapper objectMapper = new ObjectMapper();
            long deadline = System.currentTimeMillis() + danceApiTimeoutSeconds * 1000L;
            while (System.currentTimeMillis() < deadline) {
                Thread.sleep(3000);
                String respJson = restTemplate.getForObject(danceApiUrl + "/api/result/" + taskId, String.class);
                JsonNode node = objectMapper.readTree(respJson);
                String status = node.path("status").asText("");
                // 透传评分服务的阶段进度（供前端进度条展示）
                if (node.has("progress")) {
                    AI_PROGRESS.put(submissionId, node.path("progress").asInt(0));
                }
                if (node.has("stage") && !node.path("stage").isNull()) {
                    AI_STAGE.put(submissionId, node.path("stage").asText());
                }
                if ("completed".equals(status)) {
                    AI_PROGRESS.put(submissionId, 100);
                    AI_STAGE.put(submissionId, "评分完成");
                    // 下载骨架叠加预处理视频到本地uploads/ai/，随报告留存（失败不影响评分落库）
                    String teacherOverlay = downloadTaskFile(restTemplate, taskId, "teacher_overlay.mp4", submissionId, "teacher");
                    String studentOverlay = downloadTaskFile(restTemplate, taskId, "student_overlay.mp4", submissionId, "student");
                    // 下载师生并排合成对比视频（含骨架叠加与记分面板）
                    String comparison = downloadTaskFile(restTemplate, taskId, "comparison.mp4", submissionId, "comparison");
                    // 下载动作偏差最大帧对比截图，元数据随报告留存
                    ArrayNode worstFrames = objectMapper.createArrayNode();
                    for (JsonNode wf : node.path("result").path("worst_frames")) {
                        String fn = wf.path("filename").asText("");
                        if (fn.isBlank()) continue;
                        String url = downloadTaskFile(restTemplate, taskId, fn, submissionId,
                                "worst" + wf.path("rank").asInt());
                        if (url != null) {
                            ObjectNode o = wf.deepCopy();
                            o.put("url", url);
                            worstFrames.add(o);
                        }
                    }
                    // 下载逐帧偏差序列（前端实时可视化数据源）
                    String framesData = downloadTaskFile(restTemplate, taskId, "frames.json", submissionId, "frames");
                    // 下载DTW对齐可视化图与关节偏差热力图
                    String dtwVis = downloadTaskFile(restTemplate, taskId, "dtw_visualization.png", submissionId, "dtw");
                    String jointHeatmap = downloadTaskFile(restTemplate, taskId, "joint_heatmap.png", submissionId, "heatmap");
                    saveSuccess(submissionId, node.path("result"), teacherOverlay, studentOverlay, worstFrames, framesData, dtwVis, jointHeatmap, comparison);
                    return;
                }
                if ("failed".equals(status)) {
                    failScore(submissionId, node.path("error").asText("AI评分失败"));
                    return;
                }
            }
            failScore(submissionId, "AI评分超时");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failScore(submissionId, "AI评分被中断");
        } catch (Exception e) {
            failScore(submissionId, e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : "AI评分异常");
        }
    }

    /** completed：把result重组为camelCase JSON写入ai_report，附带预处理视频与偏差截图本地URL */
    private void saveSuccess(Integer submissionId, JsonNode result, String teacherOverlayUrl, String studentOverlayUrl,
                             ArrayNode worstFrames, String framesDataUrl, String dtwVisUrl, String jointHeatmapUrl,
                             String comparisonVideoUrl) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode report = objectMapper.createObjectNode();
            report.put("totalScore", result.path("total_score").asDouble());
            report.set("partScores", result.path("part_scores"));
            report.set("dimensionScores", result.path("dimension_scores"));
            report.set("feedback", result.path("feedback"));
            report.put("dtwDistance", result.path("dtw_distance").asDouble());
            report.put("rhythmCost", result.path("rhythm_cost").asDouble());
            report.put("processingTime", result.path("processing_time").asDouble());
            // 动作综合分析（总体评价/优点/不足/训练建议）
            JsonNode analysisNode = result.path("analysis");
            if (analysisNode.isObject()) report.set("analysis", analysisNode);
            // 节奏对齐分析（音乐节拍×动作节拍，独立维度）
            JsonNode rhythmNode = result.path("rhythm");
            if (rhythmNode.isObject()) report.set("rhythm", rhythmNode);
            if (teacherOverlayUrl != null) report.put("processedTeacherVideo", teacherOverlayUrl);
            if (studentOverlayUrl != null) report.put("processedStudentVideo", studentOverlayUrl);
            if (comparisonVideoUrl != null) report.put("comparisonVideo", comparisonVideoUrl);
            if (worstFrames != null && !worstFrames.isEmpty()) report.set("worstFrames", worstFrames);
            if (framesDataUrl != null) report.put("framesDataUrl", framesDataUrl);
            if (dtwVisUrl != null) report.put("dtwVisUrl", dtwVisUrl);
            if (jointHeatmapUrl != null) report.put("jointHeatmapUrl", jointHeatmapUrl);
            BigDecimal totalScore = BigDecimal.valueOf(result.path("total_score").asDouble())
                    .setScale(2, RoundingMode.HALF_UP);
            saveAiFields(submissionId, "已完成", totalScore, objectMapper.writeValueAsString(report));
        } catch (Exception e) {
            log.error("解析AI评分结果失败, submissionId={}", submissionId, e);
            failScore(submissionId, "AI评分结果解析失败");
        }
    }

    /**
     * 从评分服务下载任务产物到 backend/uploads/ai/，返回 /uploads/ai/ 访问URL；失败返回null
     */
    private String downloadTaskFile(RestTemplate restTemplate, String taskId, String filename,
                                    Integer submissionId, String role) {
        try {
            byte[] data = restTemplate.getForObject(
                    danceApiUrl + "/api/files/" + taskId + "/" + filename, byte[].class);
            if (data == null || data.length == 0) {
                log.warn("AI任务产物为空: taskId={}, file={}", taskId, filename);
                return null;
            }
            File dir = new File(System.getProperty("user.dir") + File.separator + "uploads"
                    + File.separator + "ai");
            if (!dir.exists() && !dir.mkdirs()) {
                log.warn("创建目录失败: {}", dir.getAbsolutePath());
                return null;
            }
            String ext = filename.substring(filename.lastIndexOf('.'));
            String storedName = "ai_" + submissionId + "_" + role + ext;
            Files.write(new File(dir, storedName).toPath(), data);
            log.info("AI任务产物已留存: {} ({} bytes)", storedName, data.length);
            return "/uploads/ai/" + storedName;
        } catch (Exception e) {
            log.warn("下载AI任务产物失败: taskId={}, file={}, 原因: {}", taskId, filename, e.getMessage());
            return null;
        }
    }

    /** failed/超时/异常统一失败落库，任何情况不向线程外抛出异常 */
    private void failScore(Integer submissionId, String reason) {
        String message = reason != null && !reason.isBlank() ? reason : "AI评分失败";
        log.error("舞蹈AI评分失败, submissionId={}, 原因: {}", submissionId, message);
        AI_PROGRESS.remove(submissionId);
        AI_STAGE.remove(submissionId);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode report = objectMapper.createObjectNode();
            report.put("error", message);
            saveAiFields(submissionId, "失败", null, objectMapper.writeValueAsString(report));
        } catch (Exception e) {
            saveAiFields(submissionId, "失败", null, "{\"error\":\"" + message.replace("\"", "'") + "\"}");
        }
    }

    private void saveAiFields(Integer submissionId, String aiStatus, BigDecimal aiScore, String aiReport) {
        try {
            HomeworkSubmission update = new HomeworkSubmission();
            update.setSubmissionId(submissionId);
            update.setAiStatus(aiStatus);
            update.setAiScore(aiScore);
            update.setAiReport(aiReport);
            update.setAiScoredAt(LocalDateTime.now());
            submissionMapper.updateAiFields(update);
        } catch (Exception e) {
            log.error("保存AI评分状态失败, submissionId={}", submissionId, e);
        }
    }

    /** 从逗号/分号分隔的附件URL中取第一个视频附件 */
    private String findVideoUrl(String attachmentUrl) {
        if (attachmentUrl == null || attachmentUrl.isBlank()) return null;
        for (String part : attachmentUrl.split("[,;]")) {
            String url = part.trim();
            if (url.isEmpty()) continue;
            String lower = url.toLowerCase();
            for (String ext : VIDEO_EXTS) {
                if (lower.endsWith(ext)) return url;
            }
        }
        return null;
    }

    /** /uploads/<文件名> → 工作目录/uploads/<文件名>（与FileController落盘规则一致） */
    private File resolveLocalFile(String url) {
        if (url == null || url.isBlank()) return null;
        String name = url.substring(url.lastIndexOf('/') + 1);
        int query = name.indexOf('?');
        if (query >= 0) name = name.substring(0, query);
        if (name.isBlank()) return null;
        return new File(System.getProperty("user.dir") + File.separator + "uploads", name);
    }

    /**
     * 按需启动评分服务：健康检查通过直接返回；否则配置了ssh-target时通过SSH执行服务器上的
     * start_dance_api.sh拉起服务，并轮询等待就绪（服务器无需开机自启/常驻）。
     */
    private void ensureServiceUp() throws InterruptedException {
        if (isServiceUp()) return;
        if (danceApiSshTarget == null || danceApiSshTarget.isBlank()) return; // 本机模式，由后续提交报错
        log.info("AI评分服务未响应，通过SSH拉起: {}", danceApiSshTarget);
        try {
            Process p = new ProcessBuilder("ssh", "-o", "BatchMode=yes", "-o", "ConnectTimeout=10",
                    danceApiSshTarget, danceApiSshStartCommand)
                    .redirectErrorStream(true).start();
            if (!p.waitFor(25, TimeUnit.SECONDS)) p.destroyForcibly();
        } catch (Exception e) {
            log.warn("SSH拉起AI评分服务失败: {}", e.getMessage());
        }
        long deadline = System.currentTimeMillis() + 40_000;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(2000);
            if (isServiceUp()) {
                log.info("AI评分服务已就绪: {}", danceApiUrl);
                return;
            }
        }
        throw new BusinessException("AI评分服务未启动且自动拉起失败，请检查服务器");
    }

    /** 评分服务健康检查（3秒超时） */
    private boolean isServiceUp() {
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(3000);
            factory.setReadTimeout(3000);
            new RestTemplate(factory).getForObject(danceApiUrl + "/api/health", String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10 * 1000);
        factory.setReadTimeout(60 * 1000);
        return new RestTemplate(factory);
    }
}
