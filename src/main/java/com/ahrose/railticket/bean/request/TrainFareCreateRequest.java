package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** 新增车次区间票价请求。 */
public record TrainFareCreateRequest(
        @NotBlank(message = "席别编码不能为空")
        @Size(max = 32, message = "席别编码不能超过 32 个字符")
        String seatTypeCode,

        @NotNull(message = "出发站顺序不能为空")
        @Min(value = 0, message = "出发站顺序不能小于 0")
        Integer fromStopIndex,

        @NotNull(message = "到达站顺序不能为空")
        @Min(value = 0, message = "到达站顺序不能小于 0")
        Integer toStopIndex,

        @NotNull(message = "票价不能为空")
        @DecimalMin(value = "0.00", message = "票价不能小于 0")
        @Digits(integer = 8, fraction = 2, message = "票价最多为 8 位整数和 2 位小数")
        BigDecimal price
) {
}
