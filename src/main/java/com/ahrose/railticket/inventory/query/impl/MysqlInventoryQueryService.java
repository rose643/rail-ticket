package com.ahrose.railticket.inventory.query.impl;

import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.inventory.mapper.SeatInventoryMapper;
import com.ahrose.railticket.inventory.query.InventoryQueryService;
import com.ahrose.railticket.inventory.query.model.SeatTypeAvailability;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 基于 MySQL 的区间余票只读查询实现。 */
@Service
public class MysqlInventoryQueryService implements InventoryQueryService {

    private final SeatInventoryMapper seatInventoryMapper;

    public MysqlInventoryQueryService(SeatInventoryMapper seatInventoryMapper) {
        this.seatInventoryMapper = seatInventoryMapper;
    }

    @Override
    public Map<String, Integer> getAvailableBySeatType(
            Long scheduleId,
            Integer fromStopIndex,
            Integer toStopIndex
    ) {
        if (scheduleId == null) {
            throw new BusinessException(400, "运行计划 ID 不能为空");
        }
        if (fromStopIndex == null || toStopIndex == null || fromStopIndex >= toStopIndex) {
            throw new BusinessException(400, "出发站顺序必须小于到达站顺序");
        }

        List<SeatTypeAvailability> rows = seatInventoryMapper.selectAvailableBySeatType(
                scheduleId,
                fromStopIndex,
                toStopIndex
        );

        Map<String, Integer> result = new LinkedHashMap<>();
        for (SeatTypeAvailability row : rows) {
            result.put(row.getSeatTypeCode(), row.getAvailable());
        }
        return result;
    }
}
