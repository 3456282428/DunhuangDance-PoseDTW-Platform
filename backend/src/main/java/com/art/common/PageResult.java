package com.art.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询返回结果
  * @author liukang&&heyan
 */
@Data
public class PageResult<T> implements Serializable {

    private Long total;        // 总记录数
    private List<T> rows;      // 当前页数据
    private Integer pageNum;   // 当前页码
    private Integer pageSize;  // 每页大小

    public PageResult() {
    }

    public PageResult(Long total, List<T> rows, Integer pageNum, Integer pageSize) {
        this.total = total;
        this.rows = rows;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public static <T> PageResult<T> of(Long total, List<T> rows, Integer pageNum, Integer pageSize) {
        return new PageResult<>(total, rows, pageNum, pageSize);
    }
}