package com.ahrose.railticket.notification.mapper;

import com.ahrose.railticket.bean.UserNotification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 用户站内通知数据库访问接口。 */
@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {
}
