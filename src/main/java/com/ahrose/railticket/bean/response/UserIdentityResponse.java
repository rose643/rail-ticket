package com.ahrose.railticket.bean.response;

import com.ahrose.railticket.bean.UserIdentity;
import com.ahrose.railticket.common.util.SensitiveDataMasker;

import java.time.LocalDateTime;

/** 用户实名认证响应，证件号码经过脱敏。 */
public record UserIdentityResponse(
        Long id,
        String realName,
        String idType,
        String idNo,
        Integer status,
        LocalDateTime verifiedAt
) {

    public static UserIdentityResponse from(UserIdentity identity) {
        return new UserIdentityResponse(
                identity.getId(),
                identity.getRealName(),
                identity.getIdType(),
                SensitiveDataMasker.maskIdNo(identity.getIdNo()),
                identity.getStatus(),
                identity.getVerifiedAt()
        );
    }
}
