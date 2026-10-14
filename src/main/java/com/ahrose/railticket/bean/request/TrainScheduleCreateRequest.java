package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** 新增车次运行日请求。 */
public record TrainScheduleCreateRequest(
        @NotNull(message = "运行日期不能为空") LocalDate travelDate
) {
}
