package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 用户名和密码登录请求。 */
public record UserLoginRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名不能超过 64 个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(max = 64, message = "密码不能超过 64 个字符")
        String password,

        @NotBlank(message = "验证码标识不能为空")
        @Size(max = 64, message = "验证码标识格式不正确")
        String captchaId,

        @NotBlank(message = "验证码不能为空")
        @Size(min = 4, max = 4, message = "验证码必须为 4 个字符")
        String captchaCode
) {
}
