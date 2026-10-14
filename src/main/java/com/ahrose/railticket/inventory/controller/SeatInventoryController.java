package com.ahrose.railticket.inventory.controller;

import com.ahrose.railticket.bean.request.SeatInventoryInitializeRequest;
import com.ahrose.railticket.bean.request.SeatInventoryTotalUpdateRequest;
import com.ahrose.railticket.bean.response.SeatInventoryResponse;
import com.ahrose.railticket.common.result.Result;
import com.ahrose.railticket.inventory.service.SeatInventoryService;
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

/** 管理员维护运行日区段库存的接口。 */
@RestController
@RequestMapping("/api/admin/trains/{trainId}/schedules/{scheduleId}/inventories")
public class SeatInventoryController {

    private final SeatInventoryService seatInventoryService;

    public SeatInventoryController(SeatInventoryService seatInventoryService) {
        this.seatInventoryService = seatInventoryService;
    }

    /** 查询指定运行日的区段库存，可按席别筛选。 */
    @GetMapping
    public Result<List<SeatInventoryResponse>> listSeatInventories(
            @PathVariable Long trainId,
            @PathVariable Long scheduleId,
            @RequestParam(required = false) String seatTypeCode) {
        List<SeatInventoryResponse> inventories = seatInventoryService
                .listSeatInventories(trainId, scheduleId, seatTypeCode)
                .stream()
                .map(SeatInventoryResponse::from)
                .toList();
        return Result.success(inventories);
    }

    /** 为指定运行日初始化某一席别的全部区段库存。 */
    @PostMapping
    public Result<List<SeatInventoryResponse>> initializeSeatInventory(
            @PathVariable Long trainId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody SeatInventoryInitializeRequest request) {
        List<SeatInventoryResponse> inventories = seatInventoryService
                .initializeSeatInventory(trainId, scheduleId, request)
                .stream()
                .map(SeatInventoryResponse::from)
                .toList();
        return Result.success(inventories);
    }

    /** 调整指定运行日某一席别全部区段的总库存。 */
    @PutMapping("/{seatTypeCode}")
    public Result<List<SeatInventoryResponse>> updateSeatInventoryTotal(
            @PathVariable Long trainId,
            @PathVariable Long scheduleId,
            @PathVariable String seatTypeCode,
            @Valid @RequestBody SeatInventoryTotalUpdateRequest request) {
        List<SeatInventoryResponse> inventories = seatInventoryService
                .updateSeatInventoryTotal(trainId, scheduleId, seatTypeCode, request.total())
                .stream()
                .map(SeatInventoryResponse::from)
                .toList();
        return Result.success(inventories);
    }

    /** 删除指定运行日某一席别的全部区段库存。 */
    @DeleteMapping("/{seatTypeCode}")
    public Result<Void> deleteSeatInventory(
            @PathVariable Long trainId,
            @PathVariable Long scheduleId,
            @PathVariable String seatTypeCode) {
        seatInventoryService.deleteSeatInventory(trainId, scheduleId, seatTypeCode);
        return Result.success();
    }
}
