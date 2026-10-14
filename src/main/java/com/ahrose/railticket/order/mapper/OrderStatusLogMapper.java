package com.ahrose.railticket.order.mapper;

import com.ahrose.railticket.bean.OrderStatusLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 订单状态流转日志数据库访问接口。 */
@Mapper
public interface OrderStatusLogMapper extends BaseMapper<OrderStatusLog> {
}
