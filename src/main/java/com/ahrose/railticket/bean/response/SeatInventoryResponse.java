package com.ahrose.railticket.bean.response;

import com.ahrose.railticket.bean.SeatInventory;

/** 对外返回的运行日区段库存。 */
public record SeatInventoryResponse(
        Long id,
        Long scheduleId,
        String seatTypeCode,
        Integer segmentIndex,
        Integer total,
        Integer available
) {

    public static SeatInventoryResponse from(SeatInventory seatInventory) {
        return new SeatInventoryResponse(
                seatInventory.getId(),
                seatInventory.getScheduleId(),
                seatInventory.getSeatTypeCode(),
                seatInventory.getSegmentIndex(),
                seatInventory.getTotal(),
                seatInventory.getAvailable()
        );
    }
}
