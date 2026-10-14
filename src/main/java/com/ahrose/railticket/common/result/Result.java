package com.ahrose.railticket.common.result;

import lombok.Data;

/** 所有 HTTP 接口的统一返回结构。 */
@Data
public class Result<T> {

    /** HTTP 状态码，例如 200、400、401、403、404、500。 */
    private Integer code;

    /** 对调用方展示的结果描述。 */
    private String message;

    /** 成功时返回的业务数据；失败时为 null。 */
    private T data;

    /** 成功返回（带数据）。 */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /** 成功返回（不带数据）。 */
    public static Result<Void> success() {
        return success(null);
    }

    /** 失败返回。 */
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
