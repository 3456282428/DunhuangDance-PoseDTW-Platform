package com.art.vo;

import lombok.Data;

@Data
/**
 * @author liukang&&heyan
 */
public class TodoItemVO {

    private Integer id;
    private String type;        // 作业批改 / 反馈回复 / 调课申请等
    private String title;
    private String description;
    private String status;      // 待处理 / 已处理
    private String createTime;
}
