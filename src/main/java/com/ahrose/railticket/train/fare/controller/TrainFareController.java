package com.ahrose.railticket.train.fare.controller;

import com.ahrose.railticket.bean.request.TrainFareCreateRequest;
import com.ahrose.railticket.bean.request.TrainFareUpdateRequest;
import com.ahrose.railticket.bean.response.TrainFareResponse;
import com.ahrose.railticket.common.result.Result;
import com.ahrose.railticket.train.fare.service.TrainFareService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 管理员维护车次区间票价的接口。 */
@RestController
@RequestMapping("/api/admin/trains/{trainId}/fares")
public class TrainFareController {

    private final TrainFareService trainFareService;

    public TrainFareController(TrainFareService trainFareService) {
        this.trainFareService = trainFareService;
    }

    /** 查询指定车次的票价，可按席别和乘车区间筛选。 */
    @GetMapping
    public Result<List<TrainFareResponse>> listTrainFares(
            @PathVariable Long trainId,
            @RequestParam(required = false) String seatTypeCode,
            @RequestParam(required = false) Integer fromStopIndex,
            @RequestParam(required = false) Integer toStopIndex) {
        List<TrainFareResponse> fares = trainFareService
                .listTrainFares(trainId, seatTypeCode, fromStopIndex, toStopIndex)
                .stream()
                .map(TrainFareResponse::from)
                .toList();
        return Result.success(fares);
    }

    /** 查询指定车次的一条票价记录。 */
    @GetMapping("/{fareId}")
    public Result<TrainFareResponse> getTrainFare(
            @PathVariable Long trainId,
            @PathVariable Long fareId) {
        return Result.success(TrainFareResponse.from(
                trainFareService.getTrainFare(trainId, fareId)
        ));
    }

    /** 为指定车次新增一条席别区间票价。 */
    @PostMapping
    public Result<TrainFareResponse> createTrainFare(
            @PathVariable Long trainId,
            @Valid @RequestBody TrainFareCreateRequest request) {
        return Result.success(TrainFareResponse.from(
                trainFareService.createTrainFare(trainId, request)
        ));
    }

    /** 修改指定票价记录的金额，不修改席别和乘车区间。 */
    @PutMapping("/{fareId}")
    public Result<TrainFareResponse> updateTrainFare(
            @PathVariable Long trainId,
            @PathVariable Long fareId,
            @Valid @RequestBody TrainFareUpdateRequest request) {
        return Result.success(TrainFareResponse.from(
                trainFareService.updateTrainFare(trainId, fareId, request)
        ));
    }

    /** 删除指定车次的一条票价记录。 */
    @DeleteMapping("/{fareId}")
    public Result<Void> deleteTrainFare(
            @PathVariable Long trainId,
            @PathVariable Long fareId) {
        trainFareService.deleteTrainFare(trainId, fareId);
        return Result.success();
    }
}
