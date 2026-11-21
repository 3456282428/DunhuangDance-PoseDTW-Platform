package com.art.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 返回结果封装
  * @author liukang&&heyan
 */
@Data
public class Result<T> implements Serializable {

    private Integer code;      // 状态码：200 成功，其他为失败
    private String message;    // 提示信息
    private T data;            // 数据体

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功（无数据）
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    // 成功（带数据）
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    // 成功（自定义消息）
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    // 失败（默认错误码）
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    // 失败（自定义状态码）
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}