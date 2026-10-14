package com.ahrose.railticket.train.base.controller;

import com.ahrose.railticket.bean.request.TrainCreateRequest;
import com.ahrose.railticket.bean.request.TrainStatusUpdateRequest;
import com.ahrose.railticket.bean.request.TrainUpdateRequest;
import com.ahrose.railticket.bean.response.TrainResponse;
import com.ahrose.railticket.common.result.Result;
import com.ahrose.railticket.train.base.service.TrainService;
import jakarta.validation.Valid;
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
 * 车次基础信息接口。
 * <p>
 * 后续的 TrainService 需要提供：listTrains、getTrainById、
 * createTrain、updateTrain、updateTrainStatus 方法。
 */
@RestController
@RequestMapping("/api")
public class TrainController {

    private final TrainService trainService;

    public TrainController(TrainService trainService) {
        this.trainService = trainService;
    }

    /** 面向管理员：按车次号或名称查询车次，包含停用车次。 */
    @GetMapping("/admin/trains")
    public Result<List<TrainResponse>> listTrains(@RequestParam(required = false) String keyword) {
        List<TrainResponse> trains = trainService.listTrains(keyword)
                .stream()
                .map(TrainResponse::from)
                .toList();
        return Result.success(trains);
    }

    /** 面向管理员：查询一个车次的基础信息，包含停用车次。 */
    @GetMapping("/admin/trains/{id}")
    public Result<TrainResponse> getTrain(@PathVariable Long id) {
        return Result.success(TrainResponse.from(trainService.getTrainById(id)));
    }

    /** 面向管理员：新增车次，创建后默认启用。 */
    @PostMapping("/admin/trains")
    public Result<TrainResponse> createTrain(@Valid @RequestBody TrainCreateRequest request) {
        return Result.success(TrainResponse.from(trainService.createTrain(request)));
    }

    /** 面向管理员：修改车次名称；车次号创建后不允许修改。 */
    @PutMapping("/admin/trains/{id}")
    public Result<TrainResponse> updateTrain(
            @PathVariable Long id,
            @Valid @RequestBody TrainUpdateRequest request) {
        return Result.success(TrainResponse.from(trainService.updateTrain(id, request)));
    }

    /** 面向管理员：启用或停用车次。 */
    @PatchMapping("/admin/trains/{id}/status")
    public Result<TrainResponse> updateTrainStatus(
            @PathVariable Long id,
            @Valid @RequestBody TrainStatusUpdateRequest request) {
        return Result.success(TrainResponse.from(trainService.updateTrainStatus(id, request.status())));
    }
}
