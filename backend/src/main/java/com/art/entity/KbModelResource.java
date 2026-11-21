package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 3D模型资源（知识条目适配推荐的模型资源库）
 * @author liukang&&heyan
 */
@Data
public class KbModelResource implements Serializable {

    private Integer modelId;
    private String modelName;
    /** 模型类别：DANCE舞蹈/MUSIC音乐/ART美术/CALLIGRAPHY书法/SCENE场景/PROP道具 */
    private String category;
    /** 适配标签（逗号分隔，用于条目匹配推荐） */
    private String tags;
    private String format;
    private String fileUrl;
    private String previewUrl;
    private String description;
    /** 状态：1启用 0停用 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
