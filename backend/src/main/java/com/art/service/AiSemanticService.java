package com.art.service;

import java.util.Map;

/**
 * 豆包语义增强服务（通道二·AI智能教练）：
 * 双通道架构中与通道一（MediaPipe+DTW精确评分）互补的语义层——
 * 将教师/学生视频关键帧与量化评分上下文送入豆包多模态模型，生成：
 * 1. 动作文字描述与发力点标注
 * 2. 节奏与音乐的视听一致性判断
 * 3. 针对学生动作的自然语言改进建议
 * 结果持久化至ai_report.semantic节点，随评分报告一并透出。
  * @author liukang&&heyan
 */
public interface AiSemanticService {

    /**
     * 生成/刷新AI教练语义点评（同步调用，结果落库ai_report.semantic）
     *
     * @param submissionId 提交记录ID
     * @param callerScope  调用端别（KB检索scope隔离）
     * @param force        false时已有分析结果直接返回缓存
     * @return {status: SUCCESS/FAILED, cached, semantic(完整语义结果), message}
     */
    Map<String, Object> enhanceSemantics(Integer submissionId, String callerScope, boolean force);
}
