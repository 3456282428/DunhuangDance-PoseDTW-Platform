package com.art.search;

import com.art.entity.AiModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量化门面（混合策略）：优先使用远程 Embedding 模型（真语义），
 * 未配置或调用异常时自动降级到本地哈希向量（零依赖、毫秒级），
 * 保证「有远程用远程、没有也能跑」的生产级可用性。
 * @author liukang&&heyan
 */
@Slf4j
@Service
public class KbEmbeddingService {

    /** 远程模型探测缓存时长（毫秒），避免每次检索都查库 */
    private static final long PROBE_TTL_MS = 60_000;

    private final RemoteEmbeddingService remote;
    private final LocalHashEmbeddingService local;

    private volatile AiModel remoteModel;
    private volatile long probedAt = 0L;
    /** 远程连续失败后的熔断截止时间 */
    private volatile long brokenUntil = 0L;

    public KbEmbeddingService(RemoteEmbeddingService remote, LocalHashEmbeddingService local) {
        this.remote = remote;
        this.local = local;
    }

    public LocalHashEmbeddingService local() {
        return local;
    }

    /** 当前生效的远程模型（未配置返回 null） */
    public AiModel remoteModel() {
        long now = System.currentTimeMillis();
        if (now - probedAt > PROBE_TTL_MS) {
            try {
                remoteModel = remote.pick();
            } catch (Exception e) {
                remoteModel = null;
            }
            probedAt = now;
        }
        return remoteModel;
    }

    /** 是否正使用远程语义向量 */
    public boolean isRemoteActive() {
        return remoteModel() != null && System.currentTimeMillis() > brokenUntil;
    }

    /** 当前生效模型标识（落库用于判断向量是否需要重算） */
    public String activeTag() {
        AiModel m = isRemoteActive() ? remoteModel() : null;
        return m == null ? local.tag() : remote.tag(m);
    }

    /** 当前生效维度（未实际调用远程时未知，按本地维度返回，实际维度由向量自描述） */
    public int activeDim() {
        return local.dim();
    }

    public float[] embed(String text) {
        AiModel m = remoteModel();
        if (m != null && System.currentTimeMillis() > brokenUntil) {
            try {
                return remote.embed(m, text);
            } catch (Exception e) {
                markBroken(e);
            }
        }
        return local.embed(text);
    }

    public List<float[]> embedBatch(List<String> texts) {
        AiModel m = remoteModel();
        if (m != null && System.currentTimeMillis() > brokenUntil) {
            try {
                return remote.embedBatch(m, texts);
            } catch (Exception e) {
                markBroken(e);
            }
        }
        List<float[]> out = new java.util.ArrayList<>(texts.size());
        for (String t : texts) out.add(local.embed(t));
        return out;
    }

    private void markBroken(Exception e) {
        brokenUntil = System.currentTimeMillis() + 5 * 60_000L;
        log.warn("[向量检索] 远程 Embedding 调用失败，5分钟内降级本地向量：{}", e.getMessage());
    }
}
