package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 使用 Refresh Token 刷新双 Token 的请求。 */
public record TokenRefreshRequest(
        @NotBlank(message = "刷新令牌不能为空")
        @Size(min = 40, max = 128, message = "刷新令牌格式不正确")
        String refreshToken
) {
}
