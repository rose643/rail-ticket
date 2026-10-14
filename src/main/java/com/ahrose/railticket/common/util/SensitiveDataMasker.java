package com.ahrose.railticket.common.util;

/** 对接口响应中的证件号和手机号进行脱敏。 */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
    }

    public static String maskIdNo(String idNo) {
        if (idNo == null || idNo.isBlank()) {
            return idNo;
        }
        if (idNo.length() <= 4) {
            return "*".repeat(idNo.length());
        }
        return idNo.substring(0, 2)
                + "*".repeat(idNo.length() - 4)
                + idNo.substring(idNo.length() - 2);
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        if (phone.length() < 7) {
            return "*".repeat(phone.length());
        }
        return phone.substring(0, 3)
                + "*".repeat(phone.length() - 7)
                + phone.substring(phone.length() - 4);
    }
}
