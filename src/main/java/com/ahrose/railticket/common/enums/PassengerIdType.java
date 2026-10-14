package com.ahrose.railticket.common.enums;

import java.util.Arrays;

/** 常用乘车人支持的证件类型。 */
public enum PassengerIdType {
    ID_CARD,
    PASSPORT,
    HK_MACAO_PERMIT,
    TAIWAN_PERMIT;

    /** 判断给定代码是否属于支持的证件类型。 */
    public static boolean contains(String code) {
        return code != null && Arrays.stream(values())
                .anyMatch(type -> type.name().equals(code));
    }
}
