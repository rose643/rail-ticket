package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 普通用户注册请求。 */
public record UserRegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 64, message = "用户名长度必须为 4 到 64 个字符")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度必须为 8 到 64 个字符")
        String password,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "验证码标识不能为空")
        @Size(max = 64, message = "验证码标识格式不正确")
        String captchaId,

        @NotBlank(message = "验证码不能为空")
        @Size(min = 4, max = 4, message = "验证码必须为 4 个字符")
        String captchaCode
) {
}
