package com.ahrose.railticket.train.schedule.service;

import com.ahrose.railticket.bean.TrainSchedule;
import com.ahrose.railticket.bean.request.TrainScheduleCreateRequest;

import java.time.LocalDate;
import java.util.List;

/** 车次运行日业务接口。 */
public interface TrainScheduleService {

    /**
     * 按可选日期范围查询指定车次的运行计划。
     *
     * @param trainId  车次雪花 ID
     * @param startDate 查询起始日期；为空时不限制起始日期
     * @param endDate   查询结束日期；为空时不限制结束日期
     * @return {@code List<TrainSchedule>}，按始发日期升序排列的运行计划列表
     */
    List<TrainSchedule> listTrainSchedules(Long trainId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询指定车次的一条运行计划。
     *
     * @param trainId    车次雪花 ID
     * @param scheduleId 运行计划雪花 ID
     * @return {@code TrainSchedule}，属于指定车次的运行计划
     */
    TrainSchedule getTrainSchedule(Long trainId, Long scheduleId);

    /**
     * 为指定车次新增一个运行日，创建后默认可售。
     *
     * @param trainId 车次雪花 ID
     * @param request 已通过 Controller 参数校验的运行计划新增请求
     * @return {@code TrainSchedule}，保存成功后的运行计划，包含生成的雪花 ID
     */
    TrainSchedule createTrainSchedule(Long trainId, TrainScheduleCreateRequest request);

    /**
     * 修改指定运行日的状态，状态为 1 时可售，为 0 时停运或停售。
     *
     * @param trainId    车次雪花 ID
     * @param scheduleId 运行计划雪花 ID
     * @param status     运行计划状态，1 表示可售，0 表示停运或停售
     * @return {@code TrainSchedule}，状态更新后的运行计划
     */
    TrainSchedule updateTrainScheduleStatus(Long trainId, Long scheduleId, Integer status);

    /**
     * 删除指定车次尚未产生关联业务数据的运行计划。
     * 返回类型为 {@code void}，删除成功时不返回业务数据。
     *
     * @param trainId    车次雪花 ID
     * @param scheduleId 运行计划雪花 ID
     */
    void deleteTrainSchedule(Long trainId, Long scheduleId);
}
