package com.ahrose.railticket.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户实名认证信息。 */
@Data
@TableName("user_identity")
public class UserIdentity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String realName;
    private String idType;
    private String idNo;
    private Integer status;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
