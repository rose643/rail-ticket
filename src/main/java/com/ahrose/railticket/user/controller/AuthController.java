package com.ahrose.railticket.user.controller;

import com.ahrose.railticket.bean.request.UserLoginRequest;
import com.ahrose.railticket.bean.request.UserRegisterRequest;
import com.ahrose.railticket.bean.request.TokenRefreshRequest;
import com.ahrose.railticket.bean.request.UserLogoutRequest;
import com.ahrose.railticket.bean.response.CaptchaResponse;
import com.ahrose.railticket.bean.response.LoginResponse;
import com.ahrose.railticket.bean.response.UserResponse;
import com.ahrose.railticket.common.enums.CaptchaScene;
import com.ahrose.railticket.common.result.Result;
import com.ahrose.railticket.user.service.CaptchaService;
import com.ahrose.railticket.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用户注册和登录接口。 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final CaptchaService captchaService;

    public AuthController(UserService userService, CaptchaService captchaService) {
        this.userService = userService;
        this.captchaService = captchaService;
    }

    /** 按 LOGIN 或 REGISTER 场景获取一次性图形验证码，有效期为 2 分钟。 */
    @GetMapping("/captcha")
    public Result<CaptchaResponse> getCaptcha(@RequestParam CaptchaScene scene) {
        return Result.success(captchaService.generateCaptcha(scene));
    }

    /** 注册普通用户，注册成功后返回不含密码摘要的用户信息。 */
    @PostMapping("/register")
    public Result<UserResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        return Result.success(UserResponse.from(userService.register(request)));
    }

    /** 校验图形验证码、用户名和密码，成功后返回双 Token。 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        return Result.success(userService.login(request));
    }

    /** 使用一次性 Refresh Token 轮换获取一组新的双 Token。 */
    @PostMapping("/refresh")
    public Result<LoginResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return Result.success(userService.refreshToken(request));
    }

    /** 撤销 Refresh Token 并退出当前设备的登录。 */
    @PostMapping("/logout")
    public Result<Void> logout(@Valid @RequestBody UserLogoutRequest request) {
        userService.logout(request);
        return Result.success();
    }
}
