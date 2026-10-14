package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 当前用户绑定实名认证信息的请求。 */
public record UserIdentityBindRequest(
        @NotBlank(message = "真实姓名不能为空")
        @Size(max = 64, message = "真实姓名不能超过 64 个字符")
        String realName,

        @NotBlank(message = "证件类型不能为空")
        @Pattern(
                regexp = "^(ID_CARD|PASSPORT|HK_MACAO_PERMIT|TAIWAN_PERMIT)$",
                message = "不支持的证件类型"
        )
        String idType,

        @NotBlank(message = "证件号码不能为空")
        @Size(min = 5, max = 64, message = "证件号码长度必须为 5 到 64 个字符")
        String idNo
) {
}
