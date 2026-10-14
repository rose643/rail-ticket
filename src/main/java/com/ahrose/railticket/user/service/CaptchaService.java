package com.ahrose.railticket.user.service;

import com.ahrose.railticket.bean.response.CaptchaResponse;
import com.ahrose.railticket.common.enums.CaptchaScene;

/** 注册、登录图形验证码业务接口。 */
public interface CaptchaService {

    /**
     * 生成一次性图形验证码，并将正确答案暂存在 Redis。
     *
     * @param scene 验证码使用场景
     * @return {@code CaptchaResponse}，包含验证码标识、Base64 图片和有效秒数
     */
    CaptchaResponse generateCaptcha(CaptchaScene scene);

    /**
     * 校验并立即删除一次性验证码，无论正确与否都不能再次使用。
     *
     * @param scene 验证码使用场景，必须与生成时一致
     * @param captchaId 获取验证码时返回的唯一标识
     * @param captchaCode 用户从图片中输入的验证码
     */
    void validateCaptcha(CaptchaScene scene, String captchaId, String captchaCode);
}
