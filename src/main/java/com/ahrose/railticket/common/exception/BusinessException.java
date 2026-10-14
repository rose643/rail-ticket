package com.ahrose.railticket.common.exception;

import lombok.Getter;

/** 可预期业务错误，例如余票不足、订单状态不允许变更。 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
