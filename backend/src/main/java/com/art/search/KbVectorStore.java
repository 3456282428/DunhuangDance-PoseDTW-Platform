package com.art.search;

import com.art.entity.KbDoc;
import com.art.entity.KbDocVector;
import com.art.mapper.KbAdminMapper;
import com.art.mapper.KbStudentMapper;
import com.art.mapper.KbTeacherMapper;
import com.art.mapper.KbVectorMapper;
import com.art.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 知识库向量库：负责「知识条目 → 向量 → 落库 → 装载内存索引」的全生命周期。
 * - 冷启动：从 kb_doc_vector 按当前生效模型装载索引，缺失条目增量补齐；
 * - 每次全量归集后调用 {@link #refresh()}，按内容指纹（content_hash）与模型标识做增量刷新，
 *   只有内容变更或模型切换的条目才重新向量化，避免重复计算；
 * - 检索时若生效模型发生切换（远程 Embedding 启用/停用），自动整库重算一次。
 * @author liukang&&heyan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbVectorStore {

    private static final List<String> SCOPES =
            List.of(KnowledgeService.SCOPE_ADMIN, KnowledgeService.SCOPE_TEACHER, KnowledgeService.SCOPE_STUDENT);
    /** 单次批量写入行数（控制单条 SQL 体积） */
    private static final int UPSERT_CHUNK = 200;
    /** 参与向量化的正文截断长度（标题+摘要+标签已覆盖主要语义，正文仅取头部） */
    private static final int CONTENT_LIMIT = 400;

    private final KbAdminMapper adminMapper;
    private final KbTeacherMapper teacherMapper;
    private final KbStudentMapper studentMapper;
    private final KbVectorMapper vectorMapper;
    private final KbEmbeddingService embedding;
    private final KbVectorIndex index;
    private final ChineseTokenizer tokenizer;

    /** 当前索引使用的向量模型标识 */
    private volatile String indexTag;

    private record DocRef(String scope, Integer kbId, Integer ownerId, String text, String hash, List<String> tokens) {
    }

    /** 冷启动：装载已有向量，缺失的增量补齐 */
    public void initialize() {
        try {
            refresh();
        } catch (Exception e) {
            log.warn("[向量检索] 冷启动装载失败（不影响主流程）：{}", e.getMessage());
        }
    }

    /** 是否可用（索引非空） */
    public boolean isReady() {
        return index != null && index.size() > 0;
    }

    public int indexedCount() {
        return index.size();
    }

    public String indexTag() {
        return indexTag;
    }

    /**
     * 增量刷新：内容变更/模型切换的条目重新向量化并覆盖索引
     */
    public synchronized void refresh() {
        long start = System.currentTimeMillis();
        String tag = embedding.activeTag();

        List<DocRef> refs = new ArrayList<>();
        Map<String, Integer> df = new HashMap<>();
        for (String scope : SCOPES) {
            for (KbDoc d : selectActive(scope)) {
                String text = docText(d);
                List<String> toks = tokenizer.tokens(text);
                Set<String> uniq = new LinkedHashSet<>(toks);
                uniq.addAll(tokenizer.bigrams(text));
                for (String t : uniq) df.merge(t, 1, Integer::sum);
                refs.add(new DocRef(scope, d.getKbId(), d.getOwnerId(), text, md5(String.join(" ", toks)), toks));
            }
        }
        embedding.local().updateIdf(df, refs.size());

        int changed = 0, removed = 0, loaded = 0;
        for (String scope : SCOPES) {
            Map<Integer, KbDocVector> existing = new LinkedHashMap<>();
            for (KbDocVector v : vectorMapper.selectByScope(scope)) existing.put(v.getKbId(), v);

            List<DocRef> pending = new ArrayList<>();
            Set<Integer> alive = new LinkedHashSet<>();
            for (DocRef r : refs) {
                if (!scope.equals(r.scope())) continue;
                alive.add(r.kbId());
                KbDocVector old = existing.get(r.kbId());
                if (old != null && r.hash().equals(old.getContentHash()) && tag.equals(old.getModel())) {
                    // 内容与模型均未变更：无需重算，但冷启动/重启后必须把已有向量装回内存索引
                    if (!index.has(r.scope(), r.kbId())) {
                        float[] vec = parse(old.getVec());
                        if (vec.length > 0) {
                            index.put(r.scope(), r.kbId(), r.ownerId(), vec);
                            loaded++;
                        }
                    }
                    continue;
                }
                pending.add(r);
            }
            // 已删除条目：清理向量与索引
            for (Integer kbId : existing.keySet()) {
                if (!alive.contains(kbId)) {
                    vectorMapper.deleteByDoc(scope, kbId);
                    index.remove(scope, kbId);
                    removed++;
                }
            }
            if (pending.isEmpty()) continue;

            List<String> texts = new ArrayList<>(pending.size());
            for (DocRef r : pending) texts.add(r.text());
            List<float[]> vecs = embedding.embedBatch(texts);

            List<KbDocVector> rows = new ArrayList<>(pending.size());
            for (int i = 0; i < pending.size(); i++) {
                DocRef r = pending.get(i);
                float[] vec = vecs.get(i);
                KbDocVector row = new KbDocVector();
                row.setKbScope(r.scope());
                row.setKbId(r.kbId());
                row.setOwnerId(r.ownerId());
                row.setDim(vec.length);
                row.setModel(tag);
                row.setContentHash(r.hash());
                row.setVec(join(vec));
                rows.add(row);
                index.put(r.scope(), r.kbId(), r.ownerId(), vec);
                changed++;
            }
            for (int i = 0; i < rows.size(); i += UPSERT_CHUNK) {
                vectorMapper.upsertBatch(rows.subList(i, Math.min(rows.size(), i + UPSERT_CHUNK)));
            }
        }
        indexTag = tag;
        long ms = System.currentTimeMillis() - start;
        log.info("[向量检索] 索引刷新完成：模型={}，条目 {} 条，重算 {} 条，装载 {} 条，清理 {} 条，索引 {} 条，耗时 {}ms",
                tag, refs.size(), changed, loaded, removed, index.size(), ms);
    }

    /**
     * 向量近邻检索
     * @param scopes  检索端别列表
     * @param ownerId 归属过滤（null=不过滤）
     */
    public List<KbVectorIndex.Hit> search(List<String> scopes, Integer ownerId, String question, int topK) {
        if (question == null || question.isBlank() || !isReady()) return List.of();
        String tag = embedding.activeTag();
        if (indexTag == null || !indexTag.equals(tag)) {
            try {
                refresh();
            } catch (Exception e) {
                log.warn("[向量检索] 模型切换重算失败，沿用现有索引：{}", e.getMessage());
            }
        }
        float[] q = embedding.embed(question);
        if (q == null || q.length == 0) return List.of();
        return index.search(scopes, ownerId, q, topK);
    }

    /* ==================== 内部工具 ==================== */

    private List<KbDoc> selectActive(String scope) {
        return switch (scope) {
            case KnowledgeService.SCOPE_ADMIN -> adminMapper.selectAllActive();
            case KnowledgeService.SCOPE_TEACHER -> teacherMapper.selectAllActive(null);
            default -> studentMapper.selectAllActive(null);
        };
    }

    private String docText(KbDoc d) {
        StringBuilder sb = new StringBuilder();
        if (d.getTitle() != null) sb.append(d.getTitle()).append(' ');
        if (d.getTags() != null) sb.append(d.getTags()).append(' ');
        if (d.getSummary() != null) sb.append(d.getSummary()).append(' ');
        String c = d.getContent();
        if (c != null) sb.append(c.length() > CONTENT_LIMIT ? c.substring(0, CONTENT_LIMIT) : c);
        return sb.toString().trim();
    }

    private String join(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 9);
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            // 保留 6 位小数，兼顾精度与存储体积
            sb.append(Math.round(v[i] * 1_000_000d) / 1_000_000d);
        }
        return sb.toString();
    }

    /** 解析逗号分隔向量串 */
    public static float[] parse(String vec) {
        if (vec == null || vec.isBlank()) return new float[0];
        String[] parts = vec.split(",");
        float[] v = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                v[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException e) {
                v[i] = 0f;
            }
        }
        return v;
    }

    private String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }
}
