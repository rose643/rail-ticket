package com.ahrose.railticket.station.service.impl;

import com.ahrose.railticket.bean.Station;
import com.ahrose.railticket.bean.request.StationCreateRequest;
import com.ahrose.railticket.bean.request.StationUpdateRequest;
import com.ahrose.railticket.bean.response.StationSuggestionResponse;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.station.mapper.StationMapper;
import com.ahrose.railticket.station.service.StationService;
import com.ahrose.railticket.train.stop.mapper.TrainStopMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * ClassName: StationServiceImpl
 * Package: com.ahrose.railticket.station.service.impl
 * Description:
 *
 * @Author 张浩然
 * @Create 2026/9/22 20:21
 * @Version 1.0
 */
@Service
public class StationServiceImpl extends ServiceImpl<StationMapper, Station> implements StationService {

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private TrainStopMapper trainStopMapper;

    @Override
    public List<Station> listStations(String keyword) {
        LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            wrapper.and(condition -> condition
                    .like(Station::getCode, normalizedKeyword)
                    .or()
                    .like(Station::getName, normalizedKeyword)
                    .or()
                    .like(Station::getCity, normalizedKeyword));
        }
        wrapper.orderByAsc(Station::getCity, Station::getName);
        return stationMapper.selectList(wrapper);
    }

    @Override
    public List<Station> listEnabledStations(String keyword, String city) {
        LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<>();
        // 始终只查询启用状态的车站
        wrapper.eq(Station::getStatus, 1);

        // keyword 不为空时：按编码、名称或城市模糊查询，用于前端城市/车站联想。
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            wrapper.and(condition -> condition
                    .like(Station::getCode, normalizedKeyword)
                    .or()
                    .like(Station::getName, normalizedKeyword)
                    .or()
                    .like(Station::getCity, normalizedKeyword));
        }

        // city 不为空时：按城市精确查询
        if (StringUtils.hasText(city)) {
            wrapper.eq(Station::getCity, city.trim());
        }

        wrapper.orderByAsc(Station::getCity, Station::getName);

        return stationMapper.selectList(wrapper);
    }

    @Override
    public List<StationSuggestionResponse> listStationSuggestions(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(400, "搜索关键字不能为空");
        }

        String normalizedKeyword = keyword.trim();
        String lowerKeyword = normalizedKeyword.toLowerCase(Locale.ROOT);
        List<Station> stations = listEnabledStations(normalizedKeyword, null);

        List<StationSuggestionResponse> citySuggestions = stations.stream()
                .map(Station::getCity)
                .distinct()
                .filter(city -> city.toLowerCase(Locale.ROOT).contains(lowerKeyword))
                .map(StationSuggestionResponse::city)
                .toList();
        List<StationSuggestionResponse> stationSuggestions = stations.stream()
                .map(StationSuggestionResponse::station)
                .toList();

        return Stream.concat(citySuggestions.stream(), stationSuggestions.stream())
                .toList();
    }

    @Override
    public Station getEnabledStation(Long id) {
        Station station = getStationById(id);
        if (!Integer.valueOf(1).equals(station.getStatus())) {
            throw new BusinessException(404, "车站不存在或已停用");
        }

        return station;
    }

    @Override
    public Station createStation(StationCreateRequest request) {
        if (request == null) {
            throw new BusinessException(400, "车站信息不能为空");
        }

        Station station = new Station();
        station.setCode(request.code());
        station.setName(request.name());
        station.setCity(request.city());
        station.setStatus(1);

        int insert = stationMapper.insert(station);
        if (insert > 0) {
            return station;
        } else {
            throw new BusinessException(500, "创建车站失败");
        }
    }

    @Override
    public Station updateStation(Long id, StationUpdateRequest request) {
        if (id == null || request == null) {
            throw new BusinessException(400, "车站信息或车站 ID 不能为空");
        }

        // 必须使用查询到的实体更新，才能携带 @Version 的当前版本号。
        Station existingStation = getStationById(id);

        existingStation.setName(request.name());
        existingStation.setCity(request.city());
        return updateWithOptimisticLock(existingStation, "车站信息已被其他操作修改，请刷新后重试");
    }

    @Override
    public Station updateStationStatus(Long id, Integer status) {
        if (id == null) {
            throw new BusinessException(400, "车站 ID 不能为空");
        }
        if (status == null) {
            throw new BusinessException(400, "车站状态不能为空");
        }
        if (status != 0 && status != 1) {
            throw new BusinessException(400, "车站状态只能为 0 或 1");
        }
        Station station = getStationById(id);

        // 仅在“启用 -> 停用”时检查，避免重复提交停用请求被错误拦截。
        if (status == 0 && Integer.valueOf(1).equals(station.getStatus())) {
            long referenceCount = trainStopMapper.countEnabledTrainReferencesByStationId(id);
            if (referenceCount > 0) {
                throw new BusinessException(400, "该车站仍被启用车次使用，请先调整线路或停用相关车次");
            }
        }

        station.setStatus(status);
        return updateWithOptimisticLock(station, "车站状态已被其他操作修改，请刷新后重试");
    }

    /** 根据主键获取车站；不存在时统一抛出业务异常。 */
    private Station getStationById(Long id) {
        if (id == null) {
            throw new BusinessException(400, "车站 ID 不能为空");
        }

        Station station = stationMapper.selectById(id);
        if (station == null) {
            throw new BusinessException(404, "车站不存在");
        }
        return station;
    }

    /** 执行携带 @Version 条件的更新；更新行数为 0 表示发生乐观锁冲突。 */
    private Station updateWithOptimisticLock(Station station, String conflictMessage) {
        int rows = stationMapper.updateById(station);
        if (rows == 0) {
            throw new BusinessException(409, conflictMessage);
        }
        return station;
    }
}
