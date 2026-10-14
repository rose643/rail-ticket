package com.ahrose.railticket.train.stop.service;

import com.ahrose.railticket.bean.TrainStop;
import com.ahrose.railticket.bean.request.TrainStopCreateRequest;
import com.ahrose.railticket.bean.request.TrainStopUpdateRequest;

import java.util.List;

/**
 * 车次经停站业务接口。
 * <p>
 * 负责维护车次经过的车站、站点顺序和相对始发时刻；不负责运行日、票价和库存。
 */
public interface TrainStopService {

    /**
     * 查询指定车次的全部经停站。
     *
     * @param trainId 车次雪花 ID
     * @return {@code List<TrainStop>}，按 stopIndex 升序排列的经停站列表
     */
    List<TrainStop> listTrainStops(Long trainId);

    /**
     * 为指定车次新增一个经停站。
     *
     * @param trainId 车次雪花 ID
     * @param request 已通过 Controller 参数校验的新增经停站请求
     * @return {@code TrainStop}，保存成功后的经停站记录，包含 MyBatis-Plus 生成的雪花 ID
     */
    TrainStop createTrainStop(Long trainId, TrainStopCreateRequest request);

    /**
     * 修改指定车次的一个经停站。
     * <p>
     * 实现类应校验 stopId 确实属于 trainId，避免越权修改其他车次的线路数据。
     *
     * @param trainId 车次雪花 ID
     * @param stopId  经停站记录雪花 ID
     * @param request 已通过 Controller 参数校验的修改经停站请求
     * @return {@code TrainStop}，更新后的经停站记录
     */
    TrainStop updateTrainStop(Long trainId, Long stopId, TrainStopUpdateRequest request);

    /**
     * 删除指定车次的一个经停站。
     * <p>
     * 实现类应校验 stopId 属于 trainId；删除后需要保证剩余经停站顺序仍然连续。
     * 返回类型为 {@code void}，删除成功时不返回业务数据。
     *
     * @param trainId 车次雪花 ID
     * @param stopId  经停站记录雪花 ID
     */
    void deleteTrainStop(Long trainId, Long stopId);
}
