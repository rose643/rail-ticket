package com.ahrose.railticket.order.mapper;

import com.ahrose.railticket.bean.TicketOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 车票订单数据库访问接口。 */
@Mapper
public interface TicketOrderMapper extends BaseMapper<TicketOrder> {

    /**
     * 实名认证成功后，将证件快照完全匹配且尚未关联账号的历史车票归属到该用户。
     *
     * @param userId 实际乘车人的系统用户 ID
     * @param realName 实名姓名
     * @param idType 证件类型
     * @param idNo 标准化后的证件号码
     * @return {@code int}，完成关联的历史订单数量
     */
    @Update("""
            UPDATE ticket_order
            SET passenger_user_id = #{userId}
            WHERE passenger_user_id IS NULL
              AND passenger_name = #{realName}
              AND passenger_id_type = #{idType}
              AND passenger_id_no = #{idNo}
            """)
    int bindHistoricalTicketsToUser(
            @Param("userId") Long userId,
            @Param("realName") String realName,
            @Param("idType") String idType,
            @Param("idNo") String idNo
    );

    /**
     * 查询当前用户作为实际乘车人的车票，并以实名快照兜底尚未完成账号关联的历史数据。
     *
     * @param userId 当前用户 ID
     * @param realName 有效实名认证姓名；没有有效实名信息时为 {@code null}
     * @param idType 有效实名认证证件类型；没有有效实名信息时为 {@code null}
     * @param idNo 有效实名认证证件号码；没有有效实名信息时为 {@code null}
     * @return {@code List<TicketOrder>}，本人车票列表
     */
    @Select("""
            SELECT *
            FROM ticket_order
            WHERE passenger_user_id = #{userId}
               OR (
                    passenger_user_id IS NULL
                AND #{realName} IS NOT NULL
                AND passenger_name = #{realName}
                AND passenger_id_type = #{idType}
                AND passenger_id_no = #{idNo}
               )
            ORDER BY created_at DESC
            """)
    List<TicketOrder> selectMyTickets(
            @Param("userId") Long userId,
            @Param("realName") String realName,
            @Param("idType") String idType,
            @Param("idNo") String idNo
    );

    /**
     * 查询购票人或实际乘车人有权查看的订单，实名快照用于兜底历史未关联车票。
     *
     * @param orderId 订单 ID
     * @param userId 当前用户 ID
     * @param realName 有效实名认证姓名；没有有效实名信息时为 {@code null}
     * @param idType 有效实名认证证件类型；没有有效实名信息时为 {@code null}
     * @param idNo 有效实名认证证件号码；没有有效实名信息时为 {@code null}
     * @return {@code TicketOrder}，有权查看的订单；不存在或无权限时返回 {@code null}
     */
    @Select("""
            SELECT *
            FROM ticket_order
            WHERE id = #{orderId}
              AND (
                   user_id = #{userId}
                OR passenger_user_id = #{userId}
                OR (
                       passenger_user_id IS NULL
                   AND #{realName} IS NOT NULL
                   AND passenger_name = #{realName}
                   AND passenger_id_type = #{idType}
                   AND passenger_id_no = #{idNo}
                )
              )
            """)
    TicketOrder selectVisibleOrder(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId,
            @Param("realName") String realName,
            @Param("idType") String idType,
            @Param("idNo") String idNo
    );

    /**
     * 按主键锁定订单，串行化支付、取消、退票和超时关单之间的状态竞争。
     *
     * @param orderId 订单雪花 ID
     * @return {@code TicketOrder}，锁定的订单；不存在时返回 {@code null}
     */
    @Select("""
            SELECT *
            FROM ticket_order
            WHERE id = #{orderId}
            FOR UPDATE
            """)
    TicketOrder selectByIdForUpdate(@Param("orderId") Long orderId);

    /**
     * 查询需要由补偿任务关闭的到期待支付订单。
     *
     * @param limit 单批最大查询数量
     * @return {@code List<Long>}，到期订单 ID 列表
     */
    @Select("""
            SELECT id
            FROM ticket_order
            WHERE status = 'PENDING_PAYMENT'
              AND expire_at <= NOW()
            ORDER BY expire_at ASC
            LIMIT #{limit}
            """)
    List<Long> selectExpiredPendingOrderIds(@Param("limit") int limit);
}
