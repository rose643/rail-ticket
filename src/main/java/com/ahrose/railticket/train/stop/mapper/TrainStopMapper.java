package com.ahrose.railticket.train.stop.mapper;

import com.ahrose.railticket.bean.TrainStop;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * ClassName: TrainStopMapper
 * Package: com.ahrose.railticket.train.stop.mapper
 * Description:
 *
 * @Author 张浩然
 * @Create 2026/9/24 20:58
 * @Version 1.0
 */
@Mapper
public interface TrainStopMapper extends BaseMapper<TrainStop> {

    /** 统计仍引用指定车站的启用车次数量，用于停用车站前的业务校验。 */
    @Select("""
            SELECT COUNT(*)
            FROM train_stop ts
            INNER JOIN train t ON t.id = ts.train_id
            WHERE ts.station_id = #{stationId}
              AND t.status = 1
            """)
    long countEnabledTrainReferencesByStationId(@Param("stationId") Long stationId);

    /**
     * 为插入新经停站腾出位置。
     * <p>
     * 必须按 stop_index 降序更新，否则在联合唯一索引下可能出现临时重复序号。
     */
    @Update("""
            UPDATE train_stop
            SET stop_index = stop_index + 1
            WHERE train_id = #{trainId}
              AND stop_index >= #{stopIndex}
            ORDER BY stop_index DESC
            """)
    int shiftStopIndexesForInsert(@Param("trainId") Long trainId, @Param("stopIndex") Integer stopIndex);

    /**
     * 删除经停站后，将后续站点顺序前移一位。
     * <p>
     * 必须按 stop_index 升序更新；例如 2->1 后，3 才能安全地更新为 2。
     */
    @Update("""
            UPDATE train_stop
            SET stop_index = stop_index - 1
            WHERE train_id = #{trainId}
              AND stop_index > #{stopIndex}
            ORDER BY stop_index ASC
            """)
    int shiftStopIndexesForDelete(@Param("trainId") Long trainId, @Param("stopIndex") Integer stopIndex);

    /**
     * 将待移动的经停站暂时移出正常序号范围，为区间内其他站点调整顺序腾出位置。
     */
    @Update("""
            UPDATE train_stop
            SET stop_index = -1
            WHERE id = #{stopId}
              AND train_id = #{trainId}
            """)
    int markStopIndexTemporary(@Param("trainId") Long trainId, @Param("stopId") Long stopId);

    /**
     * 经停站向后移动时，将旧位置之后至新位置之间的站点依次前移一位。
     */
    @Update("""
            UPDATE train_stop
            SET stop_index = stop_index - 1
            WHERE train_id = #{trainId}
              AND stop_index > #{oldIndex}
              AND stop_index <= #{newIndex}
            ORDER BY stop_index ASC
            """)
    int shiftStopIndexesForward(@Param("trainId") Long trainId,
                                @Param("oldIndex") Integer oldIndex,
                                @Param("newIndex") Integer newIndex);

    /**
     * 经停站向前移动时，将新位置至旧位置之前的站点依次后移一位。
     */
    @Update("""
            UPDATE train_stop
            SET stop_index = stop_index + 1
            WHERE train_id = #{trainId}
              AND stop_index >= #{newIndex}
              AND stop_index < #{oldIndex}
            ORDER BY stop_index DESC
            """)
    int shiftStopIndexesBackward(@Param("trainId") Long trainId,
                                 @Param("newIndex") Integer newIndex,
                                 @Param("oldIndex") Integer oldIndex);
}
