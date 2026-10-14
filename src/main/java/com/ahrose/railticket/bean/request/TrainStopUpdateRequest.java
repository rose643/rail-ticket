package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 修改指定经停站的请求。 */
public record TrainStopUpdateRequest(
        @NotNull(message = "车站 ID 不能为空") Long stationId,
        @NotNull(message = "经停站顺序不能为空") @Min(value = 0, message = "经停站顺序不能小于 0") Integer stopIndex,
        @Min(value = 0, message = "到达分钟数不能小于 0") Integer arrivalOffsetMinutes,
        @Min(value = 0, message = "发车分钟数不能小于 0") Integer departureOffsetMinutes
) {
}
