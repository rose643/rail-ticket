package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 创建一张车票订单的请求；价格和订单状态由服务端计算。 */
public record TicketOrderCreateRequest(
        @NotNull(message = "乘车人 ID 不能为空")
        @Positive(message = "乘车人 ID 必须大于 0")
        Long passengerId,

        @NotNull(message = "运行计划 ID 不能为空")
        @Positive(message = "运行计划 ID 必须大于 0")
        Long scheduleId,

        @NotBlank(message = "席别编码不能为空")
        @Pattern(
                regexp = "^(SECOND_CLASS|FIRST_CLASS|BUSINESS_CLASS)$",
                message = "不支持的席别编码"
        )
        String seatTypeCode,

        @NotNull(message = "出发站顺序不能为空")
        @Min(value = 0, message = "出发站顺序不能小于 0")
        Integer fromStopIndex,

        @NotNull(message = "到达站顺序不能为空")
        @Min(value = 1, message = "到达站顺序必须大于 0")
        Integer toStopIndex,

        @NotBlank(message = "请求幂等键不能为空")
        @Size(max = 64, message = "请求幂等键不能超过 64 个字符")
        String requestId
) {
}
