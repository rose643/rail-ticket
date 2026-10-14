package com.ahrose.railticket.train.base.service;

import com.ahrose.railticket.bean.Train;
import com.ahrose.railticket.bean.request.TrainCreateRequest;
import com.ahrose.railticket.bean.request.TrainUpdateRequest;

import java.util.List;

/**
 * 车次基础信息业务接口。
 * <p>
 * 本接口只管理车次本身，不处理经停站、运行日、票价和库存。
 */
public interface TrainService {

    /**
     * 查询车次基础信息，包含启用和停用车次，供管理员维护使用。
     *
     * @param keyword 按车次号或车次名称模糊匹配；为空时不按关键字过滤
     * @return {@code List<Train>}，符合条件的车次列表
     */
    List<Train> listTrains(String keyword);

    /**
     * 根据主键查询一个车次，包含停用车次。
     *
     * @param id 车次雪花 ID
     * @return {@code Train}，车次信息；不存在时由实现类抛出业务异常
     */
    Train getTrainById(Long id);

    /**
     * 新增车次，创建后默认设置为启用状态。
     *
     * @param request 已通过 Controller 参数校验的新增车次请求
     * @return {@code Train}，保存成功后的车次，包含 MyBatis-Plus 生成的雪花 ID
     */
    Train createTrain(TrainCreateRequest request);

    /**
     * 修改已有车次的展示名称。
     * <p>
     * trainNo 是业务唯一标识，创建后不允许在本方法中修改。
     *
     * @param id      车次雪花 ID
     * @param request 已通过 Controller 参数校验的修改车次请求
     * @return {@code Train}，更新后的车次信息
     */
    Train updateTrain(Long id, TrainUpdateRequest request);

    /**
     * 启用或停用指定车次；不直接删除车次，以保留运行日、票价和订单关联。
     *
     * @param id     车次雪花 ID
     * @param status 车次状态，1 表示启用，0 表示停用
     * @return {@code Train}，状态更新后的车次信息
     */
    Train updateTrainStatus(Long id, Integer status);
}
