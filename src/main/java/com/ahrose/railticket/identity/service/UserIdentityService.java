package com.ahrose.railticket.identity.service;

import com.ahrose.railticket.bean.UserIdentity;
import com.ahrose.railticket.bean.request.UserIdentityBindRequest;
import com.baomidou.mybatisplus.extension.service.IService;

/** 用户实名认证业务接口。 */
public interface UserIdentityService extends IService<UserIdentity> {

    /**
     * 查询当前用户已经绑定的实名认证信息。
     *
     * @param userId 当前登录用户雪花 ID
     * @return {@code UserIdentity}，当前用户的实名认证信息；未认证时抛出业务异常
     */
    UserIdentity getCurrentIdentity(Long userId);

    /**
     * 为当前用户首次绑定实名认证信息。
     * <p>当前项目没有第三方认证渠道，因此作为模拟认证直接标记为已认证。</p>
     *
     * @param userId 当前登录用户雪花 ID
     * @param request 已通过 Controller 参数校验的实名绑定请求
     * @return {@code UserIdentity}，保存后的实名认证信息
     */
    UserIdentity bindIdentity(Long userId, UserIdentityBindRequest request);

    /**
     * 根据有效证件查找对应的实名账号，供创建订单时关联实际乘车人。
     *
     * @param idType 证件类型
     * @param idNo 标准化后的证件号码
     * @return {@code UserIdentity}，已认证身份；没有匹配账号时返回 null
     */
    UserIdentity findVerifiedIdentity(String idType, String idNo);

    /**
     * 根据用户 ID 查询当前有效的实名认证信息，供本人车票查询使用。
     *
     * @param userId 当前登录用户雪花 ID
     * @return {@code UserIdentity}，有效实名认证信息；尚未认证或认证失效时返回 {@code null}
     */
    UserIdentity findVerifiedIdentityByUserId(Long userId);
}
