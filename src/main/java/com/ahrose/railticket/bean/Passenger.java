package com.ahrose.railticket.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户的常用乘车人。 */
@Data
@TableName("passenger")
public class Passenger {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String name;
    private String idType;
    private String idNo;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
