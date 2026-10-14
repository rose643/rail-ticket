package com.ahrose.railticket.train.fare.service.impl;

import com.ahrose.railticket.bean.Train;
import com.ahrose.railticket.bean.TrainFare;
import com.ahrose.railticket.bean.TrainStop;
import com.ahrose.railticket.bean.TrainSchedule;
import com.ahrose.railticket.bean.request.TrainFareCreateRequest;
import com.ahrose.railticket.bean.request.TrainFareUpdateRequest;
import com.ahrose.railticket.common.enums.SeatType;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.train.base.mapper.TrainMapper;
import com.ahrose.railticket.train.fare.mapper.TrainFareMapper;
import com.ahrose.railticket.train.fare.service.TrainFareService;
import com.ahrose.railticket.train.stop.mapper.TrainStopMapper;
import com.ahrose.railticket.train.schedule.mapper.TrainScheduleMapper;
import com.ahrose.railticket.train.search.service.TrainSearchCacheService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ClassName: TrainFareServiceImpl
 * Package: com.ahrose.railticket.train.fare.service.impl
 * Description:
 *
 * @Author 张浩然
 * @Create 2026/10/1 17:52
 * @Version 1.0
 */
@Service
public class TrainFareServiceImpl extends ServiceImpl<TrainFareMapper, TrainFare> implements TrainFareService {

    @Autowired
    private TrainFareMapper trainFareMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private TrainStopMapper trainStopMapper;

    @Autowired
    private TrainScheduleMapper trainScheduleMapper;

    @Autowired
    private TrainSearchCacheService searchCacheService;


    @Override
    public List<TrainFare> listTrainFares(Long trainId, String seatTypeCode, Integer fromStopIndex, Integer toStopIndex) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (seatTypeCode != null && !SeatType.contains(seatTypeCode)) {
            throw new BusinessException(400, "不支持的席别编码");
        }
        if (fromStopIndex != null && fromStopIndex < 0) {
            throw new BusinessException(400, "出发站顺序不能小于 0");
        }
        if (toStopIndex != null && toStopIndex < 0) {
            throw new BusinessException(400, "到达站顺序不能小于 0");
        }
        if (fromStopIndex != null && toStopIndex != null && fromStopIndex >= toStopIndex) {
            throw new BusinessException(400, "出发站顺序必须小于到达站顺序");
        }

        getTrainByIdOrThrow(trainId);

        validateStopIndexesWithinRoute(trainId, fromStopIndex, toStopIndex);

        LambdaQueryWrapper<TrainFare> queryWrapper = new LambdaQueryWrapper<TrainFare>()
                .eq(TrainFare::getTrainId, trainId);
        queryWrapper.eq(
                seatTypeCode != null,
                TrainFare::getSeatTypeCode,
                seatTypeCode
        );
        queryWrapper.eq(
                fromStopIndex != null,
                TrainFare::getFromStopIndex,
                fromStopIndex
        );

        queryWrapper.eq(
                toStopIndex != null,
                TrainFare::getToStopIndex,
                toStopIndex
        );
        queryWrapper.orderByAsc(TrainFare::getFromStopIndex);
        queryWrapper.orderByAsc(TrainFare::getToStopIndex);
        queryWrapper.orderByAsc(TrainFare::getSeatTypeCode);

