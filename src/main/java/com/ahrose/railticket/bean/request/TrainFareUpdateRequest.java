package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** 修改车次区间票价请求，只允许调整价格。 */
public record TrainFareUpdateRequest(
        @NotNull(message = "票价不能为空")
        @DecimalMin(value = "0.00", message = "票价不能小于 0")
        @Digits(integer = 8, fraction = 2, message = "票价最多为 8 位整数和 2 位小数")
        BigDecimal price
) {
}
