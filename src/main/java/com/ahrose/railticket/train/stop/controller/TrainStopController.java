package com.ahrose.railticket.train.stop.controller;

import com.ahrose.railticket.bean.request.TrainStopCreateRequest;
import com.ahrose.railticket.bean.request.TrainStopUpdateRequest;
import com.ahrose.railticket.bean.response.TrainStopResponse;
import com.ahrose.railticket.common.result.Result;
import com.ahrose.railticket.train.stop.service.TrainStopService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员维护车次经停站的接口。
 * <p>
 * 后续的 TrainStopService 需要提供：listTrainStops、createTrainStop、
 * updateTrainStop、deleteTrainStop 方法。
 */
@RestController
@RequestMapping("/api/admin/trains/{trainId}/stops")
public class TrainStopController {

    private final TrainStopService trainStopService;

    public TrainStopController(TrainStopService trainStopService) {
        this.trainStopService = trainStopService;
    }

    /** 查询指定车次的全部经停站，并按 stopIndex 升序返回。 */
    @GetMapping
    public Result<List<TrainStopResponse>> listTrainStops(@PathVariable Long trainId) {
        List<TrainStopResponse> stops = trainStopService.listTrainStops(trainId)
                .stream()
                .map(TrainStopResponse::from)
                .toList();
        return Result.success(stops);
    }

    /** 为指定车次新增经停站。 */
    @PostMapping
    public Result<TrainStopResponse> createTrainStop(
            @PathVariable Long trainId,
            @Valid @RequestBody TrainStopCreateRequest request) {
        return Result.success(TrainStopResponse.from(trainStopService.createTrainStop(trainId, request)));
    }

    /** 修改指定车次的一个经停站。 */
    @PutMapping("/{stopId}")
    public Result<TrainStopResponse> updateTrainStop(
            @PathVariable Long trainId,
            @PathVariable Long stopId,
            @Valid @RequestBody TrainStopUpdateRequest request) {
        return Result.success(TrainStopResponse.from(trainStopService.updateTrainStop(trainId, stopId, request)));
    }

    /** 删除指定车次的一个经停站。 */
    @DeleteMapping("/{stopId}")
    public Result<Void> deleteTrainStop(
            @PathVariable Long trainId,
            @PathVariable Long stopId) {
        trainStopService.deleteTrainStop(trainId, stopId);
        return Result.success();
    }
}
