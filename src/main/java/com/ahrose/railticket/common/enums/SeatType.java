package com.ahrose.railticket.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 固定席别字典。
 * <p>
 * 数据库使用 code 字段保存席别，例如 SECOND_CLASS。
 */
@Getter
@RequiredArgsConstructor
public enum SeatType {

    SECOND_CLASS("SECOND_CLASS", "二等座"),
    FIRST_CLASS("FIRST_CLASS", "一等座"),
    BUSINESS_CLASS("BUSINESS_CLASS", "商务座");

    private final String code;
    private final String name;

    /** 判断指定编码是否属于系统支持的席别。 */
    public static boolean contains(String code) {
        if (code == null) {
            return false;
        }

        for (SeatType seatType : values()) {
            if (seatType.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
