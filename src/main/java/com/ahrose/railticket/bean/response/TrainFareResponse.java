package com.ahrose.railticket.bean.response;

import com.ahrose.railticket.bean.TrainFare;

import java.math.BigDecimal;

/** 对外返回的车次区间票价。 */
public record TrainFareResponse(
        Long id,
        Long trainId,
        String seatTypeCode,
        Integer fromStopIndex,
        Integer toStopIndex,
        BigDecimal price
) {

    public static TrainFareResponse from(TrainFare trainFare) {
        return new TrainFareResponse(
                trainFare.getId(),
                trainFare.getTrainId(),
                trainFare.getSeatTypeCode(),
                trainFare.getFromStopIndex(),
                trainFare.getToStopIndex(),
                trainFare.getPrice()
        );
    }
}
