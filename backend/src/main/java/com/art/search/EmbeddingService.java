package com.art.search;

/**
 * 向量化服务：把文本编码为定长稠密向量，供向量检索引擎做余弦相似度召回。
 * 实现分两类：
 *  - {@link LocalHashEmbeddingService}   本地哈希向量（TF-IDF 加权 + 特征哈希），零外部依赖、毫秒级；
 *  - {@link RemoteEmbeddingService}      远程 Embedding 模型（OpenAI 兼容 /embeddings），语义最强。
 * 由 {@link KbEmbeddingService} 按配置择优选择，远程不可用时自动降级本地。
 * @author liukang&&heyan
 */
public interface EmbeddingService {

    /** 向量模型标识（落库用于判断是否需要重算向量） */
    String tag();

    /** 向量维度 */
    int dim();

    /** 文本 → 向量（已 L2 归一化，余弦相似度即点积） */
    float[] embed(String text);
}
