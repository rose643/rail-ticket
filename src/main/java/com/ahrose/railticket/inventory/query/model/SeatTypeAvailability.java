package com.ahrose.railticket.inventory.query.model;

import lombok.Data;

/** MySQL 按席别分组计算出的区间余票。 */
@Data
public class SeatTypeAvailability {

    private String seatTypeCode;
    private Integer available;
}
