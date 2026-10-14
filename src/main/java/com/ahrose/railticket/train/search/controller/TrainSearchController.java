package com.ahrose.railticket.train.search.controller;

import com.ahrose.railticket.bean.response.TrainSearchResponse;
import com.ahrose.railticket.common.result.Result;
import com.ahrose.railticket.common.enums.TrainSearchLocationType;
import com.ahrose.railticket.train.search.service.TrainSearchService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 面向普通用户的车票查询接口。 */
@Validated
@RestController
@RequestMapping("/api/trains")
public class TrainSearchController {

    private final TrainSearchService trainSearchService;

    public TrainSearchController(TrainSearchService trainSearchService) {
        this.trainSearchService = trainSearchService;
    }

    /** 按出发地点、到达地点和始发日期查询可售车次、票价及余票。 */
    @GetMapping("/search")
    public Result<List<TrainSearchResponse>> searchTrains(
            @RequestParam(defaultValue = "STATION")
            @NotBlank(message = "出发地点类型不能为空")
            String fromType,

            @RequestParam(required = false)
            @Size(max = 64, message = "出发地点不能超过 64 个字符")
            String fromValue,

            @RequestParam(defaultValue = "STATION")
            @NotBlank(message = "到达地点类型不能为空")
            String toType,

            @RequestParam(required = false)
            @Size(max = 64, message = "到达地点不能超过 64 个字符")
            String toValue,

            @RequestParam(required = false)
            @Size(max = 10, message = "出发站编码不能超过 10 个字符")
            String fromStationCode,

            @RequestParam(required = false)
            @Size(max = 10, message = "到达站编码不能超过 10 个字符")
            String toStationCode,

            @RequestParam
            @NotNull(message = "始发日期不能为空")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        // 兼容改造前的 fromStationCode/toStationCode 请求；新接口优先使用 fromValue/toValue。
        boolean useLegacyFrom = fromValue == null || fromValue.isBlank();
        boolean useLegacyTo = toValue == null || toValue.isBlank();
        String resolvedFromValue = useLegacyFrom ? fromStationCode : fromValue;
        String resolvedToValue = useLegacyTo ? toStationCode : toValue;
        TrainSearchLocationType resolvedFromType = useLegacyFrom
                ? TrainSearchLocationType.STATION
                : TrainSearchLocationType.from(fromType, "出发地点类型");
        TrainSearchLocationType resolvedToType = useLegacyTo
                ? TrainSearchLocationType.STATION
                : TrainSearchLocationType.from(toType, "到达地点类型");

        return Result.success(
                trainSearchService.searchTrains(
                        resolvedFromType,
                        resolvedFromValue,
                        resolvedToType,
                        resolvedToValue,
                        date
                )
        );
    }
}
