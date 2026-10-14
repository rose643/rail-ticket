package com.ahrose.railticket.inventory.mapper;

import com.ahrose.railticket.bean.SeatInventory;
import com.ahrose.railticket.inventory.query.model.SeatTypeAvailability;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * ClassName: SeatInventoryMapper
 * Package: com.ahrose.railticket.inventory.mapper
 * Description:
 *
 * @Author 张浩然
 * @Create 2026/10/1 20:15
 * @Version 1.0
 */
@Mapper
public interface SeatInventoryMapper extends BaseMapper<SeatInventory> {

    /**
     * 查询乘车区间内各席别所有相邻区段的最小可售数量。
     *
     * @param scheduleId   运行计划雪花 ID
     * @param fromStopIndex 出发站顺序，包含该区段
     * @param toStopIndex   到达站顺序，不包含该区段
     * @return {@code List<SeatTypeAvailability>}，各席别对应的区间余票
     */
    @Select("""
            SELECT seat_type_code,
                   MIN(available) AS available
            FROM seat_inventory
            WHERE schedule_id = #{scheduleId}
              AND segment_index >= #{fromStopIndex}
              AND segment_index < #{toStopIndex}
            GROUP BY seat_type_code
            HAVING COUNT(*) = #{toStopIndex} - #{fromStopIndex}
            ORDER BY seat_type_code
            """)
    List<SeatTypeAvailability> selectAvailableBySeatType(
            @Param("scheduleId") Long scheduleId,
            @Param("fromStopIndex") Integer fromStopIndex,
            @Param("toStopIndex") Integer toStopIndex
    );

    /**
     * 原子扣减乘车区间内全部相邻区段库存。
     * <p>调用方必须校验影响行数等于 {@code toStopIndex - fromStopIndex}，否则回滚事务。</p>
     */
    @Update("""
            UPDATE seat_inventory
            SET available = available - 1
            WHERE schedule_id = #{scheduleId}
              AND seat_type_code = #{seatTypeCode}
              AND segment_index >= #{fromStopIndex}
              AND segment_index < #{toStopIndex}
              AND available > 0
            ORDER BY segment_index ASC
            """)
    int deductAvailableSegments(
            @Param("scheduleId") Long scheduleId,
            @Param("seatTypeCode") String seatTypeCode,
            @Param("fromStopIndex") Integer fromStopIndex,
            @Param("toStopIndex") Integer toStopIndex
    );

    /** 将订单占用的全部相邻区段库存原子恢复一张。 */
    @Update("""
            UPDATE seat_inventory
            SET available = available + 1
            WHERE schedule_id = #{scheduleId}
              AND seat_type_code = #{seatTypeCode}
              AND segment_index >= #{fromStopIndex}
              AND segment_index < #{toStopIndex}
              AND available < total
            ORDER BY segment_index ASC
            """)
    int restoreAvailableSegments(
            @Param("scheduleId") Long scheduleId,
            @Param("seatTypeCode") String seatTypeCode,
            @Param("fromStopIndex") Integer fromStopIndex,
            @Param("toStopIndex") Integer toStopIndex
    );
}
