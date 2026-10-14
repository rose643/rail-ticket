package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 新增常用乘车人请求。 */
public record PassengerCreateRequest(
        @NotBlank(message = "乘车人姓名不能为空")
        @Size(max = 64, message = "乘车人姓名不能超过 64 个字符")
        String name,

        @NotBlank(message = "证件类型不能为空")
        @Pattern(
                regexp = "^(ID_CARD|PASSPORT|HK_MACAO_PERMIT|TAIWAN_PERMIT)$",
                message = "不支持的证件类型"
        )
        String idType,

        @NotBlank(message = "证件号码不能为空")
        @Size(max = 64, message = "证件号码不能超过 64 个字符")
        String idNo,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone
) {
}
