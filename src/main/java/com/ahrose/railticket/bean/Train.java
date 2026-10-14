package com.ahrose.railticket.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 车次基础信息。 */
@Data
@TableName("train")
public class Train {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String trainNo;
    private String name;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
