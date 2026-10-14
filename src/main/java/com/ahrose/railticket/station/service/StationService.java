package com.ahrose.railticket.station.service;

import com.ahrose.railticket.bean.Station;
import com.ahrose.railticket.bean.request.StationCreateRequest;
import com.ahrose.railticket.bean.request.StationUpdateRequest;
import com.ahrose.railticket.bean.response.StationSuggestionResponse;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 车站业务接口。
 * <p>
 * 该接口由 Controller 调用，具体的数据访问和业务校验在实现类中完成。
 */
public interface StationService extends IService<Station> {

    /**
     * 管理员查询全部车站，包含停用车站。
     *
     * @param keyword 按编码、名称或城市模糊匹配；为空时返回全部
     * @return {@code List<Station>}，按城市和名称排序的车站列表
     */
    List<Station> listStations(String keyword);

    /**
     * 查询所有处于启用状态的车站。
     *
     * @param keyword 按车站编码、名称或所属城市模糊匹配；为空时不按关键字过滤
     * @param city    所属城市；为空时不按城市过滤
     * @return {@code List<Station>}，符合筛选条件的启用车站列表
     */
    List<Station> listEnabledStations(String keyword, String city);

    /**
     * 根据用户输入生成查票地点候选项。
     * <p>
     * 当关键字匹配城市时返回“城市（全部车站）”候选项，同时返回匹配的具体车站候选项。
     *
     * @param keyword 城市、车站名称或车站编码关键字
     * @return {@code List<StationSuggestionResponse>}，城市候选项排在具体车站候选项之前
     */
    List<StationSuggestionResponse> listStationSuggestions(String keyword);

    /**
     * 根据主键查询一个启用状态的车站。
     *
     * @param id 车站雪花 ID
     * @return {@code Station}，启用状态的车站信息；不存在或已停用时由实现类抛出业务异常
     */
    Station getEnabledStation(Long id);

    /**
     * 新增车站。
     *
     * @param request 已通过 Controller 参数校验的新增车站请求
     * @return {@code Station}，保存成功后的车站，包含 MyBatis-Plus 生成的雪花 ID
     */
    Station createStation(StationCreateRequest request);

    /**
     * 修改已有车站的名称和所属城市。
     * <p>
     * 车站编码是业务唯一标识，创建后不允许在本方法中修改。
     *
     * @param id      车站雪花 ID
     * @param request 已通过 Controller 参数校验的修改车站请求
     * @return {@code Station}，更新后的完整车站信息
     */
    Station updateStation(Long id, StationUpdateRequest request);

    /**
     * 启用或停用指定车站；不直接删除车站，以保留历史车次和订单关联。
     *
     * @param id     车站雪花 ID
     * @param status 车站状态，1 表示启用，0 表示停用
     * @return {@code Station}，状态更新后的车站信息
     */
    Station updateStationStatus(Long id, Integer status);
}
