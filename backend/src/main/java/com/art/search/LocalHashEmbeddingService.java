package com.art.search;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 本地哈希向量：把「分词词元 + 字符 2-gram」两类特征经特征哈希（feature hashing）映射到 256 维，
 * 权重采用 TF-IDF（IDF 由知识库语料统计得出），向量 L2 归一化后余弦相似度即点积。
 * 特点：零外部依赖、离线可用、单条编码耗时 < 1ms；对「排课/排课表」「张老师/张老师课程」这类
 * 词面相近的内容召回效果好，是远程 Embedding 不可用时的生产级兜底。
 * @author liukang&&heyan
 */
@Component
public class LocalHashEmbeddingService implements EmbeddingService {

    /** 向量维度 */
    public static final int DIM = 256;

    private final ChineseTokenizer tokenizer;
    /** 语料 IDF 表：token -> idf（由向量库构建时统计注入） */
    private final AtomicReference<Map<String, Double>> idfRef = new AtomicReference<>(Map.of());

    public LocalHashEmbeddingService(ChineseTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    @Override
    public String tag() {
        return "local-hash-" + DIM;
    }

    @Override
    public int dim() {
        return DIM;
    }

    /** 用知识库语料统计 IDF（df=含该词元的条目数，total=条目总数） */
    public void updateIdf(Map<String, Integer> df, int total) {
        if (total <= 0) return;
        Map<String, Double> m = new HashMap<>(df.size() * 2);
        for (Map.Entry<String, Integer> e : df.entrySet()) {
            // 平滑 IDF，最小 1.0，避免未见词元被完全压制
            double idf = Math.log((total + 1.0) / (e.getValue() + 1.0)) + 1.0;
            m.put(e.getKey(), idf);
        }
        idfRef.set(m);
    }

    public double idf(String token) {
        Double v = idfRef.get().get(token);
        if (v != null) return v;
        // 未统计到：按最稀有处理
        return Math.log((idfRef.get().size() + 1.0) / 1.0) + 1.0;
    }

    @Override
    public float[] embed(String text) {
        float[] v = new float[DIM];
        if (text == null || text.isBlank()) return v;

        // 1) 词元特征：TF-IDF 加权
        List<String> tokens = tokenizer.tokens(text);
        Map<String, Integer> tf = new HashMap<>();
        for (String t : tokens) tf.merge(t, 1, Integer::sum);
        for (Map.Entry<String, Integer> e : tf.entrySet()) {
            double w = idf(e.getKey()) * (1.0 + Math.log(e.getValue()));
            addFeature(v, e.getKey(), w);
        }
        // 2) 字符 2-gram 特征：捕捉词形相近（权重减半，避免压制词元特征）
        for (String g : tokenizer.bigrams(text)) {
            addFeature(v, "#" + g, idf(g) * 0.5);
        }
        normalize(v);
        return v;
    }

    /** 特征哈希：murmur3 风格散列，取 31 位定桶 + 最高位定符号（降低碰撞偏置） */
    private void addFeature(float[] v, String feature, double weight) {
        int h = murmur(feature);
        int idx = (h & 0x7fffffff) % DIM;
        double sign = ((h >>> 31) & 1) == 0 ? 1.0 : -1.0;
        v[idx] += (float) (sign * weight);
    }

    private int murmur(String s) {
        int h = 0x9747b28c;
        for (int i = 0; i < s.length(); i++) {
            int k = s.charAt(i);
            k *= 0xcc9e2d51;
            k = Integer.rotateLeft(k, 15);
            k *= 0x1b873593;
            h ^= k;
            h = Integer.rotateLeft(h, 13);
            h = h * 5 + 0xe6546b64;
        }
        h ^= s.length() * 2;
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }

    private void normalize(float[] v) {
        double norm = 0;
        for (float x : v) norm += x * x;
        if (norm <= 0) return;
        float f = (float) (1.0 / Math.sqrt(norm));
        for (int i = 0; i < v.length; i++) v[i] *= f;
    }
}
