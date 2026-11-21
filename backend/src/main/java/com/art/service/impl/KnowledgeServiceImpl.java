package com.art.service.impl;

import com.art.common.BusinessException;
import com.art.common.PageResult;
import com.art.entity.KbDoc;
import com.art.entity.KbOperationLog;
import com.art.mapper.KbAdminMapper;
import com.art.mapper.KbOperationLogMapper;
import com.art.mapper.KbStudentMapper;
import com.art.mapper.KbTeacherMapper;
import com.art.search.ChineseTokenizer;
import com.art.search.KbModelRecommendService;
import com.art.search.KbRelationService;
import com.art.search.KbVectorIndex;
import com.art.search.KbVectorStore;
import com.art.service.KnowledgeService;
import com.art.util.SecurityUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 知识库统一服务实现：三端同构隔离 + 操作日志留痕 + AI检索依据
  * @author liukang&&heyan
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KbAdminMapper adminMapper;
    private final KbTeacherMapper teacherMapper;
    private final KbStudentMapper studentMapper;
    private final KbOperationLogMapper opLogMapper;
    /** 中文分词组件（jieba） */
    private final ChineseTokenizer tokenizer;
    /** 向量检索引擎 */
    private final KbVectorStore vectorStore;
    /** 跨知识库关联映射 */
    private final KbRelationService relationService;
    /** 3D模型推荐引擎：条目增删改后重算该端推荐（推荐结果随内容变化） */
    private final KbModelRecommendService modelRecommendService;

    /** 混合检索融合权重：词法命中率权重与向量余弦权重 */
    private static final double W_LEXICAL = 0.45;
    private static final double W_VECTOR = 0.55;

    @Override
    public void checkScope(String scope) {
        if (!SCOPE_ADMIN.equals(scope) && !SCOPE_TEACHER.equals(scope) && !SCOPE_STUDENT.equals(scope)) {
            throw new BusinessException("非法的知识库范围：" + scope);
        }
    }

    @Override
    public PageResult<KbDoc> pageDocs(String scope, Integer pageNum, Integer pageSize,
                                      String keyword, String category, Integer status, Integer ownerId) {
        checkScope(scope);
        PageHelper.startPage(pageNum, pageSize);
        List<KbDoc> rows = queryRows(scope, keyword, category, status, ownerId);
        PageInfo<KbDoc> info = new PageInfo<>(rows);
        return PageResult.of(info.getTotal(), info.getList(), pageNum, pageSize);
    }

    @Override
    public KbDoc getDoc(String scope, Integer kbId, Integer ownerId) {
        checkScope(scope);
        KbDoc doc = mapperOf(scope).selectById(kbId);
        if (doc == null) throw new BusinessException("知识条目不存在");
        // 教师/学员端访问归属校验：本人或机构共享可见
        if (ownerId != null && doc.getOwnerId() != null && !ownerId.equals(doc.getOwnerId())) {
            throw new BusinessException("无权查看该知识条目");
        }
        return doc;
    }

    @Override
    public void createDoc(String scope, KbDoc doc, Integer ownerId, String ownerName) {
        checkScope(scope);
        if (doc.getTitle() == null || doc.getTitle().isBlank()) throw new BusinessException("知识条目标题不能为空");
        doc.setKbId(null);
        doc.setSourceType("MANUAL");
        doc.setSourceRef(null);
        doc.setOwnerId(ownerId);
        doc.setOwnerName(ownerName);
        if (doc.getStatus() == null) doc.setStatus(1);
        if (doc.getCategory() == null || doc.getCategory().isBlank()) {
            doc.setCategory(switch (scope) {
                case SCOPE_ADMIN -> "综合";
                case SCOPE_TEACHER -> "教学资料";
                default -> "学习资料";
            });
        }
        doc.setViewCount(0);
        doc.setCreateTime(LocalDateTime.now());
        if (doc.getSummary() == null || doc.getSummary().isBlank()) {
            doc.setSummary(buildSummary(doc.getContent()));
        }
        mapperOf(scope).insert(doc);
        log(scope, doc.getKbId(), "CREATE", "新增知识条目【" + doc.getTitle() + "】");
        refreshModel3d(scope, "新增条目【" + doc.getTitle() + "】");
    }

    @Override
    public void updateDoc(String scope, Integer kbId, KbDoc doc, Integer ownerId) {
        checkScope(scope);
        KbDoc exist = mapperOf(scope).selectById(kbId);
        if (exist == null) throw new BusinessException("知识条目不存在");
        // 归属校验：手动条目仅归属人可改；自动归集条目（AUTO_*）与机构共享条目仅管理员可改
        if (ownerId != null) {
            if (exist.getOwnerId() == null || !ownerId.equals(exist.getOwnerId())) {
                throw new BusinessException("无权修改该知识条目");
            }
            if (exist.getSourceType() != null && exist.getSourceType().startsWith("AUTO")) {
                throw new BusinessException("业务系统自动归集的条目不支持编辑");
            }
        }
        doc.setKbId(kbId);
        mapperOf(scope).update(doc);
        log(scope, kbId, "UPDATE", "更新知识条目【" + exist.getTitle() + "】");
        refreshModel3d(scope, "更新条目【" + exist.getTitle() + "】");
    }

    @Override
    public void deleteDoc(String scope, Integer kbId, Integer ownerId) {
        checkScope(scope);
        KbDoc exist = mapperOf(scope).selectById(kbId);
        if (exist == null) throw new BusinessException("知识条目不存在");
        if (ownerId != null) {
            if (exist.getOwnerId() == null || !ownerId.equals(exist.getOwnerId())) {
                throw new BusinessException("无权删除该知识条目");
            }
            if (exist.getSourceType() != null && exist.getSourceType().startsWith("AUTO")) {
                throw new BusinessException("业务系统自动归集的条目不支持删除");
            }
        }
        mapperOf(scope).deleteById(kbId);
        log(scope, kbId, "DELETE", "删除知识条目【" + exist.getTitle() + "】");
        refreshModel3d(scope, "删除条目【" + exist.getTitle() + "】");
    }

    /** 条目增删改后重算该端3D模型推荐（变量性：推荐随知识库内容变化，异常隔离不影响主流程） */
    private void refreshModel3d(String scope, String reason) {
        try {
            modelRecommendService.rebuildScope(scope);
        } catch (Exception e) {
            log.warn("[知识库] 3D模型推荐重算失败（{}）：{}", reason, e.getMessage());
        }
    }

    @Override
    public void increaseView(String scope, Integer kbId) {
        checkScope(scope);
        mapperOf(scope).increaseViewCount(kbId);
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        for (String scope : List.of(SCOPE_ADMIN, SCOPE_TEACHER, SCOPE_STUDENT)) {
            List<KbDoc> all = queryRows(scope, null, null, null, null);
            long todayNew = all.stream().filter(d -> d.getCreateTime() != null && d.getCreateTime().isAfter(todayStart)).count();
            long autoNew = all.stream().filter(d -> d.getSourceType() != null && d.getSourceType().startsWith("AUTO")).count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("total", all.size());
            item.put("todayNew", todayNew);
            item.put("autoCount", autoNew);
            item.put("manualCount", all.size() - autoNew);
            result.put(scope, item);
        }
        return result;
    }

    @Override
    public List<KbDoc> searchForAi(String scope, Integer ownerId, String keyword, String category, int topK) {
        checkScope(scope);
        int limit = Math.min(Math.max(topK, 1), 20);
        if (keyword == null || keyword.isBlank()) return new ArrayList<>();
        long start = System.currentTimeMillis();
        List<String> terms = buildTerms(keyword);
        if (terms.isEmpty()) return new ArrayList<>();
        Integer owner = SCOPE_ADMIN.equals(scope) ? null : ownerId;

        // 1) 词法候选：分词词元 + 字符2-gram 的多词 LIKE 命中（日期/账号等精确串天然优先）
        List<KbDoc> lexical = switch (scope) {
            case SCOPE_ADMIN -> adminMapper.selectByKeywords(terms, category);
            case SCOPE_TEACHER -> teacherMapper.selectByKeywords(terms, category, owner);
            case SCOPE_STUDENT -> studentMapper.selectByKeywords(terms, category, owner);
            default -> throw new BusinessException("非法的知识库范围：" + scope);
        };
        Map<Integer, Integer> lexHits = new HashMap<>();
        int maxLex = 1;
        for (KbDoc d : lexical) {
            int h = hitScore(d, terms);
            if (h <= 0) continue;
            lexHits.put(d.getKbId(), h);
            maxLex = Math.max(maxLex, h);
        }

        // 2) 向量候选：余弦近邻召回（覆盖词面不同但含义相近的表述）
        List<KbVectorIndex.Hit> vecHits = vectorStore.search(List.of(scope), owner, keyword, limit * 3);
        Map<Integer, Double> cos = new HashMap<>();
        for (KbVectorIndex.Hit h : vecHits) cos.put(h.kbId(), h.score());

        // 3) 融合打分：0.45*词法命中率 + 0.55*向量余弦；两者均命中记为 hybrid
        Map<Integer, KbDoc> pool = new LinkedHashMap<>();
        for (KbDoc d : lexical) pool.put(d.getKbId(), d);
        Set<Integer> ids = new LinkedHashSet<>(lexHits.keySet());
        for (KbVectorIndex.Hit h : vecHits) ids.add(h.kbId());
        List<KbDoc> result = new ArrayList<>();
        for (Integer id : ids) {
            KbDoc d = pool.computeIfAbsent(id, k -> mapperOf(scope).selectById(k));
            if (d == null) continue;
            boolean hasLex = lexHits.containsKey(id);
            Double c = cos.get(id);
            if (!hasLex && c == null) continue;
            double lexNorm = hasLex ? (double) lexHits.get(id) / maxLex : 0d;
            double vec = c == null ? 0d : c;
            d.setRelevanceScore(Math.round(Math.min(1d, W_LEXICAL * lexNorm + W_VECTOR * vec) * 10000d) / 10000d);
            d.setMatchType(hasLex && c != null ? "hybrid" : hasLex ? "lexical" : "vector");
            result.add(d);
        }

        // 4) 跨知识库关联召回：一并召回与高相关条目存在业务关联的条目
        result.addAll(relationService.relatedFor(scope, owner, result));

        result.sort(Comparator.comparingDouble((KbDoc d) ->
                d.getRelevanceScore() == null ? 0d : d.getRelevanceScore()).reversed());
        List<KbDoc> top = result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
        // 生产观测：分词词元 / 词法与向量召回量 / 关联召回量 / 检索耗时（用于延迟与分词准确率核验）
        int fused = countSelf(result);
        log.info("[知识检索] scope={} 词元={} 词法召回={} 向量召回={} 融合={} 关联={} 返回={} 耗时={}ms",
                scope, terms, lexical.size(), vecHits.size(), fused,
                result.size() - fused, top.size(), System.currentTimeMillis() - start);
        return top;
    }

    /** 非关联条目数（即词法/向量融合得到的本体条目数） */
    private int countSelf(List<KbDoc> list) {
        int n = 0;
        for (KbDoc d : list) if (!"related".equals(d.getMatchType())) n++;
        return n;
    }

    @Override
    public List<Map<String, Object>> relatedOf(String scope, Integer kbId, Integer ownerId) {
        checkScope(scope);
        return relationService.relatedOf(scope, kbId, SCOPE_ADMIN.equals(scope) ? null : ownerId);
    }

    @Override
    public Map<String, Object> graph(String scope, Integer kbId, Integer ownerId) {
        checkScope(scope);
        return relationService.graph(scope, kbId, SCOPE_ADMIN.equals(scope) ? null : ownerId);
    }

    /** 检索词元：jieba 分词词元 + 字符 2-gram（上限 30，控制 SQL 条件数与打分开销） */
    private List<String> buildTerms(String q) {
        LinkedHashSet<String> set = new LinkedHashSet<>(tokenizer.tokens(q));
        for (String g : tokenizer.bigrams(q)) {
            if (set.size() >= 30) break;
            set.add(g);
        }
        return new ArrayList<>(set);
    }

    private int hitScore(KbDoc doc, List<String> keywords) {
        int score = 0;
        for (String kw : keywords) {
            if (contains(doc.getTitle(), kw) || contains(doc.getSummary(), kw)
                    || contains(doc.getTags(), kw) || contains(doc.getContent(), kw)) {
                score++;
            }
        }
        return score;
    }

    private boolean contains(String text, String kw) {
        return text != null && text.contains(kw);
    }

    private List<KbDoc> queryRows(String scope, String keyword, String category, Integer status, Integer ownerId) {
        return switch (scope) {
            case SCOPE_ADMIN -> adminMapper.selectPage(keyword, category, status);
            case SCOPE_TEACHER -> teacherMapper.selectPage(keyword, category, status, ownerId);
            case SCOPE_STUDENT -> studentMapper.selectPage(keyword, category, status, ownerId);
            default -> throw new BusinessException("非法的知识库范围：" + scope);
        };
    }

    private KbMapperAdapter mapperOf(String scope) {
        return switch (scope) {
            case SCOPE_ADMIN -> new KbMapperAdapter(adminMapper, null, null);
            case SCOPE_TEACHER -> new KbMapperAdapter(null, teacherMapper, null);
            case SCOPE_STUDENT -> new KbMapperAdapter(null, null, studentMapper);
            default -> throw new BusinessException("非法的知识库范围：" + scope);
        };
    }

    private void log(String scope, Integer kbId, String opType, String detail) {
        KbOperationLog log = new KbOperationLog();
        log.setKbScope(scope);
        log.setKbId(kbId);
        log.setOpType(opType);
        Integer userId = SecurityUtil.getCurrentUserId();
        String username = SecurityUtil.getCurrentUsername();
        log.setOpUserId(userId);
        log.setOpUsername(username != null ? username : "SYSTEM");
        log.setOpDetail(detail);
        log.setOpTime(LocalDateTime.now());
        opLogMapper.insert(log);
    }

    /** 三端 Mapper 的统一适配器（同构表） */
    private record KbMapperAdapter(KbAdminMapper admin, KbTeacherMapper teacher, KbStudentMapper student) {
        KbDoc selectById(Integer kbId) {
            if (admin != null) return admin.selectById(kbId);
            if (teacher != null) return teacher.selectById(kbId);
            return student.selectById(kbId);
        }
        int insert(KbDoc doc) {
            if (admin != null) return admin.insert(doc);
            if (teacher != null) return teacher.insert(doc);
            return student.insert(doc);
        }
        int update(KbDoc doc) {
            if (admin != null) return admin.update(doc);
            if (teacher != null) return teacher.update(doc);
            return student.update(doc);
        }
        int deleteById(Integer kbId) {
            if (admin != null) return admin.deleteById(kbId);
            if (teacher != null) return teacher.deleteById(kbId);
            return student.deleteById(kbId);
        }
        int increaseViewCount(Integer kbId) {
            if (admin != null) return admin.increaseViewCount(kbId);
            if (teacher != null) return teacher.increaseViewCount(kbId);
            return student.increaseViewCount(kbId);
        }
    }

    private String buildSummary(String content) {
        if (content == null || content.isBlank()) return null;
        String plain = content.replaceAll("\\s+", " ").trim();
        return plain.length() > 200 ? plain.substring(0, 200) + "…" : plain;
    }
}
