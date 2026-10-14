package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 初始化指定运行日某一席别库存的请求。 */
public record SeatInventoryInitializeRequest(
        @NotBlank(message = "席别编码不能为空")
        @Size(max = 32, message = "席别编码不能超过 32 个字符")
        String seatTypeCode,

        @NotNull(message = "总库存不能为空")
        @Min(value = 0, message = "总库存不能小于 0")
        Integer total
) {
}
