package com.ahrose.railticket.train.stop.service.impl;

import com.ahrose.railticket.bean.TrainStop;
import com.ahrose.railticket.bean.Station;
import com.ahrose.railticket.bean.Train;
import com.ahrose.railticket.bean.TrainFare;
import com.ahrose.railticket.bean.request.TrainStopCreateRequest;
import com.ahrose.railticket.bean.request.TrainStopUpdateRequest;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.station.mapper.StationMapper;
import com.ahrose.railticket.train.base.mapper.TrainMapper;
import com.ahrose.railticket.train.fare.mapper.TrainFareMapper;
import com.ahrose.railticket.train.stop.mapper.TrainStopMapper;
import com.ahrose.railticket.train.stop.service.TrainStopService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ClassName: TrainStopServiceImpl
 * Package: com.ahrose.railticket.train.stop.service.impl
 * Description:
 *
 * @Author 张浩然
 * @Create 2026/9/24 20:57
 * @Version 1.0
 */
@Service
public class TrainStopServiceImpl extends ServiceImpl<TrainStopMapper, TrainStop> implements TrainStopService {

    @Autowired
    private TrainStopMapper trainStopMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private TrainFareMapper trainFareMapper;

    @Override
    public List<TrainStop> listTrainStops(Long trainId) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }

        LambdaQueryWrapper<TrainStop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrainStop::getTrainId, trainId);
        wrapper.orderByAsc(TrainStop::getStopIndex);

        return trainStopMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrainStop createTrainStop(Long trainId, TrainStopCreateRequest request) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (request == null) {
            throw new BusinessException(400, "经停站信息不能为空");
        }
        if (request.stationId() == null) {
            throw new BusinessException(400, "车站 ID 不能为空");
        }
        if (request.stopIndex() == null || request.stopIndex() < 0) {
            throw new BusinessException(400, "经停站顺序不能小于 0");
        }
        validateTimeOffsets(request.arrivalOffsetMinutes(), request.departureOffsetMinutes());

        getTrainByIdOrThrow(trainId);
        ensureNoFaresConfigured(trainId);
        getEnabledStationByIdOrThrow(request.stationId());
        checkStationNotInTrain(trainId, request.stationId());

        long stopCount = trainStopMapper.selectCount(new LambdaQueryWrapper<TrainStop>()
                .eq(TrainStop::getTrainId, trainId));
        if (request.stopIndex() > stopCount) {
            throw new BusinessException(400, "经停站顺序不能大于当前站点数量");
        }

        // 先将插入位置及后续站点后移，再写入新站点。
        trainStopMapper.shiftStopIndexesForInsert(trainId, request.stopIndex());

        TrainStop trainStop = new TrainStop();
        trainStop.setTrainId(trainId);
        trainStop.setStationId(request.stationId());
        trainStop.setStopIndex(request.stopIndex());
        trainStop.setArrivalOffsetMinutes(request.arrivalOffsetMinutes());
        trainStop.setDepartureOffsetMinutes(request.departureOffsetMinutes());
        int rows = trainStopMapper.insert(trainStop);
        if (rows == 0) {
            throw new BusinessException(500, "新增经停站失败");
        }
        return trainStop;
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrainStop updateTrainStop(Long trainId, Long stopId, TrainStopUpdateRequest request) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (stopId == null) {
            throw new BusinessException(400, "经停站 ID 不能为空");
        }
        if (request == null) {
            throw new BusinessException(400, "经停站信息不能为空");
        }
        if (request.stationId() == null) {
            throw new BusinessException(400, "车站 ID 不能为空");
        }
        if (request.stopIndex() == null || request.stopIndex() < 0) {
            throw new BusinessException(400, "经停站顺序不能小于 0");
        }
        validateTimeOffsets(request.arrivalOffsetMinutes(), request.departureOffsetMinutes());

        getTrainByIdOrThrow(trainId);
        TrainStop trainStop = getTrainStopOrThrow(trainId, stopId);
        if (!trainStop.getStationId().equals(request.stationId())
                || !trainStop.getStopIndex().equals(request.stopIndex())) {
            ensureNoFaresConfigured(trainId);
        }
        getEnabledStationByIdOrThrow(request.stationId());
        checkStationNotInTrain(trainId, request.stationId(), stopId);

        long stopCount = trainStopMapper.selectCount(new LambdaQueryWrapper<TrainStop>()
                .eq(TrainStop::getTrainId, trainId));
        if (request.stopIndex() >= stopCount) {
            throw new BusinessException(400, "经停站顺序不能大于或等于当前站点数量");
        }

        Integer oldIndex = trainStop.getStopIndex();
        Integer newIndex = request.stopIndex();
        if (!oldIndex.equals(newIndex)) {
            int rows = trainStopMapper.markStopIndexTemporary(trainId, stopId);
            if (rows == 0) {
                throw new BusinessException(500, "调整经停站顺序失败");
            }

            if (oldIndex < newIndex) {
                trainStopMapper.shiftStopIndexesForward(trainId, oldIndex, newIndex);
            } else {
                trainStopMapper.shiftStopIndexesBackward(trainId, newIndex, oldIndex);
            }
        }

        trainStop.setStationId(request.stationId());
        trainStop.setStopIndex(newIndex);
        trainStop.setArrivalOffsetMinutes(request.arrivalOffsetMinutes());
        trainStop.setDepartureOffsetMinutes(request.departureOffsetMinutes());
        int rows = trainStopMapper.updateById(trainStop);
        if (rows == 0) {
            throw new BusinessException(500, "更新经停站失败");
        }

        return trainStop;
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTrainStop(Long trainId, Long stopId) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (stopId == null) {
            throw new BusinessException(400, "经停站 ID 不能为空");
        }
        getTrainByIdOrThrow(trainId);
        TrainStop trainStop = getTrainStopOrThrow(trainId, stopId);
        ensureNoFaresConfigured(trainId);

        int rows = trainStopMapper.deleteById(stopId);
        if (rows == 0) {
            throw new BusinessException(500, "删除经停站失败");
        }

        trainStopMapper.shiftStopIndexesForDelete(trainId, trainStop.getStopIndex());
    }

    /** 校验两个相对始发时间同时存在时，到达时间不得晚于发车时间。 */
    private void validateTimeOffsets(Integer arrivalOffsetMinutes, Integer departureOffsetMinutes) {
        if (arrivalOffsetMinutes != null && arrivalOffsetMinutes < 0
                || departureOffsetMinutes != null && departureOffsetMinutes < 0) {
            throw new BusinessException(400, "到达或发车分钟数不能小于 0");
        }
        if (arrivalOffsetMinutes != null && departureOffsetMinutes != null
                && arrivalOffsetMinutes > departureOffsetMinutes) {
            throw new BusinessException(400, "到达时间不能晚于发车时间");
        }
    }

    /** 校验车次存在。车次即使停用，管理员仍可先维护线路。 */
    private Train getTrainByIdOrThrow(Long trainId) {
        Train train = trainMapper.selectById(trainId);
        if (train == null) {
            throw new BusinessException(404, "车次不存在");
        }
        return train;
    }

    /** 新增线路时只能选择启用状态的车站。 */
    private Station getEnabledStationByIdOrThrow(Long stationId) {
        Station station = stationMapper.selectById(stationId);
        if (station == null || !Integer.valueOf(1).equals(station.getStatus())) {
            throw new BusinessException(404, "车站不存在或已停用");
        }
        return station;
    }

    /** 同一车次不允许重复经过同一个车站。 */
    private void checkStationNotInTrain(Long trainId, Long stationId) {
        checkStationNotInTrain(trainId, stationId, null);
    }

    /** 修改经停站时检查车站是否重复，并排除当前经停站记录。 */
    private void checkStationNotInTrain(Long trainId, Long stationId, Long excludedStopId) {
        LambdaQueryWrapper<TrainStop> wrapper = new LambdaQueryWrapper<TrainStop>()
                .eq(TrainStop::getTrainId, trainId)
                .eq(TrainStop::getStationId, stationId);
        if (excludedStopId != null) {
            wrapper.ne(TrainStop::getId, excludedStopId);
        }

        long count = trainStopMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(400, "该车次已配置此经停站");
        }
    }

    /** 查询属于指定车次的经停站，避免修改其他车次的线路数据。 */
    private TrainStop getTrainStopOrThrow(Long trainId, Long stopId) {
        TrainStop trainStop = trainStopMapper.selectOne(new LambdaQueryWrapper<TrainStop>()
                .eq(TrainStop::getId, stopId)
                .eq(TrainStop::getTrainId, trainId));
        if (trainStop == null) {
            throw new BusinessException(404, "经停站不存在或不属于当前车次");
        }
        return trainStop;
    }

    /** 已配置票价的车次不允许改变经停站或站点顺序，避免票价区间含义发生变化。 */
    private void ensureNoFaresConfigured(Long trainId) {
        long fareCount = trainFareMapper.selectCount(
                new LambdaQueryWrapper<TrainFare>()
                        .eq(TrainFare::getTrainId, trainId)
        );
        if (fareCount > 0) {
            throw new BusinessException(409, "该车次已配置票价，请先删除票价后再修改路线");
        }
    }
}
