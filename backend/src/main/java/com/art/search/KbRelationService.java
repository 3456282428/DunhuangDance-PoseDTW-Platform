package com.art.search;

import com.art.entity.KbDoc;
import com.art.entity.KbDocVector;
import com.art.entity.KbRelation;
import com.art.mapper.KbAdminMapper;
import com.art.mapper.KbRelationMapper;
import com.art.mapper.KbStudentMapper;
import com.art.mapper.KbTeacherMapper;
import com.art.mapper.KbVectorMapper;
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
 * 跨知识库关联映射服务。
 * <p>
 * 设计要点：关系存「业务引用级」（如 {@code schedule:12 → class:3}）而非「知识条目级」。
 * 因为同一条业务数据会在管理端/教师端/学员端各投影出一条知识条目（source_ref 可能带
 * {@code :t{teacherId}} / {@code :s{studentId}} 端别后缀），若按条目 id 建关系会产生笛卡尔积；
 * 按业务引用建关系后，查询时通过 {@code source_ref = ref OR source_ref LIKE ref + ':%'} 展开到三端，
 * 天然对齐不同端的同源内容。
 * <p>
 * 能力：
 * <ul>
 *   <li>{@link #build()}：由业务表外键推导关系并幂等重建 kb_relation；</li>
 *   <li>{@link #relatedFor(String, Integer, List)}：检索时的关联内容自动召回（融入 searchForAi）；</li>
 *   <li>{@link #relatedOf(String, Integer, Integer)}：单条目的关联内容联动查询；</li>
 *   <li>{@link #graph(String, Integer, Integer)}：以某条目为中心的关系图谱（可视化）。</li>
 * </ul>
 * @author liukang&&heyan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbRelationService {

    /** 关联关系重建的单批写入行数 */
    private static final int BUILD_CHUNK = 500;
    /** 参与关联召回的主条目数量上限（控制关系查询规模） */
    private static final int PRIMARY_LIMIT = 5;
    /** 关联召回总条数上限 */
    private static final int RELATED_LIMIT = 12;
    /** 关联条目相对主条目的分数折扣（保证主条目前置） */
    private static final double RELATED_DISCOUNT = 0.85;
    /** 内容相似关联：余弦相似度阈值（达标才建 CONTENT_SIM 关系） */
    private static final double SIM_THRESHOLD = 0.55;
    /** 内容相似关联：单条目最多关联的相似条目数 */
    private static final int SIM_TOP_N = 3;
    /** 内容相似关联：关系总量上限（防止关系表膨胀） */
    private static final int SIM_MAX_ROWS = 1500;

    /** 业务引用规范形态：前缀:数字（去掉 :t{id} / :s{id} 端别后缀） */
    private static final Pattern REF_PATTERN = Pattern.compile("^([a-z_]+:\\d+)");

    /** 业务引用前缀 → 中文名称（用于关系标签可读化） */
    private static final Map<String, String> REF_NAME = Map.ofEntries(
            Map.entry("course", "课程"),
            Map.entry("course_category", "课程分类"),
            Map.entry("classroom", "教室"),
            Map.entry("class", "班级"),
            Map.entry("schedule", "排课"),
            Map.entry("teacher", "教师"),
            Map.entry("student", "学员"),
            Map.entry("enrollment", "报名"),
            Map.entry("attendance", "考勤"),
            Map.entry("homework", "作业"),
            Map.entry("submission", "作业提交"),
            Map.entry("exam_project", "考试项目"),
            Map.entry("exam_reg", "考试报名"),
            Map.entry("payment", "缴费"),
            Map.entry("appointment", "预约"),
            Map.entry("feedback", "反馈"),
            Map.entry("comment", "评论"),
            Map.entry("srequest", "调课申请"),
            Map.entry("reschedule", "补课"),
            Map.entry("user", "账号"));

    /** 关系类型 → 中文描述 */
    private static final Map<String, String> REL_TEXT = Map.ofEntries(
            Map.entry("BELONGS_TO", "归属班级"),
            Map.entry("OF_CLASS", "所属班级"),
            Map.entry("IN_CLASS", "所在班级"),
            Map.entry("OF_TEACHER", "授课教师"),
            Map.entry("OF_STUDENT", "所属学员"),
            Map.entry("OF_COURSE", "所属课程"),
            Map.entry("IN_ROOM", "上课教室"),
            Map.entry("OF_PROJECT", "所属考试"),
            Map.entry("OF_HOMEWORK", "所属作业"),
            Map.entry("OF_SCHEDULE", "所属排课"),
            Map.entry("OF_CATEGORY", "所属分类"),
            Map.entry("CONTENT_SIM", "相似内容"));

    /** 关联维度 → 中文描述 */
    private static final Map<String, String> DIMENSION_TEXT = Map.of(
            "ORG", "组织维度",
            "TEACH", "教学维度",
            "FIN", "财务维度",
            "ASSET", "资产维度",
            "CONTENT", "内容维度");

    /** 关联来源 → 中文描述 */
    private static final Map<String, String> SOURCE_TEXT = Map.of(
            "BUSINESS_FK", "业务外键",
            "CONTENT_SIM", "内容相似");

    private final KbRelationMapper relationMapper;
    private final KbAdminMapper adminMapper;
    private final KbTeacherMapper teacherMapper;
    private final KbStudentMapper studentMapper;
    private final KbVectorMapper vectorMapper;

    /* ==================== 关系重建 ==================== */

    /**
     * 由业务表外键推导关系 + 内容相似计算关联，幂等重建 kb_relation。
     * 每条关系标注：关联类型（rel_type）、关联维度（rel_dimension）、关联来源（rel_source）、关联强度（weight）。
     * 供全量数据归集（{@code ingestAllData}）与定时任务调用。
     */
    public synchronized void build() {
        long start = System.currentTimeMillis();
        List<RefPair> pairs = relationMapper.selectBusinessPairs();
        if (pairs == null || pairs.isEmpty()) {
            relationMapper.deleteAll();
            log.info("[跨库关联] 暂无可推导的业务关联");
            return;
        }
        Set<String> seen = new HashSet<>();
        List<KbRelation> rows = new ArrayList<>(pairs.size());
        for (RefPair p : pairs) {
            if (p.getSrcRef() == null || p.getDstRef() == null || p.getRelType() == null) continue;
            if (p.getSrcRef().equals(p.getDstRef())) continue;
            String key = p.getSrcRef() + '|' + p.getDstRef() + '|' + p.getRelType();
            if (!seen.add(key)) continue;
            KbRelation r = new KbRelation();
            r.setSrcRef(p.getSrcRef());
            r.setDstRef(p.getDstRef());
            r.setRelType(p.getRelType());
            r.setRelDimension(dimensionOf(p.getSrcRef(), p.getRelType()));
            r.setRelSource("BUSINESS_FK");
            r.setSrcLabel(label(p.getSrcRef()));
            r.setDstLabel(label(p.getDstRef()));
            r.setWeight(BigDecimal.ONE);
            rows.add(r);
        }
        // 内容相似关联：基于向量余弦，标注维度=内容、强度=相似度
        List<KbRelation> simRows = buildContentSimRows();
        for (KbRelation r : simRows) {
            String key = r.getSrcRef() + '|' + r.getDstRef() + '|' + r.getRelType();
            if (seen.add(key)) rows.add(r);
        }
        relationMapper.deleteAll();
        for (int i = 0; i < rows.size(); i += BUILD_CHUNK) {
            relationMapper.upsertBatch(rows.subList(i, Math.min(rows.size(), i + BUILD_CHUNK)));
        }
        log.info("[跨库关联] 关系重建完成：业务外键 {} 条 + 内容相似 {} 条，耗时 {}ms",
                rows.size() - simRows.size(), simRows.size(), System.currentTimeMillis() - start);
    }

    /**
     * 关联维度推导：由关系类型 + 源引用前缀共同决定，规则可追溯。
     * <ul>
     *   <li>财务维度：源为 payment（缴费）；</li>
     *   <li>资产维度：IN_ROOM（教室场地）；</li>
     *   <li>教学维度：OF_TEACHER / OF_SCHEDULE / OF_HOMEWORK，及考勤/作业/考级的 OF_STUDENT；</li>
     *   <li>组织维度：班级/课程/分类归属，及报名的 OF_STUDENT；</li>
     *   <li>内容维度：CONTENT_SIM（内容相似）。</li>
     * </ul>
     */
    private String dimensionOf(String srcRef, String relType) {
        String prefix = prefixOf(srcRef);
        if ("payment".equals(prefix)) return "FIN";
        return switch (relType) {
            case "CONTENT_SIM" -> "CONTENT";
            case "IN_ROOM" -> "ASSET";
            case "OF_TEACHER", "OF_SCHEDULE", "OF_HOMEWORK" -> "TEACH";
            case "OF_STUDENT" -> "enrollment".equals(prefix) ? "ORG" : "TEACH";
            default -> "ORG";
        };
    }

    /** 业务引用前缀（prefix:id → prefix） */
    private String prefixOf(String ref) {
        if (ref == null) return "";
        int i = ref.indexOf(':');
        return i < 0 ? ref : ref.substring(0, i);
    }

    /**
     * 内容相似关联计算：同端内按向量余弦找出与每条条目最相似的 topN 条目，
     * 相似度 ≥ {@link #SIM_THRESHOLD} 则建立 CONTENT_SIM 关系（强度=相似度）。
     * 关系按业务引用记录，方向统一为字典序小的在前，保证对称去重。
     */
    private List<KbRelation> buildContentSimRows() {
        List<KbRelation> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String scope : List.of(KnowledgeService.SCOPE_ADMIN,
                KnowledgeService.SCOPE_TEACHER,
                KnowledgeService.SCOPE_STUDENT)) {
            if (out.size() >= SIM_MAX_ROWS) break;
            // 1) 取该端启用条目（带业务引用，ref 级关系才可追溯）
            List<KbDoc> docs = new ArrayList<>();
            for (KbDoc d : selectActive(scope)) {
                if (d.getSourceRef() != null && !d.getSourceRef().isBlank()) docs.add(d);
            }
            if (docs.size() < 2) continue;
            // 2) 取该端向量，kbId → 向量
            Map<Integer, float[]> vecs = new HashMap<>();
            for (KbDocVector v : vectorMapper.selectByScope(scope)) {
                float[] vec = KbVectorStore.parse(v.getVec());
                if (vec.length > 0) vecs.put(v.getKbId(), vec);
            }
            List<KbDoc> usable = new ArrayList<>(docs.size());
            for (KbDoc d : docs) {
                if (vecs.containsKey(d.getKbId())) usable.add(d);
            }
            // 3) 两两余弦，每条目保留 topN 个达标邻居
            for (int i = 0; i < usable.size() && out.size() < SIM_MAX_ROWS; i++) {
                KbDoc a = usable.get(i);
                String refA = canonical(a.getSourceRef());
                // 收集达标相似对
                List<double[]> hits = new ArrayList<>(SIM_TOP_N);
                for (int j = 0; j < usable.size(); j++) {
                    if (i == j) continue;
                    KbDoc b = usable.get(j);
                    String refB = canonical(b.getSourceRef());
                    if (refA.equals(refB)) continue; // 同源内容三端投影，天然同 ref 跳过
                    double cos = cosine(vecs.get(a.getKbId()), vecs.get(b.getKbId()));
                    if (cos < SIM_THRESHOLD) continue;
                    hits.add(new double[]{cos, j});
                }
                hits.sort((x, y) -> Double.compare(y[0], x[0]));
                for (int k = 0; k < Math.min(hits.size(), SIM_TOP_N) && out.size() < SIM_MAX_ROWS; k++) {
                    KbDoc b = usable.get((int) hits.get(k)[1]);
                    double cos = hits.get(k)[0];
                    String src = refA.compareTo(canonical(b.getSourceRef())) <= 0 ? refA : canonical(b.getSourceRef());
                    String dst = src.equals(refA) ? canonical(b.getSourceRef()) : refA;
                    String key = src + '|' + dst + '|' + "CONTENT_SIM";
                    if (seen.add(key)) {
                        KbRelation r = new KbRelation();
                        r.setSrcRef(src);
                        r.setDstRef(dst);
                        r.setRelType("CONTENT_SIM");
                        r.setRelDimension("CONTENT");
                        r.setRelSource("CONTENT_SIM");
                        r.setSrcLabel(label(src));
                        r.setDstLabel(label(dst));
                        r.setWeight(BigDecimal.valueOf(Math.round(cos * 100d) / 100d));
                        out.add(r);
                    }
                }
            }
        }
        return out;
    }

    /** 向量余弦相似度 */
    private double cosine(float[] a, float[] b) {
        int n = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na <= 0 || nb <= 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private List<KbDoc> selectActive(String scope) {
        return switch (scope) {
            case KnowledgeService.SCOPE_ADMIN -> adminMapper.selectAllActive();
            case KnowledgeService.SCOPE_TEACHER -> teacherMapper.selectAllActive(null);
            default -> studentMapper.selectAllActive(null);
        };
    }

    public int count() {
        return relationMapper.countAll();
    }

    /* ==================== 关联召回（融入检索） ==================== */

    /**
     * 检索时自动召回：取高相关主条目的业务引用 → 查关联关系 → 展开为同端知识条目。
     *
     * @param primaries 已按相关度排序的主检索结果
     * @return 关联条目（已排除主条目内已出现者，relevanceScore 按折扣给出）
     */
    public List<KbDoc> relatedFor(String scope, Integer ownerId, List<KbDoc> primaries) {
        if (primaries == null || primaries.isEmpty()) return new ArrayList<>();

        // 1) 主条目业务引用规范化（去端别后缀）并去重
        List<String> selfRefs = new ArrayList<>();
        Map<String, String> refFrom = new LinkedHashMap<>();
        for (KbDoc d : primaries) {
            if (selfRefs.size() >= PRIMARY_LIMIT) break;
            String canon = canonical(d.getSourceRef());
            if (canon == null || refFrom.containsKey(canon)) continue;
            selfRefs.add(canon);
            refFrom.put(canon, d.getTitle() == null ? canon : d.getTitle());
        }
        if (selfRefs.isEmpty()) return new ArrayList<>();

        // 2) 查关系，收集「另一端」的业务引用
        List<KbRelation> rels = relationMapper.selectByRefs(selfRefs);
        if (rels == null || rels.isEmpty()) return new ArrayList<>();
        Set<String> selfSet = new HashSet<>(selfRefs);
        Set<String> relatedRefs = new LinkedHashSet<>();
        Map<String, String> relatedOrigin = new HashMap<>();
        for (KbRelation r : rels) {
            String s = r.getSrcRef();
            String t = r.getDstRef();
            if (selfSet.contains(s) && !selfSet.contains(t)) {
                relatedRefs.add(t);
                relatedOrigin.putIfAbsent(t, s);
            } else if (selfSet.contains(t) && !selfSet.contains(s)) {
                relatedRefs.add(s);
                relatedOrigin.putIfAbsent(s, t);
            }
        }
        if (relatedRefs.isEmpty()) return new ArrayList<>();

        // 3) 展开同端条目（自动兼容 :t{id} / :s{id} 后缀）
        Set<Integer> existIds = new HashSet<>();
        double baseScore = 0d;
        for (KbDoc d : primaries) {
            existIds.add(d.getKbId());
            Double sc = d.getRelevanceScore();
            int rank = Math.max(1, primaries.indexOf(d) + 1);
            double decay = sc == null ? 1d / rank : sc;
            baseScore = Math.max(baseScore, decay);
        }

        List<KbDoc> out = new ArrayList<>();
        Integer owner = KnowledgeService.SCOPE_ADMIN.equals(scope) ? null : ownerId;
        for (String ref : relatedRefs) {
            if (out.size() >= RELATED_LIMIT) break;
            for (KbDoc d : expand(scope, owner, ref)) {
                if (out.size() >= RELATED_LIMIT) break;
                if (!existIds.add(d.getKbId())) continue;
                d.setMatchType("related");
                d.setRelatedFrom(originLabel(relatedOrigin.get(ref)));
                d.setRelevanceScore(Math.round(Math.min(1d, baseScore * RELATED_DISCOUNT) * 10000d) / 10000d);
                out.add(d);
            }
        }
        return out;
    }

    /* ==================== 联动查询 / 图谱 ==================== */

    /**
     * 单条知识条目的关联内容联动查询
     */
    public List<Map<String, Object>> relatedOf(String scope, Integer kbId, Integer ownerId) {
        KbDoc center = mapperOf(scope).selectById(kbId);
        if (center == null) return new ArrayList<>();
        String centerRef = canonical(center.getSourceRef());
        List<Map<String, Object>> out = new ArrayList<>();
        if (centerRef == null) return out;

        Integer owner = KnowledgeService.SCOPE_ADMIN.equals(scope) ? null : ownerId;
        List<KbRelation> rels = relationMapper.selectByRef(centerRef);
        Set<Integer> seen = new HashSet<>();
        seen.add(kbId);
        for (KbRelation r : rels) {
            String other = centerRef.equals(r.getSrcRef()) ? r.getDstRef() : r.getSrcRef();
            String relType = r.getRelType();
            for (KbDoc d : expand(scope, owner, other)) {
                if (!seen.add(d.getKbId())) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("kbId", d.getKbId());
                item.put("scope", scope);
                item.put("title", d.getTitle());
                item.put("category", d.getCategory());
                item.put("summary", d.getSummary());
                item.put("sourceType", d.getSourceType());
                item.put("sourceRef", d.getSourceRef());
                item.put("relType", relType);
                item.put("relText", relText(relType));
                item.put("dimension", r.getRelDimension());
                item.put("dimensionText", dimensionText(r.getRelDimension()));
                item.put("strength", r.getWeight());
                item.put("relSource", r.getRelSource());
                item.put("relSourceText", sourceText(r.getRelSource()));
                item.put("label", label(other));
                out.add(item);
            }
        }
        return out;
    }

    /**
     * 以某条目为中心的关系图谱（节点 + 边），供管理端关联可视化。
     */
    public Map<String, Object> graph(String scope, Integer kbId, Integer ownerId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> nodeKeys = new HashSet<>();

        KbDoc center = mapperOf(scope).selectById(kbId);
        if (center == null) {
            result.put("nodes", nodes);
            result.put("edges", edges);
            return result;
        }
        String centerKey = key(scope, kbId);
        nodes.add(node(centerKey, scope, center, true));
        nodeKeys.add(centerKey);

        String centerRef = canonical(center.getSourceRef());
        if (centerRef == null) {
            result.put("nodes", nodes);
            result.put("edges", edges);
            return result;
        }

        Integer owner = KnowledgeService.SCOPE_ADMIN.equals(scope) ? null : ownerId;
        List<KbRelation> rels = relationMapper.selectByRef(centerRef);
        Set<Integer> seen = new HashSet<>();
        seen.add(kbId);
        for (KbRelation r : rels) {
            String other = centerRef.equals(r.getSrcRef()) ? r.getDstRef() : r.getSrcRef();
            List<KbDoc> docs = expand(scope, owner, other);
            if (docs.isEmpty()) {
                // 关联的业务实体在本端无对应条目：仍以「引用节点」呈现，保证关系完整可见
                String refKey = "ref:" + other;
                if (nodeKeys.add(refKey)) {
                    Map<String, Object> refNode = new LinkedHashMap<>();
                    refNode.put("id", refKey);
                    refNode.put("scope", scope);
                    refNode.put("kbId", null);
                    refNode.put("title", label(other));
                    refNode.put("category", "关联业务实体");
                    refNode.put("sourceRef", other);
                    refNode.put("center", false);
                    nodes.add(refNode);
                }
                edges.add(edge(centerKey, refKey, r));
                continue;
            }
            for (KbDoc d : docs) {
                if (!seen.add(d.getKbId())) continue;
                String k = key(scope, d.getKbId());
                if (nodeKeys.add(k)) nodes.add(node(k, scope, d, false));
                edges.add(edge(centerKey, k, r));
            }
        }
        result.put("nodes", nodes);
        result.put("edges", edges);
        result.put("center", centerKey);
        return result;
    }

    /* ==================== 内部工具 ==================== */

    private List<KbDoc> expand(String scope, Integer ownerId, String ref) {
        return switch (scope) {
            case KnowledgeService.SCOPE_ADMIN -> adminMapper.selectByRefPrefix(ref);
            case KnowledgeService.SCOPE_TEACHER -> teacherMapper.selectByRefPrefix(ref, ownerId);
            case KnowledgeService.SCOPE_STUDENT -> studentMapper.selectByRefPrefix(ref, ownerId);
            default -> new ArrayList<>();
        };
    }

    private KbDocMapper mapperOf(String scope) {
        return switch (scope) {
            case KnowledgeService.SCOPE_ADMIN -> new KbDocMapper() {
                @Override public KbDoc selectById(Integer kbId) { return adminMapper.selectById(kbId); }
            };
            case KnowledgeService.SCOPE_TEACHER -> new KbDocMapper() {
                @Override public KbDoc selectById(Integer kbId) { return teacherMapper.selectById(kbId); }
            };
            case KnowledgeService.SCOPE_STUDENT -> new KbDocMapper() {
                @Override public KbDoc selectById(Integer kbId) { return studentMapper.selectById(kbId); }
            };
            default -> throw new IllegalArgumentException("非法的知识库范围：" + scope);
        };
    }

    private interface KbDocMapper {
        KbDoc selectById(Integer kbId);
    }

    /** 业务引用规范化：schedule:12:t3 → schedule:12；非规范引用原样返回 */
    private String canonical(String ref) {
        if (ref == null || ref.isBlank()) return null;
        Matcher m = REF_PATTERN.matcher(ref);
        return m.find() ? m.group(1) : ref.trim();
    }

    /**
     * 全局关联图谱（3D建模图谱数据源）：实体节点 + 全量关联边 + 维度统计。
     * 节点=业务引用实体（ref 规范化），边=kb_relation 关联（含类型/维度/强度），
     * 供知识库管理页以 3D 力导向图谱呈现各数据之间的关联。
     */
    public Map<String, Object> graphOverview() {
        List<KbRelation> rels = relationMapper.selectAll();
        // 节点按规范化 ref 聚合度数（同实体多端投影合并）
        Map<String, Integer> degree = new LinkedHashMap<>();
        for (KbRelation r : rels) {
            String s = canonical(r.getSrcRef());
            String d = canonical(r.getDstRef());
            if (s != null) degree.merge(s, 1, Integer::sum);
            if (d != null) degree.merge(d, 1, Integer::sum);
        }
        List<Map<String, Object>> nodes = new ArrayList<>();
        Map<String, String> categories = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> en : degree.entrySet()) {
            String ref = en.getKey();
            String prefix = prefixOf(ref);
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", ref);
            n.put("name", label(ref));
            n.put("category", prefix);
            n.put("categoryText", REF_NAME.getOrDefault(prefix, prefix));
            n.put("degree", en.getValue());
            nodes.add(n);
            categories.putIfAbsent(prefix, REF_NAME.getOrDefault(prefix, prefix));
        }
        // 边按 (src,dst,type) 规范化去重
        List<Map<String, Object>> edges = new ArrayList<>();
        Map<String, Integer> dimCount = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (KbRelation r : rels) {
            String s = canonical(r.getSrcRef());
            String d = canonical(r.getDstRef());
            if (s == null || d == null) continue;
            String key = s + '|' + d + '|' + r.getRelType();
            if (!seen.add(key)) continue;
            Map<String, Object> e = edge(s, d, r);
            edges.add(e);
            dimCount.merge(r.getRelDimension(), 1, Integer::sum);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        result.put("categories", categories);
        result.put("dimensionStats", dimCount);
        result.put("nodeCount", nodes.size());
        result.put("edgeCount", edges.size());
        return result;
    }

    private String label(String ref) {
        String canon = canonical(ref);
        if (canon == null) return String.valueOf(ref);
        int i = canon.indexOf(':');
        if (i < 0) return canon;
        String prefix = canon.substring(0, i);
        String id = canon.substring(i + 1);
        return REF_NAME.getOrDefault(prefix, prefix) + " #" + id;
    }

    /** 关联来源描述：主条目引用 → 「排课 #12」 */
    private String originLabel(String ref) {
        return ref == null ? null : "关联自 " + label(ref);
    }

    private String relText(String relType) {
        if (relType == null) return "";
        return REL_TEXT.getOrDefault(relType, relType);
    }

    private String dimensionText(String dimension) {
        if (dimension == null) return "";
        return DIMENSION_TEXT.getOrDefault(dimension, dimension);
    }

    private String sourceText(String source) {
        if (source == null) return "";
        return SOURCE_TEXT.getOrDefault(source, source);
    }

    private String key(String scope, Integer kbId) {
        return scope + ':' + kbId;
    }

    private Map<String, Object> node(String id, String scope, KbDoc d, boolean center) {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("id", id);
        n.put("scope", scope);
        n.put("kbId", d.getKbId());
        n.put("title", d.getTitle());
        n.put("category", d.getCategory());
        n.put("summary", d.getSummary());
        n.put("sourceType", d.getSourceType());
        n.put("sourceRef", d.getSourceRef());
        n.put("center", center);
        return n;
    }

    private Map<String, Object> edge(String source, String target, KbRelation r) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("source", source);
        e.put("target", target);
        e.put("relType", r.getRelType());
        e.put("label", relText(r.getRelType()));
        e.put("dimension", r.getRelDimension());
        e.put("dimensionText", dimensionText(r.getRelDimension()));
        e.put("strength", r.getWeight());
        e.put("relSource", r.getRelSource());
        return e;
    }
}
