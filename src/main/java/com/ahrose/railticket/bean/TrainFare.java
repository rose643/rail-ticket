package com.ahrose.railticket.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 车次在指定区间、指定席别的票价。 */
@Data
@TableName("train_fare")
public class TrainFare {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long trainId;
    private String seatTypeCode;
    private Integer fromStopIndex;
    private Integer toStopIndex;
    private BigDecimal price;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
