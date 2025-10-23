package com.cqwu.cqwu_appointment_system.service;

import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @description SSE 通知服务接口，负责管理订阅与事件推送。
 *              控制器与业务组件依赖该接口，具体实现由 Spring 注入。
 */
public interface NotificationService {

    /**
     * 为指定用户创建一个新的 SSE 订阅连接。
     *
     * @param userId 用户ID
     * @param role   用户角色（如 ADMIN/ROOT/USER），用于按角色推送
     * @return SseEmitter 实例
     */
    SseEmitter createEmitter(String userId, String role);

    /**
     * 向指定用户或使用常量 ALL_USERS 进行广播。
     *
     * @param userId    接收方用户ID或 ALL_USERS
     * @param eventName 事件名称
     * @param data      事件数据
     */
    void sendNotification(String userId, String eventName, Object data);

    /**
     * 按单一角色推送事件。
     *
     * @param role      用户角色（大小写不敏感）
     * @param eventName 事件名称
     * @param data      事件数据
     */
    void sendToRole(String role, String eventName, Object data);

    /**
     * 按多个角色推送事件。
     *
     * @param roles     角色列表（大小写不敏感）
     * @param eventName 事件名称
     * @param data      事件数据
     */
    void sendToRoles(List<String> roles, String eventName, Object data);
}
 
