package com.ahrose.railticket.user.service;

import com.ahrose.railticket.bean.SysUser;
import com.ahrose.railticket.bean.request.TokenRefreshRequest;
import com.ahrose.railticket.bean.request.UserLoginRequest;
import com.ahrose.railticket.bean.request.UserLogoutRequest;
import com.ahrose.railticket.bean.request.UserRegisterRequest;
import com.ahrose.railticket.bean.response.LoginResponse;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UserServiceTest {

    private static final String CAPTCHA_CODE = "ABCD";

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String suffix;
    private final Set<String> captchaKeys = new HashSet<>();

    /** 每个测试执行前生成独立用户名后缀。 */
    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /** 每个测试结束后清理可能尚未消费的验证码 Redis Key。 */
    @AfterEach
    void clearCaptchaKeys() {
        redisTemplate.delete(captchaKeys);
    }

    /** 验证注册会统一用户名大小写、使用 BCrypt 保存密码并创建普通用户。 */
    @Test
    void register_shouldNormalizeUsernameAndEncodePassword() {
        String captchaId = prepareCaptcha("register");
        String rawPassword = "TestPass123";

        SysUser user = userService.register(new UserRegisterRequest(
                "User_" + suffix.toUpperCase(),
                rawPassword,
                "13800138000",
                captchaId,
                CAPTCHA_CODE.toLowerCase()
        ));

        assertNotNull(user.getId());
        assertEquals(("user_" + suffix).toLowerCase(), user.getUsername());
        assertEquals("USER", user.getRole());
        assertEquals(1, user.getStatus());
        assertNotEquals(rawPassword, user.getPasswordHash());
        assertTrue(passwordEncoder.matches(rawPassword, user.getPasswordHash()));
        printUser("用户注册测试", user);
    }

    /** 验证重复用户名即使使用不同大小写也会被拒绝。 */
    @Test
    void register_whenUsernameRepeatedWithDifferentCase_shouldThrowConflict() {
        userService.register(new UserRegisterRequest(
                "user_" + suffix,
                "TestPass123",
                null,
                prepareCaptcha("register"),
                CAPTCHA_CODE
        ));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.register(new UserRegisterRequest(
                        ("USER_" + suffix).toUpperCase(),
                        "OtherPass123",
                        null,
                        prepareCaptcha("register"),
                        CAPTCHA_CODE
                ))
        );

        assertEquals(409, exception.getCode());
        assertEquals("用户名已存在", exception.getMessage());
        System.out.println("========== 重复用户名注册测试 ==========");
        System.out.println("异常信息：" + exception.getMessage());
        System.out.println("测试结果：通过");
    }

    /** 验证登录会签发 Access Token 和 Refresh Token，刷新后旧 Refresh Token 立即失效。 */
    @Test
    void loginAndRefresh_shouldRotateTokenPair() {
        String username = "login_" + suffix;
        String password = "TestPass123";
        insertUser(username, password);

        LoginResponse login = userService.login(new UserLoginRequest(
                username,
                password,
                prepareCaptcha("login"),
                CAPTCHA_CODE
        ));
        LoginResponse refreshed = userService.refreshToken(
                new TokenRefreshRequest(login.refreshToken())
        );

        assertFalse(login.accessToken().isBlank());
        assertFalse(login.refreshToken().isBlank());
        assertNotEquals(login.refreshToken(), refreshed.refreshToken());
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.refreshToken(new TokenRefreshRequest(login.refreshToken()))
        );
        assertEquals(401, exception.getCode());

        userService.logout(new UserLogoutRequest(refreshed.refreshToken()));
        System.out.println("========== 登录与双 Token 轮换测试 ==========");
        System.out.println("用户 ID：" + login.user().id());
        System.out.println("Access Token 已生成：" + (login.accessToken() != null));
        System.out.println("Refresh Token 已完成轮换：true");
        System.out.println("测试结果：通过");
    }

    private String prepareCaptcha(String scene) {
        String captchaId = UUID.randomUUID().toString();
        String key = "auth:captcha:" + scene + ":" + captchaId;
        captchaKeys.add(key);
        redisTemplate.opsForValue().set(key, CAPTCHA_CODE, Duration.ofMinutes(2));
        return captchaId;
    }

    private SysUser insertUser(String username, String password) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    private void printUser(String testName, SysUser user) {
        System.out.println("========== " + testName + " ==========");
        System.out.printf(
                "userId=%d, username=%s, role=%s, status=%d%n",
                user.getId(), user.getUsername(), user.getRole(), user.getStatus()
        );
        System.out.println("测试结果：通过");
    }
}
