package com.ahrose.railticket.inventory.service.impl;

import com.ahrose.railticket.bean.SeatInventory;
import com.ahrose.railticket.bean.TrainSchedule;
import com.ahrose.railticket.bean.TrainStop;
import com.ahrose.railticket.bean.request.SeatInventoryInitializeRequest;
import com.ahrose.railticket.common.enums.SeatType;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.inventory.mapper.SeatInventoryMapper;
import com.ahrose.railticket.inventory.service.SeatInventoryService;
import com.ahrose.railticket.train.schedule.mapper.TrainScheduleMapper;
import com.ahrose.railticket.train.stop.mapper.TrainStopMapper;
import com.ahrose.railticket.train.search.service.TrainSearchCacheService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: SeatInventoryServiceImpl
 * Package: com.ahrose.railticket.inventory.service.impl
 * Description:
 *
 * @Author 张浩然
 * @Create 2026/10/1 20:16
 * @Version 1.0
 */
@Service
public class SeatInventoryServiceImpl extends ServiceImpl<SeatInventoryMapper, SeatInventory> implements SeatInventoryService {

    @Autowired
    private TrainScheduleMapper trainScheduleMapper;

    @Autowired
    private SeatInventoryMapper seatInventoryMapper;

    @Autowired
    private TrainStopMapper trainStopMapper;

    @Autowired
    private TrainSearchCacheService searchCacheService;

    @Override
    public List<SeatInventory> listSeatInventories(Long trainId, Long scheduleId, String seatTypeCode) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (scheduleId == null) {
            throw new BusinessException(400, "运行计划 ID 不能为空");
        }
        if (seatTypeCode != null && !SeatType.contains(seatTypeCode)) {
            throw new BusinessException(400, "不支持的席别编码");
        }

        getTrainScheduleOrThrow(trainId, scheduleId);
        LambdaQueryWrapper<SeatInventory> wrapper = new LambdaQueryWrapper<SeatInventory>()
                .eq(SeatInventory::getScheduleId, scheduleId)
                .eq(
                        seatTypeCode != null,
                        SeatInventory::getSeatTypeCode,
                        seatTypeCode
                )
                .orderByAsc(SeatInventory::getSeatTypeCode)
                .orderByAsc(SeatInventory::getSegmentIndex);

        return seatInventoryMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SeatInventory> initializeSeatInventory(Long trainId, Long scheduleId, SeatInventoryInitializeRequest request) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (scheduleId == null) {
            throw new BusinessException(400, "运行计划 ID 不能为空");
        }
        if (request == null) {
            throw new BusinessException(400, "库存初始化信息不能为空");
        }
        if (!SeatType.contains(request.seatTypeCode())) {
            throw new BusinessException(400, "不支持的席别编码");
        }
        if (request.total() == null) {
            throw new BusinessException(400, "总库存不能为空");
        }
        if (request.total() < 0) {
            throw new BusinessException(400, "总库存不能小于 0");
        }

        getTrainScheduleOrThrow(trainId, scheduleId);

        long existingCount = seatInventoryMapper.selectCount(
                new LambdaQueryWrapper<SeatInventory>()
                        .eq(SeatInventory::getScheduleId, scheduleId)
                        .eq(SeatInventory::getSeatTypeCode, request.seatTypeCode())
        );
        if (existingCount > 0) {
            throw new BusinessException(409, "该运行计划的此席别库存已初始化");
        }

        List<TrainStop> trainStops = trainStopMapper.selectList(
                new LambdaQueryWrapper<TrainStop>()
                        .eq(TrainStop::getTrainId, trainId)
                        .orderByAsc(TrainStop::getStopIndex)
        );
        if (trainStops.size() < 2) {
            throw new BusinessException(400, "该车次至少需要配置两个经停站才能初始化库存");
        }

