package com.ahrose.railticket.user.service;

import com.ahrose.railticket.bean.SysUser;
import com.ahrose.railticket.bean.request.UserLoginRequest;
import com.ahrose.railticket.bean.request.TokenRefreshRequest;
import com.ahrose.railticket.bean.request.UserLogoutRequest;
import com.ahrose.railticket.bean.request.UserRegisterRequest;
import com.ahrose.railticket.bean.response.LoginResponse;

/** 用户注册、登录和当前用户查询业务接口。 */
public interface UserService {

    /**
     * 注册普通用户，密码由实现类加密，角色固定为 USER，初始状态为启用。
     *
     * @param request 已通过 Controller 参数校验的用户注册请求
     * @return {@code SysUser}，注册成功后的用户，包含生成的雪花 ID且密码摘要不得对外返回
     */
    SysUser register(UserRegisterRequest request);

    /**
     * 使用用户名和密码登录，并签发 Access Token 和 Refresh Token。
     *
     * @param request 已通过 Controller 参数校验的登录请求
     * @return {@code LoginResponse}，包含双 Token、各自过期时间和用户公开信息
     */
    LoginResponse login(UserLoginRequest request);

    /**
     * 消费旧 Refresh Token，并轮换签发一组新的双 Token。
     *
     * @param request 已通过 Controller 参数校验的刷新请求
     * @return {@code LoginResponse}，包含新双 Token、各自过期时间和用户公开信息
     */
    LoginResponse refreshToken(TokenRefreshRequest request);

    /**
     * 撤销 Refresh Token，使其不能继续刷新 Access Token。
     *
     * @param request 已通过 Controller 参数校验的退出登录请求
     */
    void logout(UserLogoutRequest request);

    /**
     * 根据 Spring Security 中已认证的用户名查询当前用户。
     *
     * @param username 当前认证用户的用户名
     * @return {@code SysUser}，当前用户完整实体；不存在时由实现类抛出业务异常
     */
    SysUser getCurrentUser(String username);
}
