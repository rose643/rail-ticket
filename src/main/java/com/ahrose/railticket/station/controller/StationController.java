package com.ahrose.railticket.station.controller;

import com.ahrose.railticket.bean.request.StationCreateRequest;
import com.ahrose.railticket.bean.request.StationStatusUpdateRequest;
import com.ahrose.railticket.bean.request.StationUpdateRequest;
import com.ahrose.railticket.bean.response.StationResponse;
import com.ahrose.railticket.bean.response.StationSuggestionResponse;
import com.ahrose.railticket.common.result.Result;
import com.ahrose.railticket.station.service.StationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 车站接口。
 * <p>
 * 你后续编写的 StationService 需要提供以下方法：
 * listEnabledStations、getEnabledStation、createStation、updateStation、updateStationStatus。
 */
@RestController
@RequestMapping("/api")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    /** 面向普通用户：查询已启用车站，可按关键字和城市筛选。 */
    @GetMapping("/stations")
    public Result<List<StationResponse>> listStations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city) {
        List<StationResponse> stations = stationService.listEnabledStations(keyword, city)
                .stream()
                .map(StationResponse::from)
                .toList();
        return Result.success(stations);
    }

    /** 面向普通用户：根据输入内容返回“整个城市”和“具体车站”两类查票候选项。 */
    @GetMapping("/stations/suggestions")
    public Result<List<StationSuggestionResponse>> listStationSuggestions(
            @RequestParam
            @NotBlank(message = "搜索关键字不能为空")
            @Size(max = 64, message = "搜索关键字不能超过 64 个字符")
            String keyword) {
        return Result.success(stationService.listStationSuggestions(keyword));
    }

    /** 面向普通用户：查询已启用车站详情。 */
    @GetMapping("/stations/{id}")
    public Result<StationResponse> getStation(@PathVariable Long id) {
        return Result.success(StationResponse.from(stationService.getEnabledStation(id)));
    }

    /** 面向管理员：查询全部车站，包含停用车站。 */
    @GetMapping("/admin/stations")
    public Result<List<StationResponse>> listAdminStations(
            @RequestParam(required = false) String keyword
    ) {
        return Result.success(stationService.listStations(keyword)
                .stream()
                .map(StationResponse::from)
                .toList());
    }

    /** 面向管理员：新增车站。 */
    @PostMapping("/admin/stations")
    public Result<StationResponse> createStation(@Valid @RequestBody StationCreateRequest request) {
        return Result.success(StationResponse.from(stationService.createStation(request)));
    }

    /** 面向管理员：修改车站名称和城市；车站编码创建后不允许修改。 */
    @PutMapping("/admin/stations/{id}")
    public Result<StationResponse> updateStation(
            @PathVariable Long id,
            @Valid @RequestBody StationUpdateRequest request) {
        return Result.success(StationResponse.from(stationService.updateStation(id, request)));
    }

    /** 面向管理员：启用或停用车站，0 为停用，1 为启用。 */
    @PatchMapping("/admin/stations/{id}/status")
    public Result<StationResponse> updateStationStatus(
            @PathVariable Long id,
            @Valid @RequestBody StationStatusUpdateRequest request) {
        return Result.success(StationResponse.from(stationService.updateStationStatus(id, request.status())));
    }
}
