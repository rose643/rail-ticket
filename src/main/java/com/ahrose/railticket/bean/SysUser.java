package com.ahrose.railticket.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 系统用户。 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private String passwordHash;
    private String phone;
    private String role;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
