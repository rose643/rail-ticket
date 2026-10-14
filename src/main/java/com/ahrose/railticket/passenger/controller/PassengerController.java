package com.ahrose.railticket.passenger.controller;

import com.ahrose.railticket.bean.request.PassengerCreateRequest;
import com.ahrose.railticket.bean.request.PassengerPhoneUpdateRequest;
import com.ahrose.railticket.bean.response.PassengerResponse;
import com.ahrose.railticket.common.result.Result;
import com.ahrose.railticket.passenger.service.PassengerService;
import com.ahrose.railticket.security.CurrentUserId;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 当前登录用户的常用乘车人接口。 */
@RestController
@RequestMapping("/api/passengers")
public class PassengerController {

    private final PassengerService passengerService;

    public PassengerController(PassengerService passengerService) {
        this.passengerService = passengerService;
    }

    /** 查询当前用户的全部常用乘车人，返回证件号和手机号脱敏后的列表。 */
    @GetMapping
    public Result<List<PassengerResponse>> listPassengers(@CurrentUserId Long userId) {
        List<PassengerResponse> passengers = passengerService.listPassengers(userId)
                .stream()
                .map(PassengerResponse::from)
                .toList();
        return Result.success(passengers);
    }

    /** 查询当前用户的指定常用乘车人，返回脱敏后的乘车人信息。 */
    @GetMapping("/{passengerId}")
    public Result<PassengerResponse> getPassenger(
            @CurrentUserId Long userId,
            @PathVariable Long passengerId
    ) {
        return Result.success(PassengerResponse.from(
                passengerService.getPassenger(userId, passengerId)
        ));
    }

    /** 为当前用户新增常用乘车人，userId 从 JWT 获取而不是由前端传入。 */
    @PostMapping
    public Result<PassengerResponse> createPassenger(
            @CurrentUserId Long userId,
            @Valid @RequestBody PassengerCreateRequest request
    ) {
        return Result.success(PassengerResponse.from(
                passengerService.createPassenger(userId, request)
        ));
    }

    /** 修改属于当前用户的常用乘车人手机号，身份信息创建后不允许修改。 */
    @PatchMapping("/{passengerId}/phone")
    public Result<PassengerResponse> updatePassengerPhone(
            @CurrentUserId Long userId,
            @PathVariable Long passengerId,
            @Valid @RequestBody PassengerPhoneUpdateRequest request
    ) {
        return Result.success(PassengerResponse.from(
                passengerService.updatePassengerPhone(userId, passengerId, request)
        ));
    }

    /** 删除属于当前用户的常用乘车人，不影响已经保存乘车人快照的历史订单。 */
    @DeleteMapping("/{passengerId}")
    public Result<Void> deletePassenger(
            @CurrentUserId Long userId,
            @PathVariable Long passengerId
    ) {
        passengerService.deletePassenger(userId, passengerId);
        return Result.success();
    }
}
