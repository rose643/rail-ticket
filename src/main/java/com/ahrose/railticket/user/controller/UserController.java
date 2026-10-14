package com.ahrose.railticket.user.controller;

import com.ahrose.railticket.bean.response.UserResponse;
import com.ahrose.railticket.common.result.Result;
import com.ahrose.railticket.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 已登录用户访问自己的账户信息接口。 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 查询当前登录用户的公开信息。 */
    @GetMapping("/me")
    public Result<UserResponse> getCurrentUser(Authentication authentication) {
        return Result.success(UserResponse.from(
                userService.getCurrentUser(authentication.getName())
        ));
    }
}
