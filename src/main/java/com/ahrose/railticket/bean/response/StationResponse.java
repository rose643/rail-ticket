package com.ahrose.railticket.bean.response;

import com.ahrose.railticket.bean.Station;

/** 对外返回的车站信息，不暴露实体中的创建和更新时间。 */
public record StationResponse(Long id, String code, String name, String city, Integer status) {

    public static StationResponse from(Station station) {
        return new StationResponse(
                station.getId(),
                station.getCode(),
                station.getName(),
                station.getCity(),
                station.getStatus()
        );
    }
}
