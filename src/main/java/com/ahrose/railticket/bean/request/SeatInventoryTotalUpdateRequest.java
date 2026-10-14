package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 调整指定运行日某一席别总库存的请求。 */
public record SeatInventoryTotalUpdateRequest(
        @NotNull(message = "总库存不能为空")
        @Min(value = 0, message = "总库存不能小于 0")
        Integer total
) {
}
