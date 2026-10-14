package com.ahrose.railticket.train.fare.service;

import com.ahrose.railticket.bean.TrainFare;
import com.ahrose.railticket.bean.request.TrainFareCreateRequest;
import com.ahrose.railticket.bean.request.TrainFareUpdateRequest;

import java.util.List;

/** 车次区间票价业务接口。 */
public interface TrainFareService {

    /**
     * 按可选的席别和乘车区间查询指定车次的票价。
     *
     * @param trainId      车次雪花 ID
     * @param seatTypeCode 席别编码；为空时不按席别过滤
     * @param fromStopIndex 出发站顺序；为空时不按出发站过滤
     * @param toStopIndex   到达站顺序；为空时不按到达站过滤
     * @return {@code List<TrainFare>}，按区间和席别排序的票价列表
     */
    List<TrainFare> listTrainFares(
            Long trainId,
            String seatTypeCode,
            Integer fromStopIndex,
            Integer toStopIndex
    );

    /**
     * 查询指定车次的一条票价记录。
     *
     * @param trainId 车次雪花 ID
     * @param fareId  票价记录雪花 ID
     * @return {@code TrainFare}，属于指定车次的票价记录
     */
    TrainFare getTrainFare(Long trainId, Long fareId);

    /**
     * 为指定车次新增一条席别区间票价。
     *
     * @param trainId 车次雪花 ID
     * @param request 已通过 Controller 参数校验的新增票价请求
     * @return {@code TrainFare}，保存成功后的票价记录，包含生成的雪花 ID
     */
    TrainFare createTrainFare(Long trainId, TrainFareCreateRequest request);

    /**
     * 修改指定票价记录的金额。
     *
     * @param trainId 车次雪花 ID
     * @param fareId  票价记录雪花 ID
     * @param request 已通过 Controller 参数校验的票价修改请求
     * @return {@code TrainFare}，价格更新后的票价记录
     */
    TrainFare updateTrainFare(Long trainId, Long fareId, TrainFareUpdateRequest request);

    /**
     * 删除指定车次的一条票价记录。
     * 返回类型为 {@code void}，删除成功时不返回业务数据。
     *
     * @param trainId 车次雪花 ID
     * @param fareId  票价记录雪花 ID
     */
    void deleteTrainFare(Long trainId, Long fareId);
}
