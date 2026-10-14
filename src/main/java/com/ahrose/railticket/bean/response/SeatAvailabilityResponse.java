package com.ahrose.railticket.bean.response;

import java.math.BigDecimal;

/** 查票结果中某一席别的票价和余票信息。 */
public record SeatAvailabilityResponse(
        String seatTypeCode,
        String seatTypeName,
        BigDecimal price,
        Integer available
) {
}
