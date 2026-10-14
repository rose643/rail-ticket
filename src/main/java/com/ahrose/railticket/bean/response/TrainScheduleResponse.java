package com.ahrose.railticket.bean.response;

import com.ahrose.railticket.bean.TrainSchedule;

import java.time.LocalDate;

/** 对外返回的车次运行日信息。 */
public record TrainScheduleResponse(
        Long id,
        Long trainId,
        LocalDate travelDate,
        Integer status
) {

    public static TrainScheduleResponse from(TrainSchedule trainSchedule) {
        return new TrainScheduleResponse(
                trainSchedule.getId(),
                trainSchedule.getTrainId(),
                trainSchedule.getTravelDate(),
                trainSchedule.getStatus()
        );
    }
}