        List<SeatInventory> inventories = new ArrayList<>(trainStops.size() - 1);
        for (int index = 0; index < trainStops.size(); index++) {
            if (!Integer.valueOf(index).equals(trainStops.get(index).getStopIndex())) {
                throw new BusinessException(500, "车次经停站顺序不连续，无法初始化库存");
            }
            if (index == trainStops.size() - 1) {
                continue;
            }

            SeatInventory seatInventory = new SeatInventory();
            seatInventory.setScheduleId(scheduleId);
            seatInventory.setSeatTypeCode(request.seatTypeCode());
            seatInventory.setSegmentIndex(index);
            seatInventory.setTotal(request.total());
            seatInventory.setAvailable(request.total());

            int rows = seatInventoryMapper.insert(seatInventory);
            if (rows == 0) {
                throw new BusinessException(500, "初始化区段库存失败");
            }
            inventories.add(seatInventory);
        }

        searchCacheService.evictScheduleAfterCommit(scheduleId);
        return inventories;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SeatInventory> updateSeatInventoryTotal(Long trainId, Long scheduleId, String seatTypeCode, Integer total) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (scheduleId == null) {
            throw new BusinessException(400, "运行计划 ID 不能为空");
        }
        if (!SeatType.contains(seatTypeCode)) {
            throw new BusinessException(400, "不支持的席别编码");
        }

        getTrainScheduleOrThrow(trainId, scheduleId);

        List<SeatInventory> inventories = seatInventoryMapper.selectList(
                new LambdaQueryWrapper<SeatInventory>()
                        .eq(SeatInventory::getScheduleId, scheduleId)
                        .eq(SeatInventory::getSeatTypeCode, seatTypeCode)
                        .orderByAsc(SeatInventory::getSegmentIndex)
                        .last("FOR UPDATE")
        );
        if (inventories.isEmpty()) {
            throw new BusinessException(404, "该运行计划尚未初始化此席别库存");
        }

        for (SeatInventory inventory : inventories) {
            int sold = inventory.getTotal() - inventory.getAvailable();
            if (total < sold) {
                throw new BusinessException(
                        409,
                        "区段 " + inventory.getSegmentIndex()
                                + " 已售 " + sold + " 张，总库存不能调整为 " + total
                );
            }
        }

        for (SeatInventory inventory : inventories) {
            int sold = inventory.getTotal() - inventory.getAvailable();
            if (total.equals(inventory.getTotal())) {
                continue;
            }

            inventory.setTotal(total);
            inventory.setAvailable(total - sold);

            int rows = seatInventoryMapper.updateById(inventory);
            if (rows == 0) {
                throw new BusinessException(500, "更新区段库存失败");
            }
        }

        searchCacheService.evictScheduleAfterCommit(scheduleId);
        return inventories;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSeatInventory(Long trainId, Long scheduleId, String seatTypeCode) {
        if (trainId == null) {
            throw new BusinessException(400, "车次 ID 不能为空");
        }
        if (scheduleId == null) {
            throw new BusinessException(400, "运行计划 ID 不能为空");
        }
        if (!SeatType.contains(seatTypeCode)) {
            throw new BusinessException(400, "不支持的席别编码");
        }

        TrainSchedule trainSchedule = getTrainScheduleOrThrow(trainId, scheduleId);
        if (!Integer.valueOf(0).equals(trainSchedule.getStatus())) {
            throw new BusinessException(409, "请先将运行计划设置为停运或停售");
        }

        List<SeatInventory> inventories = seatInventoryMapper.selectList(
                new LambdaQueryWrapper<SeatInventory>()
                        .eq(SeatInventory::getScheduleId, scheduleId)
                        .eq(SeatInventory::getSeatTypeCode, seatTypeCode)
                        .orderByAsc(SeatInventory::getSegmentIndex)
                        .last("FOR UPDATE")
        );
        if (inventories.isEmpty()) {
            throw new BusinessException(404, "该运行计划尚未初始化此席别库存");
        }

        for (SeatInventory inventory : inventories) {
            if (!inventory.getTotal().equals(inventory.getAvailable())) {
                throw new BusinessException(409, "该席别已经产生售票记录，不能删除库存");
            }
        }

        int rows = seatInventoryMapper.delete(
                new LambdaQueryWrapper<SeatInventory>()
                        .eq(SeatInventory::getScheduleId, scheduleId)
                        .eq(SeatInventory::getSeatTypeCode, seatTypeCode)
        );
        if (rows != inventories.size()) {
            throw new BusinessException(500, "删除区段库存失败");
        }
        searchCacheService.evictScheduleAfterCommit(scheduleId);
    }

    /** 校验运行计划存在并且属于指定车次。 */
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
