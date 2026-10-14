package com.ahrose.railticket.passenger.service.impl;

import com.ahrose.railticket.bean.Passenger;
import com.ahrose.railticket.bean.request.PassengerCreateRequest;
import com.ahrose.railticket.bean.request.PassengerPhoneUpdateRequest;
import com.ahrose.railticket.common.enums.PassengerIdType;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.passenger.mapper.PassengerMapper;
import com.ahrose.railticket.passenger.service.PassengerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** 当前登录用户的常用乘车人业务实现。 */
@Service
public class PassengerServiceImpl extends ServiceImpl<PassengerMapper, Passenger>
        implements PassengerService {

    private final PassengerMapper passengerMapper;

    public PassengerServiceImpl(PassengerMapper passengerMapper) {
        this.passengerMapper = passengerMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Passenger> listPassengers(Long userId) {
        return passengerMapper.selectList(
                new LambdaQueryWrapper<Passenger>()
                        .eq(Passenger::getUserId, userId)
                        .orderByDesc(Passenger::getCreatedAt)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Passenger getPassenger(Long userId, Long passengerId) {
        return getOwnedPassengerOrThrow(userId, passengerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Passenger createPassenger(Long userId, PassengerCreateRequest request) {
        String idType = normalizeIdType(request.idType());
        String idNo = normalizeIdNo(request.idNo());
        checkDocumentAvailable(userId, idType, idNo);

        Passenger passenger = new Passenger();
        passenger.setUserId(userId);
        passenger.setName(request.name().trim());
        passenger.setIdType(idType);
        passenger.setIdNo(idNo);
        passenger.setPhone(normalizePhone(request.phone()));

        try {
            if (passengerMapper.insert(passenger) == 0) {
                throw new BusinessException(500, "新增乘车人失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "该证件号码已添加为常用乘车人");
        }
        return getOwnedPassengerOrThrow(userId, passenger.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Passenger updatePassengerPhone(
            Long userId,
            Long passengerId,
            PassengerPhoneUpdateRequest request
    ) {
        getOwnedPassengerOrThrow(userId, passengerId);

        Passenger passenger = new Passenger();
        passenger.setId(passengerId);
        passenger.setPhone(request.phone().trim());

        if (passengerMapper.updateById(passenger) == 0) {
            throw new BusinessException(409, "乘车人信息已发生变化，请刷新后重试");
        }
        return getOwnedPassengerOrThrow(userId, passengerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePassenger(Long userId, Long passengerId) {
        getOwnedPassengerOrThrow(userId, passengerId);
        int rows = passengerMapper.delete(
                new LambdaQueryWrapper<Passenger>()
                        .eq(Passenger::getId, passengerId)
                        .eq(Passenger::getUserId, userId)
        );
        if (rows == 0) {
            throw new BusinessException(404, "乘车人不存在");
        }
    }

    private Passenger getOwnedPassengerOrThrow(Long userId, Long passengerId) {
        Passenger passenger = passengerMapper.selectOne(
                new LambdaQueryWrapper<Passenger>()
                        .eq(Passenger::getId, passengerId)
                        .eq(Passenger::getUserId, userId)
        );
        if (passenger == null) {
            throw new BusinessException(404, "乘车人不存在");
        }
        return passenger;
    }

    private void checkDocumentAvailable(
            Long userId,
            String idType,
            String idNo
    ) {
        LambdaQueryWrapper<Passenger> wrapper = new LambdaQueryWrapper<Passenger>()
                .eq(Passenger::getUserId, userId)
                .eq(Passenger::getIdType, idType)
                .eq(Passenger::getIdNo, idNo);
        if (passengerMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(409, "该证件号码已添加为常用乘车人");
        }
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

    private String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }
}
