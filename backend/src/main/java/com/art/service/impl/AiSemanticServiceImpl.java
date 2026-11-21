package com.art.service.impl;

import com.art.common.BusinessException;
import com.art.entity.AiInvokeLog;
import com.art.entity.AiModel;
import com.art.entity.Homework;
import com.art.entity.HomeworkSubmission;
import com.art.entity.KbDoc;
import com.art.mapper.AiInvokeLogMapper;
import com.art.mapper.AiModelMapper;
import com.art.mapper.HomeworkMapper;
import com.art.mapper.HomeworkSubmissionMapper;
import com.art.service.AiSemanticService;
import com.art.service.KnowledgeService;
import com.art.util.SecurityUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 豆包语义增强服务实现（通道二）：
 * 流程：缓存检查 → 归属校验 → 关键帧抽取（ffmpeg，每视频6帧）→ KB优先检索 →
 *      组装量化上下文Prompt → 豆包/responses多模态理解 → 解析JSON → 落库ai_report.semantic
 * 通道一量化数据（部位分/维度分/DTW/节奏）作为Prompt上下文，模型只做语义解释不做打分。
  * @author liukang&&heyan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSemanticServiceImpl implements AiSemanticService {

    /** 每个视频抽取的关键帧数 */
    private static final int FRAMES_PER_VIDEO = 6;
    private static final String[] VIDEO_EXTS = {".mp4", ".avi", ".mov", ".mkv", ".webm"};
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HomeworkSubmissionMapper submissionMapper;
    private final HomeworkMapper homeworkMapper;
    private final AiModelMapper modelMapper;
    private final AiInvokeLogMapper invokeLogMapper;
    private final KnowledgeService knowledgeService;
    private final DoubaoVisionClient doubaoVisionClient;

    @Override
    public Map<String, Object> enhanceSemantics(Integer submissionId, String callerScope, boolean force) {
        Map<String, Object> result = new LinkedHashMap<>();
        HomeworkSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) throw new BusinessException("提交记录不存在");
        checkOwnership(callerScope, submission);

        // 缓存命中：已分析且未强制刷新
        JsonNode existing = parseReport(submission.getAiReport()).path("semantic");
        if (!force && existing.isObject() && existing.path("available").asBoolean(false)) {
            result.put("status", "SUCCESS");
            result.put("cached", true);
            result.put("semantic", existing.deepCopy());
            return result;
        }

        Homework homework = homeworkMapper.selectById(submission.getHomeworkId());
        if (homework == null) throw new BusinessException("作业不存在");
        JsonNode report = parseReport(submission.getAiReport());
        if (!report.isObject() || !report.has("partScores")) {
            throw new BusinessException("请先完成AI评分，再生成AI教练点评");
        }

        // 1. 定位豆包视觉理解模型
        AiModel model = pickVisionModel();
        if (model == null) {
            throw new BusinessException("豆包语义模型未注册（请执行migrate_doubao_semantic.sql）");
        }

        // 2. KB优先检索（AI调用约定：优先使用知识库已有数据作为回答依据）
        Integer ownerId = resolveOwnerId(callerScope);
        List<KbDoc> kbHits = new ArrayList<>(knowledgeService.searchForAi(
                callerScope, ownerId, homework.getTitle() + " 动作要领 教学要点", null, 3));

        // 全量留痕
        AiInvokeLog invokeLog = new AiInvokeLog();
        invokeLog.setCallerScope(callerScope);
        invokeLog.setCallerUserId(SecurityUtil.getCurrentUserId());
        invokeLog.setQuestion("[语义增强] 提交#" + submissionId + " " + homework.getTitle());
        invokeLog.setKbScope(callerScope);
        invokeLog.setHitCount(kbHits.size());
        invokeLog.setInvokeStatus("PENDING");
        invokeLog.setInvokeTime(LocalDateTime.now());
        invokeLogMapper.insert(invokeLog);
        result.put("invokeId", invokeLog.getInvokeId());

        try {
            // 3. 关键帧抽取
            File teacherVideo = resolveLocalFile(homework.getRefVideoUrl());
            File studentVideo = resolveLocalFile(findVideoUrl(submission.getAttachmentUrl()));
            if (teacherVideo == null || !teacherVideo.exists()
                    || studentVideo == null || !studentVideo.exists()) {
                throw new BusinessException("教师或学生视频文件缺失，无法进行视觉语义分析");
            }
            List<String> frames = new ArrayList<>();
            File tmpDir = Files.createTempDirectory("ai_semantic").toFile();
            try {
                extractFrames(teacherVideo, "t", frames);
                extractFrames(studentVideo, "s", frames);
            } finally {
                cleanup(tmpDir);
            }
            if (frames.isEmpty()) throw new BusinessException("关键帧抽取失败");

            // 4. 组装Prompt（量化上下文 + KB要点 + 输出规范）并调用
            String prompt = buildPrompt(homework.getTitle(), report, kbHits);
            String answer = doubaoVisionClient.analyze(model, frames, prompt);
            if (answer.length() > 4000) answer = answer.substring(0, 4000);

            // 5. 解析结果并落库ai_report.semantic（评分字段原值保值）
            JsonNode semantic = parseSemantic(answer, model.getCurrentVersion());
            ObjectNode merged = report.deepCopy();
            merged.set("semantic", semantic);
            HomeworkSubmission update = new HomeworkSubmission();
            update.setSubmissionId(submissionId);
            update.setAiStatus(submission.getAiStatus());
            update.setAiScore(submission.getAiScore());
            update.setAiScoredAt(submission.getAiScoredAt());
            update.setAiReport(merged.toString());
            submissionMapper.updateAiFields(update);

            invokeLog.setInvokeStatus("SUCCESS");
            invokeLog.setAnswer(answer);
            invokeLogMapper.updateResult(invokeLog);
            result.put("status", "SUCCESS");
            result.put("cached", false);
            result.put("semantic", semantic);
        } catch (Exception e) {
            String err = e.getMessage() == null ? "未知错误" : e.getMessage();
            if (err.length() > 500) err = err.substring(0, 500);
            log.warn("AI教练语义点评失败, submissionId={}: {}", submissionId, err);
            invokeLog.setInvokeStatus("FAILED");
            invokeLog.setErrorMsg(err);
            invokeLogMapper.updateResult(invokeLog);
            result.put("status", "FAILED");
            result.put("message", err);
        }
        return result;
    }

    /** 端别归属校验：学员仅能分析本人提交 */
    private void checkOwnership(String callerScope, HomeworkSubmission submission) {
        if (KnowledgeService.SCOPE_STUDENT.equals(callerScope)
                && !submission.getStudentId().equals(SecurityUtil.getCurrentStudentId())) {
            throw new BusinessException("无权访问他人提交记录");
        }
    }

    /** 选取豆包视觉理解模型：provider含DOUBAO且model_type=VISION且ENABLED */
    private AiModel pickVisionModel() {
        return modelMapper.selectAll().stream()
                .filter(m -> "VISION".equalsIgnoreCase(m.getModelType()))
                .filter(m -> m.getProvider() != null && m.getProvider().toUpperCase().contains("DOUBAO"))
                .filter(m -> "ENABLED".equals(m.getStatus()))
                .filter(m -> m.getApiKey() != null && !m.getApiKey().isBlank())
                .findFirst().orElse(null);
    }

    /** 组装多模态Prompt：教师关键帧在前、学生关键帧在后（与抽取顺序一致） */
    private String buildPrompt(String title, JsonNode report, List<KbDoc> kbHits) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业舞蹈教学教练。用户将按顺序提供：前6张为【教师示范】视频关键帧，后6张为【学生练习】视频关键帧。")
          .append("结合以下量化评分数据（由姿态估计+DTW时序对齐流水线精确计算，你负责语义解释，不要重新打分）：\n");
        sb.append("作业：").append(title).append("\n");
        sb.append("总分：").append(report.path("totalScore").asText("未知")).append("；");
        sb.append("部位得分：").append(compact(report.path("partScores"))).append("；");
        sb.append("维度得分：").append(compact(report.path("dimensionScores"))).append("；");
        sb.append("DTW距离：").append(report.path("dtwDistance").asText("未知")).append("\n");
        JsonNode rhythm = report.path("rhythm");
        if (rhythm.isObject() && rhythm.path("available").asBoolean(false)) {
            sb.append("节奏对齐：音乐").append(rhythm.path("music_bpm").asText()).append("BPM，动作")
              .append(rhythm.path("motion_bpm").asText()).append("BPM，节拍命中率")
              .append(rhythm.path("beat_hit_rate").asDouble(0) * 100).append("%，平均踩拍偏差")
              .append(rhythm.path("avg_offset_ms").asText()).append("ms\n");
        }
        if (!kbHits.isEmpty()) {
            sb.append("教学要点参考（知识库）：");
            for (KbDoc doc : kbHits) {
                String s = doc.getSummary() == null ? "" : doc.getSummary();
                if (s.length() > 60) s = s.substring(0, 60);
                sb.append("「").append(doc.getTitle() == null ? "" : doc.getTitle()).append("：").append(s).append("」 ");
            }
            sb.append("\n");
        }
        sb.append("请严格只输出如下JSON（不要markdown代码块、不要多余文字）：\n")
          .append("{\"teacherAnalysis\":\"教师示范动作解析：动作结构与发力点标注，120字内\",")
          .append("\"studentAnalysis\":\"学生动作解析：对照帧可见的具体问题，120字内\",")
          .append("\"rhythmConsistency\":\"节奏与音乐的视听一致性判断，80字内\",")
          .append("\"suggestions\":[{\"title\":\"建议标题\",\"detail\":\"具体改进方法，60字内\"}]}")
          .append("，suggestions给出3条针对学生最需改进之处的建议。");
        return sb.toString();
    }

    /** 宽松解析模型输出：剥离代码围栏后取首个JSON对象；失败则降级rawText */
    private JsonNode parseSemantic(String answer, String modelName) {
        ObjectMapper om = new ObjectMapper();
        ObjectNode out = om.createObjectNode();
        out.put("available", true);
        out.put("analyzedAt", LocalDateTime.now().format(TS_FMT));
        out.put("model", modelName);
        try {
            String s = answer.trim().replace("```json", "").replace("```", "");
            int start = s.indexOf('{'), end = s.lastIndexOf('}');
            JsonNode parsed = om.readTree(s.substring(start, end + 1));
            out.set("teacherAnalysis", parsed.path("teacherAnalysis"));
            out.set("studentAnalysis", parsed.path("studentAnalysis"));
            out.set("rhythmConsistency", parsed.path("rhythmConsistency"));
            out.set("suggestions", parsed.path("suggestions").isArray()
                    ? parsed.path("suggestions") : om.createArrayNode());
        } catch (Exception e) {
            out.put("rawText", answer);
        }
        return out;
    }

    /** 量化对象紧凑化：{肩部: 82.5, ...} */
    private String compact(JsonNode node) {
        if (!node.isObject()) return "未知";
        StringBuilder sb = new StringBuilder();
        node.fields().forEachRemaining(e -> {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getKey()).append(":").append(e.getValue().asText());
        });
        return sb.toString();
    }

    /** 用ffprobe取视频时长（秒），失败返回-1 */
    private double probeDuration(File video) {
        try {
            Process p = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries",
                    "format=duration", "-of", "csv=p=0", video.getAbsolutePath())
                    .redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor(10, TimeUnit.SECONDS);
            return Double.parseDouble(out.split("\\s+")[0]);
        } catch (Exception e) {
            return -1;
        }
    }

    /** 每视频均匀抽FRAMES_PER_VIDEO帧（避开首尾15%），base64 data URL加入frames */
    private void extractFrames(File video, String tag, List<String> frames) {
        double dur = probeDuration(video);
        if (dur <= 0) return;
        File tmp = new File(System.getProperty("java.io.tmpdir"), "ai_semantic_" + tag
                + "_" + System.nanoTime());
        if (!tmp.mkdirs()) return;
        try {
            for (int i = 0; i < FRAMES_PER_VIDEO; i++) {
                double t = dur * (0.15 + 0.75 * i / (FRAMES_PER_VIDEO - 1.0));
                File jpg = new File(tmp, tag + "_" + i + ".jpg");
                // ffmpeg日志量大，必须丢弃输出避免管道缓冲撑满导致进程阻塞
                Process p = new ProcessBuilder("ffmpeg", "-y", "-ss", String.format("%.2f", t),
                        "-i", video.getAbsolutePath(), "-frames:v", "1",
                        "-vf", "scale=640:-2", "-q:v", "4", jpg.getAbsolutePath())
                        .redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .start();
                if (!p.waitFor(20, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                    continue;
                }
                if (jpg.exists() && jpg.length() > 1024) {
                    String b64 = Base64.getEncoder().encodeToString(Files.readAllBytes(jpg.toPath()));
                    frames.add("data:image/jpeg;base64," + b64);
                }
            }
        } catch (Exception e) {
            log.warn("关键帧抽取异常({}): {}", tag, e.getMessage());
        } finally {
            cleanup(tmp);
        }
    }

    private void cleanup(File dir) {
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) f.delete();
        dir.delete();
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

    private JsonNode parseReport(String aiReport) {
        try {
            if (aiReport == null || aiReport.isBlank()) return new ObjectMapper().createObjectNode();
            return new ObjectMapper().readTree(aiReport);
        } catch (Exception e) {
            return new ObjectMapper().createObjectNode();
        }
    }

    /** 教师/学员端KB检索需限定本人可见范围（与统一调用入口一致） */
    private Integer resolveOwnerId(String callerScope) {
        if (KnowledgeService.SCOPE_TEACHER.equals(callerScope)) return SecurityUtil.getCurrentTeacherId();
        if (KnowledgeService.SCOPE_STUDENT.equals(callerScope)) return SecurityUtil.getCurrentStudentId();
        return null;
    }
}
