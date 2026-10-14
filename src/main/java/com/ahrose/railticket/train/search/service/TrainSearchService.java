package com.ahrose.railticket.train.search.service;

import com.ahrose.railticket.bean.response.TrainSearchResponse;
import com.ahrose.railticket.common.enums.TrainSearchLocationType;

import java.time.LocalDate;
import java.util.List;

/** 面向普通用户的车票查询业务接口。 */
public interface TrainSearchService {

    /**
     * 按出发地点、到达地点和始发日期查询可售车次。地点既可以是城市，也可以是具体车站。
     *
     * @param fromType   出发地点类型，CITY 或 STATION
     * @param fromValue  出发城市名称或车站编码
     * @param toType     到达地点类型，CITY 或 STATION
     * @param toValue    到达城市名称或车站编码
     * @param travelDate 车次的始发日期
     * @return {@code List<TrainSearchResponse>}，包含发到时间、历时、各席别票价和余票的车次列表
     */
    List<TrainSearchResponse> searchTrains(
            TrainSearchLocationType fromType,
            String fromValue,
            TrainSearchLocationType toType,
            String toValue,
            LocalDate travelDate
    );

    /**
     * 兼容原有的按两个车站编码查票的服务调用。
     *
     * @return {@code List<TrainSearchResponse>}，具体车站之间的可售车次列表
     */
    default List<TrainSearchResponse> searchTrains(
            String fromStationCode,
            String toStationCode,
            LocalDate travelDate
    ) {
        return searchTrains(
                TrainSearchLocationType.STATION,
                fromStationCode,
                TrainSearchLocationType.STATION,
                toStationCode,
                travelDate
        );
    }
}
