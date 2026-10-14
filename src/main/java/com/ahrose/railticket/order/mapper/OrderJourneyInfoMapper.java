package com.ahrose.railticket.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 批量查询订单对应的完整行程展示信息。 */
@Mapper
public interface OrderJourneyInfoMapper {

    /** 根据订单 ID 批量关联运行计划、车次、经停站和车站，避免列表查询产生 N+1 SQL。 */
    @Select("""
            <script>
            SELECT o.id                                AS order_id,
                   t.id                                AS train_id,
                   t.train_no                          AS train_no,
                   t.name                              AS train_name,
                   tsc.travel_date                     AS travel_date,
                   from_station.code                   AS from_station_code,
                   from_station.name                   AS from_station_name,
                   from_stop.departure_offset_minutes  AS departure_offset_minutes,
                   to_station.code                     AS to_station_code,
                   to_station.name                     AS to_station_name,
                   to_stop.arrival_offset_minutes      AS arrival_offset_minutes
            FROM ticket_order o
            INNER JOIN train_schedule tsc ON tsc.id = o.schedule_id
            INNER JOIN train t ON t.id = tsc.train_id
            INNER JOIN train_stop from_stop
                    ON from_stop.train_id = t.id
                   AND from_stop.stop_index = o.from_stop_index
            INNER JOIN station from_station ON from_station.id = from_stop.station_id
            INNER JOIN train_stop to_stop
                    ON to_stop.train_id = t.id
                   AND to_stop.stop_index = o.to_stop_index
            INNER JOIN station to_station ON to_station.id = to_stop.station_id
            WHERE o.id IN
            <foreach collection="orderIds" item="orderId" open="(" separator="," close=")">
                #{orderId}
            </foreach>
            </script>
            """)
    List<OrderJourneyInfo> selectByOrderIds(@Param("orderIds") List<Long> orderIds);
}
