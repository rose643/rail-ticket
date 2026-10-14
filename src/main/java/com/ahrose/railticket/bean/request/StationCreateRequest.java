package com.ahrose.railticket.bean.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 新增车站请求。 */
public record StationCreateRequest(
        @NotBlank(message = "车站编码不能为空") @Size(max = 10, message = "车站编码不能超过 10 个字符") String code,
        @NotBlank(message = "车站名称不能为空") @Size(max = 64, message = "车站名称不能超过 64 个字符") String name,
        @NotBlank(message = "所属城市不能为空") @Size(max = 64, message = "所属城市不能超过 64 个字符") String city
) {
}
