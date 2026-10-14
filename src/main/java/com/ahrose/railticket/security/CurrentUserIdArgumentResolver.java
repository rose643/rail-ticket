package com.ahrose.railticket.security;

import com.ahrose.railticket.common.exception.BusinessException;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** 从 Spring Security 已验证的 JWT 中解析 uid 声明。 */
@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录或登录已失效");
        }

        Jwt jwt = extractJwt(authentication);
        Object userId = jwt.getClaim("uid");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        if (userId instanceof String value) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException ignored) {
                // 继续抛出统一的认证信息异常。
            }
        }
        throw new BusinessException(401, "登录信息无效，请重新登录");
    }

    private Jwt extractJwt(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        throw new BusinessException(401, "登录信息无效，请重新登录");
    }
}
