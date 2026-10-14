package com.ahrose.railticket.train.search.model;

import lombok.Data;

import java.time.LocalDate;

/** 查票联表查询得到的候选车次，不对应单独的数据库表。 */
@Data
public class TrainSearchCandidate {

    private Long scheduleId;
    private Long trainId;
    private String trainNo;
    private String trainName;
    private LocalDate travelDate;
    private String fromStationCode;
    private String fromStationName;
    private Integer fromStopIndex;
    private Integer departureOffsetMinutes;
    private String toStationCode;
    private String toStationName;
    private Integer toStopIndex;
    private Integer arrivalOffsetMinutes;
}
