package com.ahrose.railticket.bean.response;

import com.ahrose.railticket.bean.TrainStop;

/** 对外返回的车次经停站基础信息。 */
public record TrainStopResponse(
        Long id,
        Long trainId,
        Long stationId,
        Integer stopIndex,
        Integer arrivalOffsetMinutes,
        Integer departureOffsetMinutes
) {
    public static TrainStopResponse from(TrainStop trainStop) {
        return new TrainStopResponse(
                trainStop.getId(),
                trainStop.getTrainId(),
                trainStop.getStationId(),
                trainStop.getStopIndex(),
                trainStop.getArrivalOffsetMinutes(),
                trainStop.getDepartureOffsetMinutes()
        );
    }
}
