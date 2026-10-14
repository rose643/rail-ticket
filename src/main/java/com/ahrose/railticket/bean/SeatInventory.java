package com.ahrose.railticket.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 运行日、席别、区段维度的可售库存。 */
@Data
@TableName("seat_inventory")
public class SeatInventory {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long scheduleId;
    private String seatTypeCode;
    private Integer segmentIndex;
    private Integer total;
    private Integer available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
