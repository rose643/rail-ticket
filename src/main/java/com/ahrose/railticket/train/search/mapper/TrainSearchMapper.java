package com.ahrose.railticket.train.search.mapper;

import com.ahrose.railticket.train.search.model.TrainSearchCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/** 用户查票联表查询 Mapper。 */
@Mapper
public interface TrainSearchMapper {

    /**
     * 查询同时经过出发站和到达站，并在指定日期处于可售状态的候选车次。
     *
     * @param fromType  出发地点类型，CITY 或 STATION
     * @param fromValue 出发城市名称或车站编码
     * @param toType    到达地点类型，CITY 或 STATION
     * @param toValue   到达城市名称或车站编码
     * @param travelDate      车次始发日期
     * @return {@code List<TrainSearchCandidate>}，按照发车偏移时间和车次号排序的候选车次
     */
    @Select("""
            <script>
            SELECT tsc.id                          AS schedule_id,
                   t.id                            AS train_id,
                   t.train_no                      AS train_no,
                   t.name                          AS train_name,
                   tsc.travel_date                 AS travel_date,
                   from_station.code               AS from_station_code,
                   from_station.name               AS from_station_name,
                   from_stop.stop_index            AS from_stop_index,
                   from_stop.departure_offset_minutes AS departure_offset_minutes,
                   to_station.code                 AS to_station_code,
                   to_station.name                 AS to_station_name,
                   to_stop.stop_index              AS to_stop_index,
                   to_stop.arrival_offset_minutes  AS arrival_offset_minutes
            FROM train_schedule tsc
            INNER JOIN train t
                    ON t.id = tsc.train_id
                   AND t.status = 1
            INNER JOIN train_stop from_stop
                    ON from_stop.train_id = t.id
            INNER JOIN station from_station
                    ON from_station.id = from_stop.station_id
                   AND from_station.status = 1
                   <choose>
                       <when test="fromType == 'CITY'">
                           AND from_station.city = #{fromValue}
                       </when>
                       <otherwise>
                           AND from_station.code = #{fromValue}
                       </otherwise>
                   </choose>
            INNER JOIN train_stop to_stop
                    ON to_stop.train_id = t.id
                   AND to_stop.stop_index > from_stop.stop_index
            INNER JOIN station to_station
                    ON to_station.id = to_stop.station_id
                   AND to_station.status = 1
                   <choose>
                       <when test="toType == 'CITY'">
                           AND to_station.city = #{toValue}
                       </when>
                       <otherwise>
                           AND to_station.code = #{toValue}
                       </otherwise>
                   </choose>
            WHERE tsc.travel_date = #{travelDate}
              AND tsc.status = 1
            ORDER BY from_stop.departure_offset_minutes ASC,
                     t.train_no ASC
            </script>
            """)
    List<TrainSearchCandidate> selectSearchCandidates(
            @Param("fromType") String fromType,
            @Param("fromValue") String fromValue,
            @Param("toType") String toType,
            @Param("toValue") String toValue,
            @Param("travelDate") LocalDate travelDate
    );
}
