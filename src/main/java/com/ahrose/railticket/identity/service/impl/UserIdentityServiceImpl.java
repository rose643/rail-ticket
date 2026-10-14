package com.ahrose.railticket.identity.service.impl;

import com.ahrose.railticket.bean.UserIdentity;
import com.ahrose.railticket.bean.request.UserIdentityBindRequest;
import com.ahrose.railticket.common.enums.PassengerIdType;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.identity.mapper.UserIdentityMapper;
import com.ahrose.railticket.identity.service.UserIdentityService;
import com.ahrose.railticket.order.mapper.TicketOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

/** 用户实名认证业务实现。 */
@Service
public class UserIdentityServiceImpl extends ServiceImpl<UserIdentityMapper, UserIdentity>
        implements UserIdentityService {

    private final UserIdentityMapper userIdentityMapper;
    private final TicketOrderMapper ticketOrderMapper;

    public UserIdentityServiceImpl(
            UserIdentityMapper userIdentityMapper,
            TicketOrderMapper ticketOrderMapper
    ) {
        this.userIdentityMapper = userIdentityMapper;
        this.ticketOrderMapper = ticketOrderMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserIdentity getCurrentIdentity(Long userId) {
        UserIdentity identity = findByUserId(userId);
        if (identity == null) {
            throw new BusinessException(404, "当前用户尚未实名认证");
        }
        return identity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserIdentity bindIdentity(Long userId, UserIdentityBindRequest request) {
        if (findByUserId(userId) != null) {
            throw new BusinessException(409, "当前用户已经完成实名认证");
        }

        String idType = normalizeIdType(request.idType());
        String idNo = normalizeIdNo(request.idNo());
        long documentCount = userIdentityMapper.selectCount(
                new LambdaQueryWrapper<UserIdentity>()
                        .eq(UserIdentity::getIdType, idType)
                        .eq(UserIdentity::getIdNo, idNo)
        );
        if (documentCount > 0) {
            throw new BusinessException(409, "该证件已经绑定其他账号");
        }

        UserIdentity identity = new UserIdentity();
        identity.setUserId(userId);
        identity.setRealName(request.realName().trim());
        identity.setIdType(idType);
        identity.setIdNo(idNo);
        identity.setStatus(1);
        identity.setVerifiedAt(LocalDateTime.now());

        try {
            if (userIdentityMapper.insert(identity) == 0) {
                throw new BusinessException(500, "实名认证保存失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "当前账号或证件已经完成实名认证");
        }
        ticketOrderMapper.bindHistoricalTicketsToUser(
                userId,
                identity.getRealName(),
                identity.getIdType(),
                identity.getIdNo()
        );
        return getCurrentIdentity(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserIdentity findVerifiedIdentity(String idType, String idNo) {
        return userIdentityMapper.selectOne(
                new LambdaQueryWrapper<UserIdentity>()
                        .eq(UserIdentity::getIdType, normalizeIdType(idType))
                        .eq(UserIdentity::getIdNo, normalizeIdNo(idNo))
                        .eq(UserIdentity::getStatus, 1)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserIdentity findVerifiedIdentityByUserId(Long userId) {
        return userIdentityMapper.selectOne(
                new LambdaQueryWrapper<UserIdentity>()
                        .eq(UserIdentity::getUserId, userId)
                        .eq(UserIdentity::getStatus, 1)
        );
    }

    private UserIdentity findByUserId(Long userId) {
        return userIdentityMapper.selectOne(
                new LambdaQueryWrapper<UserIdentity>()
                        .eq(UserIdentity::getUserId, userId)
        );
    }

    private String normalizeIdType(String idType) {
        String normalized = idType.trim().toUpperCase(Locale.ROOT);
        if (!PassengerIdType.contains(normalized)) {
            throw new BusinessException(400, "不支持的证件类型");
        }
        return normalized;
    }

    private String normalizeIdNo(String idNo) {
        return idNo.trim().toUpperCase(Locale.ROOT);
    }
}
