package com.ahrose.railticket.train.schedule.service.impl;

import com.ahrose.railticket.bean.Train;
import com.ahrose.railticket.bean.TrainSchedule;
import com.ahrose.railticket.bean.request.TrainScheduleCreateRequest;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.train.base.mapper.TrainMapper;
import com.ahrose.railticket.train.schedule.mapper.TrainScheduleMapper;
import com.ahrose.railticket.train.schedule.service.TrainScheduleService;
import com.ahrose.railticket.train.search.service.TrainSearchCacheService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * ClassName: TrainScheduleServiceImpl
 * Package: com.ahrose.railticket.train.schedule.service.impl
 * Description:
 *
 * @Author 张浩然
 * @Create 2026/9/29 20:16
 * @Version 1.0
 */
@Service
public class TrainScheduleServiceImpl extends ServiceImpl<TrainScheduleMapper, TrainSchedule> implements TrainScheduleService {

    @Autowired
    private TrainScheduleMapper trainScheduleMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private TrainSearchCacheService searchCacheService;

    @Override
    public List<TrainSchedule> listTrainSchedules(Long trainId, LocalDate startDate, LocalDate endDate) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        getTrainByIdOrThrow(trainId);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(400, "开始日期不能晚于结束日期");
        }

        LambdaQueryWrapper<TrainSchedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrainSchedule::getTrainId, trainId);
        queryWrapper.ge(startDate != null, TrainSchedule::getTravelDate, startDate);
        queryWrapper.le(endDate != null, TrainSchedule::getTravelDate, endDate);
        queryWrapper.orderByAsc(TrainSchedule::getTravelDate);

        return trainScheduleMapper.selectList(queryWrapper);
    }

    @Override
    public TrainSchedule getTrainSchedule(Long trainId, Long scheduleId) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (scheduleId == null) {
            throw new BusinessException(400, "运行计划 ID 不能为空");
        }

        return getTrainScheduleOrThrow(trainId, scheduleId);
    }

    @Override
    public TrainSchedule createTrainSchedule(Long trainId, TrainScheduleCreateRequest request) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (request == null) {
            throw new BusinessException(400, "运行计划信息不能为空");
        }
        if (request.travelDate() == null) {
            throw new BusinessException(400, "车次的始发日期不能为空");
        }

        getTrainByIdOrThrow(trainId);

        TrainSchedule existingSchedule = trainScheduleMapper.selectOne(
                new LambdaQueryWrapper<TrainSchedule>()
                        .eq(TrainSchedule::getTrainId, trainId)
                        .eq(TrainSchedule::getTravelDate, request.travelDate())
        );
        if (existingSchedule != null) {
            if (Integer.valueOf(1).equals(existingSchedule.getStatus())) {
                throw new BusinessException(409, "该车次在指定日期已存在运行计划");
            }
            throw new BusinessException(409, "该车次在指定日期已存在停运计划，请直接恢复运行状态");
        }

        TrainSchedule trainSchedule = new TrainSchedule();
        trainSchedule.setTrainId(trainId);
        trainSchedule.setTravelDate(request.travelDate());
        trainSchedule.setStatus(1);

        int rows = trainScheduleMapper.insert(trainSchedule);
        if (rows == 0) {
            throw new BusinessException(500, "新增运行计划失败");
        }

        // 新计划不在任何旧查票结果的 schedule 索引中，必须按日期清除旧列表或空结果缓存。
        searchCacheService.evictTravelDateAfterCommit(trainSchedule.getTravelDate());

        return trainSchedule;
    }

    @Override
    public TrainSchedule updateTrainScheduleStatus(Long trainId, Long scheduleId, Integer status) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (scheduleId == null) {
            throw new BusinessException(400, "运行计划 ID 不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "运行计划状态只能为 0 或 1");
        }
        TrainSchedule trainSchedule = getTrainScheduleOrThrow(trainId, scheduleId);

        if (status.equals(trainSchedule.getStatus())) {
            return trainSchedule;
        }

        boolean becomingEnabled = Integer.valueOf(0).equals(trainSchedule.getStatus()) && status == 1;
        trainSchedule.setStatus(status);
        int rows = trainScheduleMapper.updateById(trainSchedule);
        if (rows == 0) {
            throw new BusinessException(500, "更新运行计划状态失败");
        }
        if (becomingEnabled) {
            // 恢复运行的计划不在旧查票结果中，无法通过 scheduleId 反向索引找到旧缓存。
            searchCacheService.evictTravelDateAfterCommit(trainSchedule.getTravelDate());
        } else {
            // 停运时，旧查票结果已经包含该计划，可以通过 scheduleId 精确失效。
            searchCacheService.evictScheduleAfterCommit(trainSchedule.getId());
        }
        return trainSchedule;
    }

    @Override
    public void deleteTrainSchedule(Long trainId, Long scheduleId) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (scheduleId == null) {
            throw new BusinessException(400, "运行计划 ID 不能为空");
        }

        TrainSchedule trainSchedule = getTrainScheduleOrThrow(trainId, scheduleId);
        int rows = trainScheduleMapper.deleteById(trainSchedule.getId());
        if (rows == 0) {
            throw new BusinessException(500, "删除运行计划失败");
        }
        searchCacheService.evictScheduleAfterCommit(trainSchedule.getId());
    }

    /** 校验车次存在。停用车次的运行计划仍允许管理员查询和维护。 */
    private Train getTrainByIdOrThrow(Long trainId) {
        Train train = trainMapper.selectById(trainId);
        if (train == null) {
            throw new BusinessException(404, "车次不存在");
        }
        return train;
    }

    /** 查询属于指定车次的运行计划，避免访问其他车次的数据。 */
    private TrainSchedule getTrainScheduleOrThrow(Long trainId, Long scheduleId) {
        TrainSchedule trainSchedule = trainScheduleMapper.selectOne(
                new LambdaQueryWrapper<TrainSchedule>()
                        .eq(TrainSchedule::getId, scheduleId)
                        .eq(TrainSchedule::getTrainId, trainId)
        );
        if (trainSchedule == null) {
            throw new BusinessException(404, "运行计划不存在或不属于当前车次");
        }
        return trainSchedule;
    }
}