        return trainFareMapper.selectList(queryWrapper);
    }

    @Override
    public TrainFare getTrainFare(Long trainId, Long fareId) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (fareId == null) {
            throw new BusinessException(400, "票价 ID 不能为空");
        }
        return getTrainFareOrThrow(trainId, fareId);
    }

    @Override
    public TrainFare createTrainFare(Long trainId, TrainFareCreateRequest request) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (request == null) {
            throw new BusinessException(400, "票价信息不能为空");
        }

        if (!SeatType.contains(request.seatTypeCode())) {
            throw new BusinessException(400, "不支持的席别编码");
        }
        if (request.fromStopIndex() >= request.toStopIndex()) {
            throw new BusinessException(400, "出发站顺序必须小于到达站顺序");
        }

        getTrainByIdOrThrow(trainId);
        validateStopIndexesWithinRoute(trainId, request.fromStopIndex(), request.toStopIndex());

        long count = trainFareMapper.selectCount(
                new LambdaQueryWrapper<TrainFare>()
                        .eq(TrainFare::getTrainId, trainId)
                        .eq(TrainFare::getSeatTypeCode, request.seatTypeCode())
                        .eq(TrainFare::getFromStopIndex, request.fromStopIndex())
                        .eq(TrainFare::getToStopIndex, request.toStopIndex())
        );
        if (count > 0) {
            throw new BusinessException(409, "该车次、席别和乘车区间已配置票价");
        }

        TrainFare trainFare = new TrainFare();
        trainFare.setTrainId(trainId);
        trainFare.setSeatTypeCode(request.seatTypeCode());
        trainFare.setFromStopIndex(request.fromStopIndex());
        trainFare.setToStopIndex(request.toStopIndex());
        trainFare.setPrice(request.price());

        int rows = trainFareMapper.insert(trainFare);
        if (rows == 0) {
            throw new BusinessException(500, "新增票价失败");
        }
        evictTrainSearchCache(trainId);
        return trainFare;
    }

    @Override
    public TrainFare updateTrainFare(Long trainId, Long fareId, TrainFareUpdateRequest request) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (fareId == null) {
            throw new BusinessException(400, "票价 ID 不能为空");
        }
        if (request == null) {
            throw new BusinessException(400, "票价信息不能为空");
        }

        TrainFare trainFare = getTrainFareOrThrow(trainId, fareId);
        if (trainFare.getPrice().compareTo(request.price()) == 0) {
            return trainFare;
        }

        trainFare.setPrice(request.price());
        int rows = trainFareMapper.updateById(trainFare);
        if (rows == 0) {
            throw new BusinessException(500, "更新票价失败");
        }
        evictTrainSearchCache(trainId);
        return trainFare;
    }

    @Override
    public void deleteTrainFare(Long trainId, Long fareId) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (fareId == null) {
            throw new BusinessException(400, "票价 ID 不能为空");
        }
        getTrainFareOrThrow(trainId, fareId);
        int rows = trainFareMapper.deleteById(fareId);
        if (rows == 0) {
            throw new BusinessException(500, "删除票价失败");
        }
        evictTrainSearchCache(trainId);

    }

    /** 校验车次存在。停用车次的票价仍允许管理员查询和维护。 */
    private Train getTrainByIdOrThrow(Long trainId) {
        Train train = trainMapper.selectById(trainId);
        if (train == null) {
            throw new BusinessException(404, "车次不存在");
        }
        return train;
    }

    /** 校验票价区间没有超出车次路线范围。 */
    private void validateStopIndexesWithinRoute(
            Long trainId,
            Integer fromStopIndex,
            Integer toStopIndex
    ) {
        if (fromStopIndex == null && toStopIndex == null) {
            return;
        }

        TrainStop lastStop = trainStopMapper.selectOne(
                new LambdaQueryWrapper<TrainStop>()
                        .eq(TrainStop::getTrainId, trainId)
                        .orderByDesc(TrainStop::getStopIndex)
                        .last("LIMIT 1")
        );
        if (lastStop == null) {
            throw new BusinessException(400, "该车次尚未配置经停站");
        }

        Integer lastStopIndex = lastStop.getStopIndex();
        if (fromStopIndex != null && fromStopIndex >= lastStopIndex) {
            throw new BusinessException(400, "出发站顺序必须小于最后一个停靠站顺序");
        }
        if (toStopIndex != null && toStopIndex > lastStopIndex) {
            throw new BusinessException(400, "到达站顺序不能大于最后一个停靠站顺序");
        }
    }

    /** 查询属于指定车次的票价记录，避免访问其他车次的数据。 */
    private TrainFare getTrainFareOrThrow(Long trainId, Long fareId) {
        TrainFare trainFare = trainFareMapper.selectOne(
                new LambdaQueryWrapper<TrainFare>()
                        .eq(TrainFare::getId, fareId)
                        .eq(TrainFare::getTrainId, trainId)
        );
        if (trainFare == null) {
            throw new BusinessException(404, "票价不存在或不属于当前车次");
        }
        return trainFare;
    }

    /** 票价变化后清除该车次所有运行计划的查票缓存。 */
    private void evictTrainSearchCache(Long trainId) {
        trainScheduleMapper.selectList(
                        new LambdaQueryWrapper<TrainSchedule>()
                                .select(TrainSchedule::getId)
                                .eq(TrainSchedule::getTrainId, trainId)
                ).stream()
                .map(TrainSchedule::getId)
                .distinct()
                .forEach(searchCacheService::evictScheduleAfterCommit);
    }
}
