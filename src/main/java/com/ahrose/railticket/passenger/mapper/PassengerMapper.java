package com.ahrose.railticket.passenger.mapper;

import com.ahrose.railticket.bean.Passenger;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 常用乘车人数据库访问接口。 */
@Mapper
public interface PassengerMapper extends BaseMapper<Passenger> {
}
