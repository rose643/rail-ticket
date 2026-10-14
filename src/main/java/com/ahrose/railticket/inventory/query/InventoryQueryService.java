package com.ahrose.railticket.inventory.query;

import java.util.Map;

/** 为查票等只读场景提供区间余票查询能力。 */
public interface InventoryQueryService {

    /**
     * 查询指定运行计划在乘车区间内各席别的可售数量。
     * <p>
     * 每个席别的余票等于 {@code [fromStopIndex, toStopIndex)} 内所有相邻区段
     * {@code available} 的最小值。
     *
     * @param scheduleId   运行计划雪花 ID
     * @param fromStopIndex 出发站顺序，包含该区段
     * @param toStopIndex   到达站顺序，不包含该区段
     * @return {@code Map<String, Integer>}，键为席别编码，值为该乘车区间的可售数量
     */
    Map<String, Integer> getAvailableBySeatType(
            Long scheduleId,
            Integer fromStopIndex,
            Integer toStopIndex
    );
}
