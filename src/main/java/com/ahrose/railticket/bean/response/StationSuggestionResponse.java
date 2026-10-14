package com.ahrose.railticket.bean.response;

import com.ahrose.railticket.bean.Station;
import com.ahrose.railticket.common.enums.TrainSearchLocationType;

/** 车站搜索框的一个候选项；value 可直接作为查票接口对应地点的参数。 */
public record StationSuggestionResponse(
        TrainSearchLocationType type,
        String value,
        String label,
        String city,
        String stationCode
) {

    /** 创建代表整个城市的候选项。 */
    public static StationSuggestionResponse city(String city) {
        return new StationSuggestionResponse(
                TrainSearchLocationType.CITY,
                city,
                city + "（全部车站）",
                city,
                null
        );
    }

    /** 创建代表具体车站的候选项。 */
    public static StationSuggestionResponse station(Station station) {
        return new StationSuggestionResponse(
                TrainSearchLocationType.STATION,
                station.getCode(),
                station.getName(),
                station.getCity(),
                station.getCode()
        );
    }
}
