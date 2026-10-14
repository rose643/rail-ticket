package com.ahrose.railticket.user.service.impl;

import com.ahrose.railticket.bean.response.CaptchaResponse;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.common.enums.CaptchaScene;
import com.ahrose.railticket.user.service.CaptchaService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

/** 使用 Redis 分场景保存一次性注册、登录图形验证码。 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    /** 验证码存储前缀；完整格式：auth:captcha:{场景}:{captchaId}，值为正确验证码。 */
    private static final String REDIS_KEY_PREFIX = "auth:captcha:";
    private static final String CODE_CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 4;
    private static final int IMAGE_WIDTH = 120;
    private static final int IMAGE_HEIGHT = 40;

    /** 验证码在Redis中的有效期；验证码读取时会被原子删除，因此只能使用一次。 */
    private static final Duration EXPIRE_TIME = Duration.ofMinutes(2);

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public CaptchaServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public CaptchaResponse generateCaptcha(CaptchaScene scene) {
        checkScene(scene);
        String captchaId = UUID.randomUUID().toString();
        String captchaCode = generateCode();
        redisTemplate.opsForValue().set(redisKey(scene, captchaId), captchaCode, EXPIRE_TIME);

        return new CaptchaResponse(
                captchaId,
                "data:image/png;base64," + renderImage(captchaCode),
                EXPIRE_TIME.toSeconds()
        );
    }

    @Override
    public void validateCaptcha(CaptchaScene scene, String captchaId, String captchaCode) {
        checkScene(scene);
        String expectedCode = redisTemplate.opsForValue().getAndDelete(redisKey(scene, captchaId));
        if (expectedCode == null) {
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }
        if (!expectedCode.equals(captchaCode.trim().toUpperCase(Locale.ROOT))) {
            throw new BusinessException(400, "验证码错误，请重新获取");
        }
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int index = 0; index < CODE_LENGTH; index++) {
            code.append(CODE_CHARACTERS.charAt(secureRandom.nextInt(CODE_CHARACTERS.length())));
        }
        return code.toString();
    }

    private String renderImage(String code) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(245, 247, 250));
            graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

            for (int index = 0; index < 6; index++) {
                graphics.setColor(randomLightColor());
                graphics.drawLine(
                        secureRandom.nextInt(IMAGE_WIDTH),
                        secureRandom.nextInt(IMAGE_HEIGHT),
                        secureRandom.nextInt(IMAGE_WIDTH),
                        secureRandom.nextInt(IMAGE_HEIGHT)
                );
            }

            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            for (int index = 0; index < code.length(); index++) {
                graphics.setColor(randomDarkColor());
                graphics.drawString(String.valueOf(code.charAt(index)), 13 + index * 25, 29);
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException exception) {
            throw new BusinessException(500, "验证码图片生成失败");
        }
    }

    private Color randomLightColor() {
        return new Color(
                150 + secureRandom.nextInt(106),
                150 + secureRandom.nextInt(106),
                150 + secureRandom.nextInt(106)
        );
    }

    private Color randomDarkColor() {
        return new Color(
                secureRandom.nextInt(120),
                secureRandom.nextInt(120),
                secureRandom.nextInt(120)
        );
    }

    private String redisKey(CaptchaScene scene, String captchaId) {
        return REDIS_KEY_PREFIX
                + scene.name().toLowerCase(Locale.ROOT)
                + ":"
                + captchaId.trim();
    }

    private void checkScene(CaptchaScene scene) {
        if (scene == null) {
            throw new BusinessException(400, "验证码场景不能为空");
        }
    }
}
