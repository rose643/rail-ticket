package com.ahrose.railticket.train.search.service.impl;

import com.ahrose.railticket.bean.TrainFare;
import com.ahrose.railticket.bean.response.SeatAvailabilityResponse;
import com.ahrose.railticket.bean.response.TrainSearchResponse;
import com.ahrose.railticket.common.enums.SeatType;
import com.ahrose.railticket.common.enums.TrainSearchLocationType;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.inventory.query.InventoryQueryService;
import com.ahrose.railticket.train.fare.mapper.TrainFareMapper;
import com.ahrose.railticket.train.search.mapper.TrainSearchMapper;
import com.ahrose.railticket.train.search.model.TrainSearchCandidate;
import com.ahrose.railticket.train.search.service.TrainSearchCacheService;
import com.ahrose.railticket.train.search.service.TrainSearchService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 面向普通用户的车票查询业务实现。 */
@Service
public class TrainSearchServiceImpl implements TrainSearchService {

    private final TrainSearchMapper trainSearchMapper;
    private final TrainFareMapper trainFareMapper;
    private final InventoryQueryService inventoryQueryService;
    private final TrainSearchCacheService searchCacheService;

    public TrainSearchServiceImpl(
            TrainSearchMapper trainSearchMapper,
            TrainFareMapper trainFareMapper,
            InventoryQueryService inventoryQueryService,
            TrainSearchCacheService searchCacheService
    ) {
        this.trainSearchMapper = trainSearchMapper;
        this.trainFareMapper = trainFareMapper;
        this.inventoryQueryService = inventoryQueryService;
        this.searchCacheService = searchCacheService;
    }

    @Override
    public List<TrainSearchResponse> searchTrains(
            TrainSearchLocationType fromType,
            String fromValue,
            TrainSearchLocationType toType,
            String toValue,
            LocalDate travelDate
    ) {
        validateLocation(fromType, fromValue, "出发");
        validateLocation(toType, toValue, "到达");
        String normalizedFromValue = normalizeLocationValue(fromType, fromValue);
        String normalizedToValue = normalizeLocationValue(toType, toValue);
        if (fromType == toType && normalizedFromValue.equals(normalizedToValue)) {
            throw new BusinessException(400, "出发地点和到达地点不能相同");
        }
        if (travelDate == null) {
            throw new BusinessException(400, "始发日期不能为空");
        }
        if (travelDate.isBefore(LocalDate.now())) {
            throw new BusinessException(400, "始发日期不能早于当前日期");
        }

        return searchCacheService.getOrLoad(
                fromType,
                normalizedFromValue,
                toType,
                normalizedToValue,
                travelDate,
                () -> queryDatabase(
                        fromType,
                        normalizedFromValue,
                        toType,
                        normalizedToValue,
                        travelDate
                )
        );
    }

    private List<TrainSearchResponse> queryDatabase(
            TrainSearchLocationType fromType,
            String fromValue,
            TrainSearchLocationType toType,
            String toValue,
            LocalDate travelDate
    ) {
        List<TrainSearchCandidate> candidates = trainSearchMapper.selectSearchCandidates(
                fromType.name(),
                fromValue,
                toType.name(),
                toValue,
                travelDate
        );
        List<TrainSearchResponse> result = new ArrayList<>(candidates.size());
        for (TrainSearchCandidate candidate : candidates) {
            result.add(toResponse(candidate));
        }
        return result;
    }

    /** 校验地点类型、值以及具体车站编码的长度。 */
    private void validateLocation(
            TrainSearchLocationType type,
            String value,
            String direction
    ) {
        if (type == null) {
            throw new BusinessException(400, direction + "地点类型不能为空");
        }
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, direction + "地点不能为空");
        }
        if (type == TrainSearchLocationType.STATION && value.trim().length() > 10) {
            throw new BusinessException(400, direction + "车站编码不能超过 10 个字符");
        }
        if (type == TrainSearchLocationType.CITY && value.trim().length() > 64) {
            throw new BusinessException(400, direction + "城市名称不能超过 64 个字符");
        }
    }

    /** 车站编码统一转为大写；城市名称只去除首尾空格。 */
    private String normalizeLocationValue(TrainSearchLocationType type, String value) {
        String normalized = value.trim();
        if (type == TrainSearchLocationType.STATION) {
            return normalized.toUpperCase(Locale.ROOT);
        }
        return normalized;
    }

    /** 将候选车次与票价、区间余票组合为最终查票结果。 */
    private TrainSearchResponse toResponse(TrainSearchCandidate candidate) {
        if (candidate.getDepartureOffsetMinutes() == null
                || candidate.getArrivalOffsetMinutes() == null) {
            throw new BusinessException(500, "车次经停时间配置不完整");
        }

        LocalDateTime departureTime = candidate.getTravelDate()
                .atStartOfDay()
                .plusMinutes(candidate.getDepartureOffsetMinutes());
        LocalDateTime arrivalTime = candidate.getTravelDate()
                .atStartOfDay()
                .plusMinutes(candidate.getArrivalOffsetMinutes());
        if (arrivalTime.isBefore(departureTime)) {
            throw new BusinessException(500, "车次到达时间早于发车时间");
        }

        List<TrainFare> fares = trainFareMapper.selectList(
                new LambdaQueryWrapper<TrainFare>()
                        .eq(TrainFare::getTrainId, candidate.getTrainId())
                        .eq(TrainFare::getFromStopIndex, candidate.getFromStopIndex())
                        .eq(TrainFare::getToStopIndex, candidate.getToStopIndex())
        );
        Map<String, Integer> availableBySeatType = inventoryQueryService.getAvailableBySeatType(
                candidate.getScheduleId(),
                candidate.getFromStopIndex(),
                candidate.getToStopIndex()
        );

        List<SeatAvailabilityResponse> seats = new ArrayList<>();
        for (SeatType seatType : SeatType.values()) {
            TrainFare fare = findFare(fares, seatType.getCode());
            if (fare == null) {
                continue;
            }
            seats.add(new SeatAvailabilityResponse(
                    seatType.getCode(),
                    seatType.getName(),
                    fare.getPrice(),
                    availableBySeatType.getOrDefault(seatType.getCode(), 0)
            ));
        }

        return new TrainSearchResponse(
                candidate.getScheduleId(),
                candidate.getTrainId(),
                candidate.getTrainNo(),
                candidate.getTrainName(),
                candidate.getTravelDate(),
                candidate.getFromStationCode(),
                candidate.getFromStationName(),
                candidate.getFromStopIndex(),
                candidate.getToStationCode(),
                candidate.getToStationName(),
                candidate.getToStopIndex(),
                departureTime,
                arrivalTime,
                Duration.between(departureTime, arrivalTime).toMinutes(),
                seats
        );
    }

    /** 从当前乘车区间的票价列表中查找指定席别票价。 */
    private TrainFare findFare(List<TrainFare> fares, String seatTypeCode) {
        for (TrainFare fare : fares) {
            if (seatTypeCode.equals(fare.getSeatTypeCode())) {
                return fare;
            }
        }
        return null;
    }
}
