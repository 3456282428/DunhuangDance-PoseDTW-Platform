package com.art.service;

import java.util.Map;

/**
 * 舞蹈AI智能评分服务（对接Python FastAPI评分服务）
  * @author liukang&&heyan
 */
public interface DanceAiService {

    /**
     * 触发AI评分（异步）：落库"评分中"状态后提交异步任务。
     * 已有留存评分结果且force=false时直接返回现有结果，不重复评分。
     */
    Map<String, Object> triggerScore(Integer submissionId, boolean force);

    /**
     * 查询AI评分结果
     */
    Map<String, Object> getAiResult(Integer submissionId);
}
