package com.ahrose.railticket.bean.response;

import com.ahrose.railticket.bean.SysUser;

import java.time.LocalDateTime;

/** 对外返回的用户公开信息，不包含密码摘要。 */
public record UserResponse(
        Long id,
        String username,
        String phone,
        String role,
        Integer status,
        LocalDateTime createdAt
) {

    public static UserResponse from(SysUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
