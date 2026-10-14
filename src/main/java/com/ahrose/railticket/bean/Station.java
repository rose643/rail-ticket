package com.ahrose.railticket.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/** 车站基础信息。 */
@Data
@TableName("station")
public class Station {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String code;
    private String name;
    private String city;
    private Integer status;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
