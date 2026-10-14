package com.ahrose.railticket.bean.response;

/** 获取登录验证码后返回的验证码标识和 Base64 图片。 */
public record CaptchaResponse(
        String captchaId,
        String imageBase64,
        long expiresInSeconds
) {
}
