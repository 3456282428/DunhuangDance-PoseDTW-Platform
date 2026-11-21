package com.art.search;

import com.art.entity.KbDoc;
import com.art.entity.KbEntryModel;
import com.art.entity.KbModelResource;
import com.art.entity.KbRelation;
import com.art.mapper.KbAdminMapper;
import com.art.mapper.KbEntryModelMapper;
import com.art.mapper.KbModelResourceMapper;
import com.art.mapper.KbRelationMapper;
import com.art.mapper.KbStudentMapper;
import com.art.mapper.KbTeacherMapper;
import com.art.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识条目 → 3D模型 推荐引擎（规则可追溯，结果随知识库内容动态变化）。
 * <p>
 * 推荐规则（每条目独立评分，取最高分模型）：
 * <ul>
 *   <li>标签命中：模型适配标签命中条目标题/分类/标签/摘要/正文头部，每命中 +0.25，上限 0.60；</li>
 *   <li>关联上下文加成：条目业务引用经 kb_relation 关联到教室（IN_ROOM）→ 场景类模型 +0.20；
 *       关联到考级项目（OF_PROJECT）→ 道具类模型 +0.15；</li>
 *   <li>类目加成：条目属于教室资源/排课课表/班级档案等场景类目 → 场景类模型 +0.10；</li>
 *   <li>类别默认兜底：以上均未命中时，给出端别通用场景模型（0.30），保证每条目都有适配推荐。</li>
 * </ul>
 * 变量性：文档新增/更新/删除、全量归集、关系重建后都会触发 {@link #rebuildScope(String)} 重算，
 * 推荐结果不是常量，而是知识库内容的函数。
 * @author liukang&&heyan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbModelRecommendService {

    /** 业务引用规范形态：前缀:数字（去掉 :t{id} / :s{id} 端别后缀） */
    private static final Pattern REF_PATTERN = Pattern.compile("^([a-z_]+:\\d+)");

    /** 正文参与匹配的截断长度（标题/摘要/标签已覆盖主要语义） */
    private static final int CONTENT_LIMIT = 400;
    /** 单条目推荐批量写入行数 */
    private static final int UPSERT_CHUNK = 200;
    /** 命中标签在 matchTags 中的最大保留个数 */
    private static final int MAX_MATCH_TAGS = 5;

    /** 模型类别 → 中文描述 */
    private static final Map<String, String> CATEGORY_TEXT = Map.of(
            "DANCE", "舞蹈", "MUSIC", "音乐", "ART", "美术",
            "CALLIGRAPHY", "书法", "SCENE", "场景", "PROP", "道具");

    /** 场景类目：属于这些分类的条目，场景类模型获得类目加成 */
    private static final Set<String> SCENE_CATEGORIES = Set.of("教室资源", "排课课表", "班级档案");

    /** 兜底模型名称（无任何命中时给出的通用适配模型） */
    private static final String FALLBACK_MODEL_NAME = "舞台剧场场景";

    private final KbModelResourceMapper modelMapper;
    private final KbEntryModelMapper entryModelMapper;
    private final KbRelationMapper relationMapper;
    private final KbAdminMapper adminMapper;
    private final KbTeacherMapper teacherMapper;
    private final KbStudentMapper studentMapper;

    /* ==================== 重建（变量性保证） ==================== */

    /** 三端全量重算 */
    public synchronized Map<String, Object> rebuildAll() {
        Map<String, Object> stats = new LinkedHashMap<>();
        for (String scope : List.of(KnowledgeService.SCOPE_ADMIN, KnowledgeService.SCOPE_TEACHER, KnowledgeService.SCOPE_STUDENT)) {
            try {
                stats.put(scope, rebuildScope(scope));
            } catch (Exception e) {
                log.warn("[模型推荐] {} 端重算失败：{}", scope, e.getMessage());
                stats.put(scope, Map.of("error", String.valueOf(e.getMessage())));
            }
        }
        log.info("[模型推荐] 三端重算完成：{}", stats);
        return stats;
    }

    /**
     * 单端重算：为该端每条启用条目重新计算适配模型并落库，清理已失效条目的推荐。
     * 供全量归集、文档增删改、关系重建后调用（保证推荐随内容变化）。
     */
    public synchronized Map<String, Object> rebuildScope(String scope) {
        long start = System.currentTimeMillis();
        List<KbModelResource> models = modelMapper.selectEnabled();
        List<KbDoc> docs = selectActive(scope);
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("docs", docs.size());
        if (models.isEmpty()) {
            stat.put("matched", 0);
            return stat;
        }
        // 关联上下文：一次装载全部关系，构建 ref → 关联对端前缀集合
        Map<String, Set<String>> peerPrefixes = loadPeerPrefixes();

        List<KbEntryModel> rows = new ArrayList<>(docs.size());
        List<Integer> aliveIds = new ArrayList<>(docs.size());
        int matched = 0, fallback = 0;
        for (KbDoc d : docs) {
            KbEntryModel row = scoreOne(scope, d, models, peerPrefixes);
            if (row == null) continue;
            rows.add(row);
            aliveIds.add(d.getKbId());
            if (row.getMatchRule() != null && row.getMatchRule().contains("兜底")) fallback++;
            else matched++;
        }
        if (aliveIds.isEmpty()) {
            entryModelMapper.deleteByScope(scope);
        } else {
            entryModelMapper.deleteByScopeNotIn(scope, aliveIds);
            for (int i = 0; i < rows.size(); i += UPSERT_CHUNK) {
                entryModelMapper.upsertBatch(rows.subList(i, Math.min(rows.size(), i + UPSERT_CHUNK)));
            }
        }
        stat.put("matched", matched);
        stat.put("fallback", fallback);
        stat.put("costMs", System.currentTimeMillis() - start);
        return stat;
    }

    /* ==================== 单条目推荐查询（API 输出） ==================== */

    /**
     * 查询条目的适配3D模型推荐：无落库记录时即时重算并落库（懒刷新）。
     * 输出结构化结果：模型信息 + 匹配分 + 匹配规则（可追溯）+ 更新时间 + 动态更新说明。
     */
    public Map<String, Object> recommendationOf(String scope, Integer kbId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scope", scope);
        out.put("kbId", kbId);
        KbDoc doc = selectById(scope, kbId);
        if (doc == null) {
            out.put("matched", false);
            out.put("reason", "知识条目不存在");
            return out;
        }
        KbEntryModel entry = entryModelMapper.selectByDoc(scope, kbId);
        if (entry == null) {
            // 懒刷新：无记录（新建/迁移后）即时重算单条目
            List<KbModelResource> models = modelMapper.selectEnabled();
            if (!models.isEmpty()) {
                entry = scoreOne(scope, doc, models, loadPeerPrefixes());
                if (entry != null) entryModelMapper.upsertBatch(List.of(entry));
            }
        }
        if (entry == null) {
            out.put("matched", false);
            out.put("reason", "暂无可用3D模型资源");
            return out;
        }
        KbModelResource model = modelMapper.selectById(entry.getModelId());
        if (model == null) {
            out.put("matched", false);
            out.put("reason", "推荐模型已下架，等待下次重算");
            return out;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("modelId", model.getModelId());
        m.put("modelName", model.getModelName());
        m.put("category", model.getCategory());
        m.put("categoryText", categoryText(model.getCategory()));
        m.put("tags", model.getTags());
        m.put("format", model.getFormat());
        m.put("fileUrl", model.getFileUrl());
        m.put("previewUrl", model.getPreviewUrl());
        m.put("description", model.getDescription());
        out.put("matched", true);
        out.put("model", m);
        out.put("matchScore", entry.getMatchScore());
        out.put("matchRule", entry.getMatchRule());
        out.put("matchTags", entry.getMatchTags());
        out.put("updateTime", entry.getUpdateTime());
        out.put("dynamic", true);
        out.put("note", "推荐结果为变量：随知识库内容（条目增删改/归集/关联重建）自动更新");
        return out;
    }

    /* ==================== 规则评分核心 ==================== */

    /**
     * 单条目评分：遍历候选模型，取最高分；全无命中走类别默认兜底。
     */
    private KbEntryModel scoreOne(String scope, KbDoc d, List<KbModelResource> models, Map<String, Set<String>> peerPrefixes) {
        String docText = docText(d);
        String ref = canonical(d.getSourceRef());
        Set<String> peers = ref == null ? null : peerPrefixes.get(ref);
        boolean sceneCategory = d.getCategory() != null && SCENE_CATEGORIES.contains(d.getCategory());

        KbModelResource best = null;
        double bestScore = 0;
        String bestRule = null;
        String bestTags = null;
        for (KbModelResource model : models) {
            double score = 0;
            List<String> ruleParts = new ArrayList<>();
            // 1) 标签命中
            List<String> hits = hitTags(model.getTags(), docText);
            if (!hits.isEmpty()) {
                score += Math.min(0.60, 0.25 * hits.size());
                ruleParts.add("标签命中(" + String.join("/", hits) + ")");
            }
            // 2) 关联上下文加成
            if (peers != null && !peers.isEmpty()) {
                if ("SCENE".equals(model.getCategory()) && peers.contains("classroom")) {
                    score += 0.20;
                    ruleParts.add("关联加成(上课教室)");
                }
                if ("PROP".equals(model.getCategory()) && peers.contains("exam_project")) {
                    score += 0.15;
                    ruleParts.add("关联加成(考级比赛)");
                }
            }
            // 3) 类目加成
            if ("SCENE".equals(model.getCategory()) && sceneCategory) {
                score += 0.10;
                ruleParts.add("类目加成(" + d.getCategory() + ")");
            }
            score = Math.min(0.99, score);
            if (score > bestScore) {
                bestScore = score;
                best = model;
                bestRule = ruleParts.isEmpty() ? "规则评分" : String.join("+", ruleParts);
                bestTags = hits.isEmpty() ? null : String.join(",", hits);
            }
        }
        // 4) 类别默认兜底：保证每条条目都有适配推荐
        if (best == null || bestScore <= 0) {
            best = models.stream().filter(m -> FALLBACK_MODEL_NAME.equals(m.getModelName())).findFirst().orElse(models.get(0));
            bestScore = 0.30;
            bestRule = "类别默认兜底(" + best.getModelName() + ")";
            bestTags = null;
        }
        KbEntryModel row = new KbEntryModel();
        row.setKbScope(scope);
        row.setKbId(d.getKbId());
        row.setModelId(best.getModelId());
        row.setMatchScore(BigDecimal.valueOf(Math.round(bestScore * 10000d) / 10000d));
        row.setMatchRule(bestRule);
        row.setMatchTags(bestTags != null && bestTags.length() > 200 ? bestTags.substring(0, 200) : bestTags);
        return row;
    }

    /** 模型适配标签与条目文本的命中集合（保持标签顺序，截取前 MAX_MATCH_TAGS 个） */
    private List<String> hitTags(String modelTags, String docText) {
        if (modelTags == null || modelTags.isBlank() || docText == null || docText.isEmpty()) return List.of();
        Set<String> hits = new LinkedHashSet<>();
        for (String tag : modelTags.split("[,，]")) {
            String t = tag.trim();
            if (t.isEmpty() || hits.size() >= MAX_MATCH_TAGS) continue;
            if (docText.contains(t.toLowerCase())) hits.add(t);
        }
        return new ArrayList<>(hits);
    }

    /** 条目匹配文本：标题+分类+标签+摘要+正文头部（小写） */
    private String docText(KbDoc d) {
        StringBuilder sb = new StringBuilder();
        if (d.getTitle() != null) sb.append(d.getTitle()).append(' ');
        if (d.getCategory() != null) sb.append(d.getCategory()).append(' ');
        if (d.getTags() != null) sb.append(d.getTags()).append(' ');
        if (d.getSummary() != null) sb.append(d.getSummary()).append(' ');
        if (d.getContent() != null) {
            String c = d.getContent();
            sb.append(c.length() > CONTENT_LIMIT ? c.substring(0, CONTENT_LIMIT) : c);
        }
        return sb.toString().toLowerCase();
    }

    /** 关联上下文：ref → 对端引用前缀集合（教室/考级项目等场景线索） */
    private Map<String, Set<String>> loadPeerPrefixes() {
        Map<String, Set<String>> map = new HashMap<>();
        List<KbRelation> rels = relationMapper.selectAll();
        for (KbRelation r : rels) {
            String src = r.getSrcRef();
            String dst = r.getDstRef();
            if (src == null || dst == null) continue;
            map.computeIfAbsent(src, k -> new HashSet<>()).add(prefix(dst));
            map.computeIfAbsent(dst, k -> new HashSet<>()).add(prefix(src));
        }
        return map;
    }

    private String prefix(String ref) {
        int i = ref.indexOf(':');
        return i < 0 ? ref : ref.substring(0, i);
    }

    /** 业务引用规范化：schedule:12:t3 → schedule:12 */
    private String canonical(String ref) {
        if (ref == null || ref.isBlank()) return null;
        Matcher m = REF_PATTERN.matcher(ref);
        return m.find() ? m.group(1) : ref.trim();
    }

    private String categoryText(String category) {
        if (category == null) return "";
        return CATEGORY_TEXT.getOrDefault(category, category);
    }

    private List<KbDoc> selectActive(String scope) {
        return switch (scope) {
            case KnowledgeService.SCOPE_ADMIN -> adminMapper.selectAllActive();
            case KnowledgeService.SCOPE_TEACHER -> teacherMapper.selectAllActive(null);
            case KnowledgeService.SCOPE_STUDENT -> studentMapper.selectAllActive(null);
            default -> List.of();
        };
    }

    private KbDoc selectById(String scope, Integer kbId) {
        return switch (scope) {
            case KnowledgeService.SCOPE_ADMIN -> adminMapper.selectById(kbId);
            case KnowledgeService.SCOPE_TEACHER -> teacherMapper.selectById(kbId);
            case KnowledgeService.SCOPE_STUDENT -> studentMapper.selectById(kbId);
            default -> null;
        };
    }
}
