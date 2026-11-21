package com.art.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识条目向量（三端知识库共用一张向量表，kb_scope 区分端别）
  * @author liukang&&heyan
 */
@Data
public class KbDocVector implements Serializable {

    private Long id;
    private String kbScope;
    private Integer kbId;
    private Integer ownerId;
    private Integer dim;
    /** 向量模型标识（本地 local-hash-256 / 远程 remote:模型名），用于判断是否需要重算 */
    private String model;
    /** 文本指纹（分词词元序列 MD5，内容变更则重算向量） */
    private String contentHash;
    /** 向量（逗号分隔 float，已 L2 归一化） */
    private String vec;
    private LocalDateTime updateTime;
}
