package com.ahrose.railticket.bean.response;

import java.time.LocalDateTime;

/** 登录或刷新成功后返回的双 Token 和用户信息。 */
public record LoginResponse(
        String tokenType,
        String accessToken,
        LocalDateTime accessTokenExpiresAt,
        String refreshToken,
        LocalDateTime refreshTokenExpiresAt,
        UserResponse user
) {
}
