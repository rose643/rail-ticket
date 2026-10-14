package com.ahrose.railticket.inventory.service;

import com.ahrose.railticket.bean.SeatInventory;
import com.ahrose.railticket.bean.request.SeatInventoryInitializeRequest;

import java.util.List;

/** 运行日区段库存业务接口。 */
public interface SeatInventoryService {

    /**
     * 查询指定运行日的区段库存，可按席别筛选。
     *
     * @param trainId      车次雪花 ID
     * @param scheduleId   运行计划雪花 ID
     * @param seatTypeCode 席别编码；为空时查询全部席别
     * @return {@code List<SeatInventory>}，按席别和区段顺序排列的库存列表
     */
    List<SeatInventory> listSeatInventories(
            Long trainId,
            Long scheduleId,
            String seatTypeCode
    );

    /**
     * 为指定运行日初始化某一席别的全部区段库存。
     *
     * @param trainId    车次雪花 ID
     * @param scheduleId 运行计划雪花 ID
     * @param request    已通过 Controller 参数校验的库存初始化请求
     * @return {@code List<SeatInventory>}，初始化成功的全部区段库存列表
     */
    List<SeatInventory> initializeSeatInventory(
            Long trainId,
            Long scheduleId,
            SeatInventoryInitializeRequest request
    );

    /**
     * 调整指定运行日某一席别全部区段的总库存。
     *
     * @param trainId      车次雪花 ID
     * @param scheduleId   运行计划雪花 ID
     * @param seatTypeCode 需要调整的席别编码
     * @param total        调整后的总库存
     * @return {@code List<SeatInventory>}，调整后的全部区段库存列表
     */
    List<SeatInventory> updateSeatInventoryTotal(
            Long trainId,
            Long scheduleId,
            String seatTypeCode,
            Integer total
    );

    /**
     * 删除指定运行日某一席别的全部区段库存。
     * 返回类型为 {@code void}，删除成功时不返回业务数据。
     *
     * @param trainId      车次雪花 ID
     * @param scheduleId   运行计划雪花 ID
     * @param seatTypeCode 需要删除的席别编码
     */
    void deleteSeatInventory(
            Long trainId,
            Long scheduleId,
            String seatTypeCode
    );
}
