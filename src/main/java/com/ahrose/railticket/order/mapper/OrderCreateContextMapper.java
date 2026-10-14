package com.ahrose.railticket.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 创建订单所需运行计划、车次、区间和票价的联表查询。 */
@Mapper
public interface OrderCreateContextMapper {

    /**
     * 一次查询取得服务端可信的订单上下文，前端不传价格、车次状态和发车时间。
     * 使用左连接保留运行计划，使服务层能够区分路线、车站或票价配置缺失；
     * 共享锁会保持到订单事务结束，防止校验过程中票价或运行状态被并发修改。
     *
     * @param scheduleId 运行计划雪花 ID
     * @param seatTypeCode 席别编码
     * @param fromStopIndex 出发站顺序
     * @param toStopIndex 到达站顺序
     * @return {@code OrderCreateContext}，订单校验上下文；运行计划不存在时返回 {@code null}
     */
    @Select("""
            SELECT tsc.id                              AS schedule_id,
                   tsc.train_id                        AS train_id,
                   tsc.travel_date                     AS travel_date,
                   tsc.status                          AS schedule_status,
                   t.status                            AS train_status,
                   from_stop.id                        AS from_stop_id,
                   to_stop.id                          AS to_stop_id,
                   from_station.status                 AS from_station_status,
                   to_station.status                   AS to_station_status,
                   from_stop.departure_offset_minutes  AS departure_offset_minutes,
                   fare.id                             AS fare_id,
                   fare.price                          AS price
            FROM train_schedule tsc
            INNER JOIN train t
                    ON t.id = tsc.train_id
            LEFT JOIN train_stop from_stop
                   ON from_stop.train_id = t.id
                  AND from_stop.stop_index = #{fromStopIndex}
            LEFT JOIN station from_station
                   ON from_station.id = from_stop.station_id
            LEFT JOIN train_stop to_stop
                   ON to_stop.train_id = t.id
                  AND to_stop.stop_index = #{toStopIndex}
            LEFT JOIN station to_station
                   ON to_station.id = to_stop.station_id
            LEFT JOIN train_fare fare
                   ON fare.train_id = t.id
                  AND fare.seat_type_code = #{seatTypeCode}
                  AND fare.from_stop_index = #{fromStopIndex}
                  AND fare.to_stop_index = #{toStopIndex}
            WHERE tsc.id = #{scheduleId}
            FOR SHARE
            """)
    OrderCreateContext selectOrderCreateContext(
            @Param("scheduleId") Long scheduleId,
            @Param("seatTypeCode") String seatTypeCode,
            @Param("fromStopIndex") Integer fromStopIndex,
            @Param("toStopIndex") Integer toStopIndex
    );
}
