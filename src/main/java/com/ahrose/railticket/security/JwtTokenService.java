package com.ahrose.railticket.security;

import com.ahrose.railticket.bean.SysUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/** 签发项目自身使用的 JWT 访问令牌。 */
@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long accessExpireMinutes;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.access-expire-minutes}") long accessExpireMinutes
    ) {
        if (accessExpireMinutes <= 0) {
            throw new IllegalArgumentException("JWT Access Token 有效期必须大于 0 分钟");
        }
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessExpireMinutes = accessExpireMinutes;
    }

    /** 为已认证用户签发访问令牌。 */
    public IssuedToken issueAccessToken(SysUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(accessExpireMinutes, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getUsername())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("uid", user.getId())
                .claim("roles", List.of("ROLE_" + user.getRole()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    /** 已签发的访问令牌及其绝对过期时间。 */
    public record IssuedToken(String value, Instant expiresAt) {
    }
}
