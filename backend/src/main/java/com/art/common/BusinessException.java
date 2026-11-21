package com.art.common;

import lombok.Getter;

/**
 * 业务异常，用于业务逻辑错误抛出，由全局异常处理器捕获
  * @author liukang&&heyan
 */
@Getter
public class BusinessException extends RuntimeException {

    private Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}