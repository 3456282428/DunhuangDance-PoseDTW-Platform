package com.art.dto;

import lombok.Data;

/**
 * 豆包视频模型调用请求参数规范（预留）
 * 对接火山引擎方舟（Ark）视频生成API（doubao-seedance系列），本期仅定义接口与参数，
 * 不实现具体视频生成业务逻辑；模型在ai_model表中默认DISABLED，启用后可无缝对接。
  * @author liukang&&heyan
 */
@Data
public class AiVideoRequest {

    /** 调用端别：ADMIN/TEACHER/STUDENT */
    private String scope;

    /** 生成内容描述（必填，最长500字） */
    private String prompt;

    /**
     * 分辨率（可选）：480p/720p/1080p，默认720p
     */
    private String resolution;

    /**
     * 视频时长（秒，可选）：5/10，默认5
     */
    private Integer durationSeconds;

    /**
     * 帧率（可选，fps）：默认24
     */
    private Integer fps;

    /** 参考视频URL（可选，首帧参考，需公网可访问） */
    private String refVideoUrl;

    /** 指定ai_model中豆包视频模型ID（可选，缺省取唯一启用的VIDEO类型模型） */
    private Integer modelId;
}
