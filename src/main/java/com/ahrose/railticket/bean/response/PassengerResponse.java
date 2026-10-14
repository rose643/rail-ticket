package com.ahrose.railticket.bean.response;

import com.ahrose.railticket.bean.Passenger;
import com.ahrose.railticket.common.util.SensitiveDataMasker;

import java.time.LocalDateTime;

/** 常用乘车人响应，证件号码和手机号经过脱敏。 */
public record PassengerResponse(
        Long id,
        String name,
        String idType,
        String idNo,
        String phone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PassengerResponse from(Passenger passenger) {
        return new PassengerResponse(
                passenger.getId(),
                passenger.getName(),
                passenger.getIdType(),
                SensitiveDataMasker.maskIdNo(passenger.getIdNo()),
                SensitiveDataMasker.maskPhone(passenger.getPhone()),
                passenger.getCreatedAt(),
                passenger.getUpdatedAt()
        );
    }

}
