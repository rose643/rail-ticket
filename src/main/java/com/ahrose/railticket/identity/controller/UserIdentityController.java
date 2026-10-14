package com.ahrose.railticket.identity.controller;

import com.ahrose.railticket.bean.request.UserIdentityBindRequest;
import com.ahrose.railticket.bean.response.UserIdentityResponse;
import com.ahrose.railticket.common.result.Result;
import com.ahrose.railticket.identity.service.UserIdentityService;
import com.ahrose.railticket.security.CurrentUserId;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前登录用户的实名认证接口。 */
@RestController
@RequestMapping("/api/users/me/identity")
public class UserIdentityController {

    private final UserIdentityService userIdentityService;

    public UserIdentityController(UserIdentityService userIdentityService) {
        this.userIdentityService = userIdentityService;
    }

    /** 查询当前用户的实名认证信息，返回的证件号码已经脱敏。 */
    @GetMapping
    public Result<UserIdentityResponse> getCurrentIdentity(@CurrentUserId Long userId) {
        return Result.success(UserIdentityResponse.from(
                userIdentityService.getCurrentIdentity(userId)
        ));
    }

    /** 首次绑定实名认证信息；当前项目使用模拟认证，不允许重复绑定或修改。 */
    @PostMapping
    public Result<UserIdentityResponse> bindIdentity(
            @CurrentUserId Long userId,
            @Valid @RequestBody UserIdentityBindRequest request
    ) {
        return Result.success(UserIdentityResponse.from(
                userIdentityService.bindIdentity(userId, request)
        ));
    }
}
