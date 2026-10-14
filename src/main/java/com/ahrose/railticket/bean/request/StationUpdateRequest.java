package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 修改车站请求。车站编码创建后不允许修改。 */
public record StationUpdateRequest(
        @NotBlank(message = "车站名称不能为空") @Size(max = 64, message = "车站名称不能超过 64 个字符") String name,
        @NotBlank(message = "所属城市不能为空") @Size(max = 64, message = "所属城市不能超过 64 个字符") String city
) {
}
