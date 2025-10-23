package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Slf4j
public class NotificationService {

    private static final long SSE_SESSION_TIMEOUT = 30 * 60 * 1000L; // 30 minutes
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 为指定用户创建一个新的 SseEmitter。
     * @param userId 用户ID
     * @return SseEmitter 实例
     */
    public SseEmitter createEmitter(String userId) {
        SseEmitter emitter = new SseEmitter(SSE_SESSION_TIMEOUT);
        this.emitters.put(userId, emitter);

        emitter.onCompletion(() -> {
            log.info("用户 '{}' 的SSE连接已完成", userId);
            this.emitters.remove(userId);
        });
        emitter.onTimeout(() -> {
            log.warn("用户 '{}' 的SSE连接超时", userId);
            emitter.complete();
            this.emitters.remove(userId);
        });
        emitter.onError(e -> {
            log.error("用户 '{}' 的SSE连接发生错误", userId, e);
            emitter.complete();
            this.emitters.remove(userId);
        });

        // 发送一个初始连接成功的事件
        sendNotification(userId, Constants.SseEvent.CONNECTION_ESTABLISHED, "连接成功");

        return emitter;
    }

    /**
     * 向指定用户或所有用户发送通知。
     * @param userId 用户ID，或使用 "all_users" 进行广播
     * @param eventName 事件名称
     * @param data 事件数据
     */
    public void sendNotification(String userId, String eventName, Object data) {
        if ("all_users".equals(userId)) {
            log.info("正在向所有在线用户广播事件 '{}'", eventName);
            emitters.forEach((id, emitter) -> sendToEmitter(emitter, id, eventName, data));
        } else {
            SseEmitter emitter = this.emitters.get(userId);
            if (emitter != null) {
                sendToEmitter(emitter, userId, eventName, data);
            } else {
                log.warn("无法找到用户 '{}' 的SSE Emitter，可能已离线", userId);
            }
        }
    }

    /**
     * 实际向单个 Emitter 发送事件的私有方法。
     * @param emitter SseEmitter 实例
     * @param userId  接收方的用户ID
     * @param eventName 事件名称
     * @param data 事件数据
     */
    private void sendToEmitter(SseEmitter emitter, String userId, String eventName, Object data) {
        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name(eventName)
                .data(data)
                .id(String.valueOf(System.currentTimeMillis()));
            emitter.send(event);
            log.info("已向用户 '{}' 发送事件 '{}'", userId, eventName);
        } catch (IOException e) {
            log.error("向用户 '{}' 发送事件 '{}' 失败", userId, eventName, e);
            emitter.complete();
            this.emitters.remove(userId);
        }
    }
}
