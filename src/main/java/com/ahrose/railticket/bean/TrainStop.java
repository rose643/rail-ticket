package com.ahrose.railticket.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 车次经停站及相对始发时刻。 */
@Data
@TableName("train_stop")
public class TrainStop {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long trainId;
    private Long stationId;
    private Integer stopIndex;
    private Integer arrivalOffsetMinutes;
    private Integer departureOffsetMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
