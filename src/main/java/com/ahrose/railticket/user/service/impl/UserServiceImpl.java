package com.ahrose.railticket.user.service.impl;

import com.ahrose.railticket.bean.SysUser;
import com.ahrose.railticket.bean.request.UserLoginRequest;
import com.ahrose.railticket.bean.request.TokenRefreshRequest;
import com.ahrose.railticket.bean.request.UserLogoutRequest;
import com.ahrose.railticket.bean.request.UserRegisterRequest;
import com.ahrose.railticket.bean.response.LoginResponse;
import com.ahrose.railticket.bean.response.UserResponse;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.common.enums.CaptchaScene;
import com.ahrose.railticket.security.AuthenticatedUser;
import com.ahrose.railticket.security.JwtTokenService;
import com.ahrose.railticket.security.RefreshTokenService;
import com.ahrose.railticket.user.mapper.UserMapper;
import com.ahrose.railticket.user.service.CaptchaService;
import com.ahrose.railticket.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

/** 用户注册、登录和当前用户查询业务实现。 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final CaptchaService captchaService;
    private final RefreshTokenService refreshTokenService;

    public UserServiceImpl(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            CaptchaService captchaService,
            RefreshTokenService refreshTokenService
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.captchaService = captchaService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser register(UserRegisterRequest request) {
        if (request == null) {
            throw new BusinessException(400, "注册信息不能为空");
        }
        captchaService.validateCaptcha(
                CaptchaScene.REGISTER,
                request.captchaId(),
                request.captchaCode()
        );
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(400, "密码内容过长");
        }

        String username = normalizeUsername(request.username());
        String phone = normalizePhone(request.phone());
        checkUsernameAndPhoneAvailable(username, phone);

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(phone);
        user.setRole("USER");
        user.setStatus(1);

        try {
            int rows = userMapper.insert(user);
            if (rows == 0) {
                throw new BusinessException(500, "用户注册失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "用户名或手机号已存在");
        }

        SysUser saved = userMapper.selectById(user.getId());
        return saved == null ? user : saved;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(UserLoginRequest request) {
        if (request == null) {
            throw new BusinessException(400, "登录信息不能为空");
        }
        captchaService.validateCaptcha(
                CaptchaScene.LOGIN,
                request.captchaId(),
                request.captchaCode()
        );

        String username = normalizeUsername(request.username());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password())
            );
        } catch (AuthenticationException exception) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (!(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new BusinessException(500, "登录认证结果异常");
        }
        SysUser user = userMapper.selectById(principal.getUserId());
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        return issueTokenPair(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse refreshToken(TokenRefreshRequest request) {
        Long userId = refreshTokenService.consume(request.refreshToken());
        SysUser user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(401, "用户不存在或已被禁用，请重新登录");
        }
        return issueTokenPair(user);
    }

    @Override
    public void logout(UserLogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    @Override
    @Transactional(readOnly = true)
    public SysUser getCurrentUser(String username) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, normalizeUsername(username))
        );
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(403, "用户已被禁用");
        }
        return user;
    }

    /** 校验用户名和手机号均未被注册。 */
    private void checkUsernameAndPhoneAvailable(String username, String phone) {
        long usernameCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
        if (usernameCount > 0) {
            throw new BusinessException(409, "用户名已存在");
        }

        if (phone == null) {
            return;
        }
        long phoneCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getPhone, phone)
        );
        if (phoneCount > 0) {
            throw new BusinessException(409, "手机号已存在");
        }
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phone.trim();
    }

    private LoginResponse issueTokenPair(SysUser user) {
        JwtTokenService.IssuedToken accessToken = jwtTokenService.issueAccessToken(user);
        RefreshTokenService.IssuedToken refreshToken = refreshTokenService.issue(user);
        return new LoginResponse(
                "Bearer",
                accessToken.value(),
                toLocalDateTime(accessToken.expiresAt()),
                refreshToken.value(),
                toLocalDateTime(refreshToken.expiresAt()),
                UserResponse.from(user)
        );
    }

    private LocalDateTime toLocalDateTime(java.time.Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
