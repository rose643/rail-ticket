package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 启用或停用车次请求。 */
public record TrainStatusUpdateRequest(
        @NotNull(message = "车次状态不能为空") @Min(value = 0, message = "车次状态只能为 0 或 1") @Max(value = 1, message = "车次状态只能为 0 或 1") Integer status
) {
}
