package com.ahrose.railticket.bean.response;

import com.ahrose.railticket.bean.Train;

/** 对外返回的车次基础信息。 */
public record TrainResponse(Long id, String trainNo, String name, Integer status) {

    public static TrainResponse from(Train train) {
        return new TrainResponse(train.getId(), train.getTrainNo(), train.getName(), train.getStatus());
    }
}
