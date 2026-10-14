package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 修改车次运行日状态请求。 */
public record TrainScheduleStatusUpdateRequest(
        @NotNull(message = "运行状态不能为空")
        @Min(value = 0, message = "运行状态只能为 0 或 1")
        @Max(value = 1, message = "运行状态只能为 0 或 1")
        Integer status
) {
}
