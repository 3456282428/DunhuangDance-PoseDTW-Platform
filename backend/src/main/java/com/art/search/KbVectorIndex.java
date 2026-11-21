package com.art.search;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存向量检索引擎：按端别（ADMIN/TEACHER/STUDENT）分片持有知识条目向量，
 * 支持增量 upsert / 删除、冷启动整库装载、按「本人或机构共享」归属过滤的余弦近邻检索。
 * 向量在写入前已 L2 归一化，因此余弦相似度退化为点积，单条打分仅一次乘加循环，
 * 千级条目检索耗时 < 1ms（实测见验证记录）。
 * @author liukang&&heyan
 */
@Component
public class KbVectorIndex {

    /** 索引条目：scope + kbId 唯一定位，ownerId 供端别归属过滤 */
    public record Entry(String scope, Integer kbId, Integer ownerId, float[] vec) {
    }

    /** 命中：scope/kbId/余弦相似度 */
    public record Hit(String scope, Integer kbId, double score) {
    }

    private final Map<String, List<Entry>> byScope = new ConcurrentHashMap<>();
    private final Map<String, Entry> byKey = new ConcurrentHashMap<>();

    private static String key(String scope, Integer kbId) {
        return scope + ':' + kbId;
    }

    /** 新增或覆盖一条向量 */
    public void put(String scope, Integer kbId, Integer ownerId, float[] vec) {
        if (scope == null || kbId == null || vec == null || vec.length == 0) return;
        Entry e = new Entry(scope, kbId, ownerId, vec);
        byKey.put(key(scope, kbId), e);
        byScope.computeIfAbsent(scope, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(e);
    }

    public void remove(String scope, Integer kbId) {
        if (scope == null || kbId == null) return;
        Entry removed = byKey.remove(key(scope, kbId));
        if (removed != null) {
            List<Entry> list = byScope.get(scope);
            if (list != null) list.remove(removed);
        }
    }

    public void clear() {
        byKey.clear();
        byScope.clear();
    }

    public int size() {
        return byKey.size();
    }

    public int size(String scope) {
        List<Entry> l = byScope.get(scope);
        return l == null ? 0 : l.size();
    }

    public boolean has(String scope, Integer kbId) {
        return byKey.containsKey(key(scope, kbId));
    }

    /**
     * 向量近邻检索
     * @param scopes  检索端别（多端时按相似度合并排序）
     * @param ownerId 归属过滤：null=不过滤（管理端）；非null=本人条目 + 机构共享条目(owner=null)
     * @param q       查询向量（已归一化）
     * @param topK    最多返回条数
     */
    public List<Hit> search(List<String> scopes, Integer ownerId, float[] q, int topK) {
        if (scopes == null || scopes.isEmpty() || q == null || q.length == 0) return List.of();
        List<Hit> hits = new ArrayList<>();
        for (String scope : scopes) {
            List<Entry> list = byScope.get(scope);
            if (list == null || list.isEmpty()) continue;
            boolean filterOwner = ownerId != null && !"ADMIN".equals(scope);
            for (Entry e : list) {
                if (e.vec().length != q.length) continue;
                if (filterOwner && e.ownerId() != null && !ownerId.equals(e.ownerId())) continue;
                double dot = 0;
                float[] v = e.vec();
                for (int i = 0; i < v.length; i++) dot += v[i] * q[i];
                if (dot > 0) hits.add(new Hit(e.scope(), e.kbId(), dot));
            }
        }
        hits.sort(Comparator.comparingDouble(Hit::score).reversed());
        return hits.size() > topK ? new ArrayList<>(hits.subList(0, topK)) : hits;
    }
}
