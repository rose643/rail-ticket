package com.ahrose.railticket.passenger.service;

import com.ahrose.railticket.bean.Passenger;
import com.ahrose.railticket.bean.request.PassengerCreateRequest;
import com.ahrose.railticket.bean.request.PassengerPhoneUpdateRequest;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/** 当前登录用户的常用乘车人业务接口。 */
public interface PassengerService extends IService<Passenger> {

    /**
     * 查询当前用户的全部常用乘车人。
     *
     * @param userId 当前登录用户雪花 ID
     * @return {@code List<Passenger>}，当前用户的常用乘车人列表
     */
    List<Passenger> listPassengers(Long userId);

    /**
     * 查询当前用户的指定常用乘车人。
     *
     * @param userId 当前登录用户雪花 ID
     * @param passengerId 乘车人雪花 ID
     * @return {@code Passenger}，属于当前用户的乘车人；不存在时抛出业务异常
     */
    Passenger getPassenger(Long userId, Long passengerId);

    /**
     * 为当前用户新增常用乘车人。
     *
     * @param userId 当前登录用户雪花 ID
     * @param request 已通过 Controller 参数校验的新增请求
     * @return {@code Passenger}，保存成功后的完整乘车人信息
     */
    Passenger createPassenger(Long userId, PassengerCreateRequest request);

    /**
     * 修改当前用户指定常用乘车人的联系电话。
     * <p>姓名、证件类型和证件号码属于身份信息，创建后不允许修改。</p>
     *
     * @param userId 当前登录用户雪花 ID
     * @param passengerId 乘车人雪花 ID
     * @param request 已通过 Controller 参数校验的手机号修改请求
     * @return {@code Passenger}，手机号修改后的完整乘车人信息
     */
    Passenger updatePassengerPhone(
            Long userId,
            Long passengerId,
            PassengerPhoneUpdateRequest request
    );

    /**
     * 删除常用乘车人；历史订单依靠下单快照保存乘车人信息，因此不受影响。
     *
     * @param userId 当前登录用户雪花 ID
     * @param passengerId 乘车人雪花 ID
     */
    void deletePassenger(Long userId, Long passengerId);
}
