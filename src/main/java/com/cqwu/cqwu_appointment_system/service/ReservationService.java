package com.cqwu.cqwu_appointment_system.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.cqwu.cqwu_appointment_system.domain.dto.UpdateReservationStatusRequest;
import com.cqwu.cqwu_appointment_system.domain.query.ReservationQuery;
import com.cqwu.cqwu_appointment_system.entity.ReservationWithBLOBs;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.entity.Venue;

import jakarta.persistence.EntityNotFoundException;

/**
 * @description 预约服务接口，定义了所有与预约申请相关的业务操作。
 */
public interface ReservationService {

    /**
     * 创建一个新的预约申请。
     * 实现应确保新创建的预约状态为 PENDING，并执行所有必要的业务校验。
     *
     * @param reservation 包含预约核心信息的实体，如时间、场地ID、事由等。
     * @param currentUser 发起预约的当前用户实体。
     * @param venue       将要被预约的场地实体。
     * @return 持久化后，包含完整信息（如ID、创建时间）的预约实体。
     */
    ReservationWithBLOBs createReservation(ReservationWithBLOBs reservation, User currentUser, Venue venue);

    /**
     * 取消一个已存在的预约申请。
     * 用户可取消自己的预约，管理员有权取消任何预约。
     *
     * @param request 包含要取消的预约ID以及操作者信息的数据传输对象。
     * @return 更新为 CANCELED 状态后的预约实体。
     */
    ReservationWithBLOBs cancelReservation(UpdateReservationStatusRequest request);

    /**
     * [管理员权限] 批准一个待审批的预约申请。
     * 这是核心的“竞争”操作，实现时必须进行最终的时间冲突检查并创建 TimeBlock。
     *
     * @param request 包含要批准的预约ID、批准意见以及操作者（管理员）信息的数据传输对象。
     * @return 更新为 CONFIRMED 状态后的预约实体。
     */
    ReservationWithBLOBs approveReservation(UpdateReservationStatusRequest request);

    /**
     * [管理员权限] 驳回一个待审批的预约申请。
     *
     * @param request 包含要驳回的预约ID、驳回理由以及操作者（管理员）信息的数据传输对象。
     * @return 更新为 REJECTED 状态后的预约实体。
     */
    ReservationWithBLOBs rejectReservation(UpdateReservationStatusRequest request);

    /**
     * 根据预约的唯一ID获取完整的预约实体信息。
     *
     * @param reservationId 预约的主键ID。
     * @return 对应的预约实体，若不存在则返回 null。
     */
    ReservationWithBLOBs findReservationById(Long reservationId);

    /**
     * 根据唯一的预约码获取完整的预约实体信息。
     *
     * @param code 预约成功后生成的唯一凭证码。
     * @return 对应的预约实体，若不存在则返回 null。
     */
    ReservationWithBLOBs findReservationByCode(String code);

    /**
     * 根据动态查询条件分页查询预约列表。
     *
     * @param query 封装了过滤条件（如用户ID、状态、场地ID）和分页参数的查询对象。
     * @return 封装了查询结果和分页信息的 Spring Data Page 对象。
     */
    Page<ReservationWithBLOBs> searchReservations(ReservationQuery query);

    /**
     * 根据预约ID列表批量查询预约信息。
     *
     * @param ids 包含一个或多个预约ID的列表。
     * @return 包含查询到的预约实体的列表，顺序可能与输入ID不一致。
     */
    List<ReservationWithBLOBs> findReservationsByIds(List<Long> ids);

    /**
     * [系统内部调用] 由系统自动批准一个预约申请。
     *
     * @param reservation 待批准的预约实体。
     * @return 更新为 CONFIRMED 状态后的预约实体。
     */
    ReservationWithBLOBs approveBySystem(ReservationWithBLOBs reservation);

    /**
     * [系统内部调用] 由系统自动拒绝一个预约申请。
     *
     * @param reservation 待拒绝的预约实体。
     * @param reason      拒绝原因。
     * @return 更新为 REJECTED 状态后的预约实体。
     */
    ReservationWithBLOBs rejectBySystem(ReservationWithBLOBs reservation, String reason);

    /**
     * 为指定的预约生成核验二维码。
     * 内部会处理权限校验和缓存。
     *
     * @param reservationId 要生成二维码的预约ID
     * @param currentUser   当前操作的用户
     * @return 二维码的 Base64 Data URI 字符串
     * @throws AccessDeniedException   如果用户无权访问该预约
     * @throws EntityNotFoundException 如果预约不存在
     */
    String generateQrCodeForReservation(Long reservationId, User currentUser);
}