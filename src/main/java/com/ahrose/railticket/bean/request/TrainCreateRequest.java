package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 新增车次请求。 */
public record TrainCreateRequest(
        @NotBlank(message = "车次号不能为空") @Size(max = 32, message = "车次号不能超过 32 个字符") String trainNo,
        @NotBlank(message = "车次名称不能为空") @Size(max = 64, message = "车次名称不能超过 64 个字符") String name
) {
}
