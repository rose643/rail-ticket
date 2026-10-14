package com.ahrose.railticket.train.schedule.controller;

import com.ahrose.railticket.bean.request.TrainScheduleCreateRequest;
import com.ahrose.railticket.bean.request.TrainScheduleStatusUpdateRequest;
import com.ahrose.railticket.bean.response.TrainScheduleResponse;
import com.ahrose.railticket.common.result.Result;
import com.ahrose.railticket.train.schedule.service.TrainScheduleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 管理员维护车次运行日期的接口。 */
@RestController
@RequestMapping("/api/admin/trains/{trainId}/schedules")
public class TrainScheduleController {

    private final TrainScheduleService trainScheduleService;

    public TrainScheduleController(TrainScheduleService trainScheduleService) {
        this.trainScheduleService = trainScheduleService;
    }

    /** 查询指定车次的运行计划，可按始发日期范围筛选。 */
    @GetMapping
    public Result<List<TrainScheduleResponse>> listTrainSchedules(
            @PathVariable Long trainId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TrainScheduleResponse> schedules = trainScheduleService
                .listTrainSchedules(trainId, startDate, endDate)
                .stream()
                .map(TrainScheduleResponse::from)
                .toList();
        return Result.success(schedules);
    }

    /** 查询指定车次的一条运行计划。 */
    @GetMapping("/{scheduleId}")
    public Result<TrainScheduleResponse> getTrainSchedule(
            @PathVariable Long trainId,
            @PathVariable Long scheduleId) {
        return Result.success(TrainScheduleResponse.from(
                trainScheduleService.getTrainSchedule(trainId, scheduleId)
        ));
    }

    /** 为指定车次新增一个运行日，创建后默认可售。 */
    @PostMapping
    public Result<TrainScheduleResponse> createTrainSchedule(
            @PathVariable Long trainId,
            @Valid @RequestBody TrainScheduleCreateRequest request) {
        return Result.success(TrainScheduleResponse.from(
                trainScheduleService.createTrainSchedule(trainId, request)
        ));
    }

    /** 将指定运行日设置为可售或停运、停售。 */
    @PatchMapping("/{scheduleId}/status")
    public Result<TrainScheduleResponse> updateTrainScheduleStatus(
            @PathVariable Long trainId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody TrainScheduleStatusUpdateRequest request) {
        return Result.success(TrainScheduleResponse.from(
                trainScheduleService.updateTrainScheduleStatus(trainId, scheduleId, request.status())
        ));
    }

    /** 删除指定车次的一条运行计划。 */
    @DeleteMapping("/{scheduleId}")
    public Result<Void> deleteTrainSchedule(
            @PathVariable Long trainId,
            @PathVariable Long scheduleId) {
        trainScheduleService.deleteTrainSchedule(trainId, scheduleId);
        return Result.success();
    }
}
