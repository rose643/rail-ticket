package com.ahrose.railticket.common.enums;

import com.ahrose.railticket.common.exception.BusinessException;

import java.util.Locale;

/** 查票地点类型：可以按城市范围查询，也可以指定一个具体车站。 */
public enum TrainSearchLocationType {

    /** 城市下所有启用车站。 */
    CITY,

    /** 指定的一个启用车站。 */
    STATION;

    /** 将接口传入的字符串转换为地点类型，并提供统一的业务错误提示。 */
    public static TrainSearchLocationType from(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, fieldName + "不能为空");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, fieldName + "只能为 CITY 或 STATION");
        }
    }
}
