package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 修改车次请求。车次号创建后不允许修改。 */
public record TrainUpdateRequest(
        @NotBlank(message = "车次名称不能为空") @Size(max = 64, message = "车次名称不能超过 64 个字符") String name
) {
}
