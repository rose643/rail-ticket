package com.ahrose.railticket.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 某车次在指定日期的运行记录。 */
@Data
@TableName("train_schedule")
public class TrainSchedule {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long trainId;
    private LocalDate travelDate;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
