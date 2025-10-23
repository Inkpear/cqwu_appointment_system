package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.service.NotificationService;
import java.time.Duration;
import java.time.Instant;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @description SSE 通知服务实现。负责连接生命周期管理、按用户/角色推送以及广播。
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    // 使用构造器注入的实例字段而不是 static，避免 @Value 对静态字段不生效
    private final long sseSessionTimeoutMs; // default 30 minutes
    private final long heartbeatBaseMs;     // base/min interval for heartbeat
    private final double heartbeatMultiplier; // P 倍率
    private final long heartbeatMaxMs;       // 上限间隔，避免客户端误判掉线
    private final TaskScheduler taskScheduler;
    private final AtomicLong lastHeartbeatCostMs = new AtomicLong(0L);
    private volatile ScheduledFuture<?> heartbeatFuture;
    private final boolean broadcastJitterEnabled; // 广播时对不同用户添加抖动
    private final long broadcastJitterMaxMs;       // 广播抖动上限
    private final boolean broadcastBatchEnabled;   // 广播分批开关
    private final int broadcastBatchSize;          // 每批大小
    private final long broadcastBatchGapMs;        // 批间隔
    private final long broadcastBatchWithinJitterMs; // 批内抖动上限

    public NotificationServiceImpl(
            @Value("${logging.sse.life:PT30M}") Duration sseSessionTimeout,
            @Value("${logging.sse.heartbeat.base:PT25S}") Duration heartbeatBase,
            @Value("${logging.sse.heartbeat.multiplier:1.0}") double heartbeatMultiplier,
            @Value("${logging.sse.heartbeat.max:PT45S}") Duration heartbeatMax,
            @Value("${logging.sse.broadcast.jitter.enabled:true}") boolean broadcastJitterEnabled,
            @Value("${logging.sse.broadcast.jitter.max:PT3S}") Duration broadcastJitterMax,
            @Value("${logging.sse.broadcast.batch.enabled:true}") boolean broadcastBatchEnabled,
            @Value("${logging.sse.broadcast.batch.size:50}") int broadcastBatchSize,
            @Value("${logging.sse.broadcast.batch.gap:300ms}") Duration broadcastBatchGap,
            @Value("${logging.sse.broadcast.batch.within-jitter:200ms}") Duration broadcastBatchWithinJitter,
            TaskScheduler taskScheduler) {
        this.sseSessionTimeoutMs = sseSessionTimeout.toMillis();
        this.heartbeatBaseMs = Math.max(0L, heartbeatBase.toMillis());
        this.heartbeatMultiplier = heartbeatMultiplier > 0 ? heartbeatMultiplier : 1.0d;
        long maxMs = heartbeatMax != null ? heartbeatMax.toMillis() : 0L;
        this.heartbeatMaxMs = maxMs > 0 ? Math.max(maxMs, this.heartbeatBaseMs) : 0L; // 保证 max >= base
        this.broadcastJitterEnabled = broadcastJitterEnabled;
        long jitterMax = broadcastJitterMax != null ? broadcastJitterMax.toMillis() : 0L;
        this.broadcastJitterMaxMs = Math.max(0L, jitterMax);
        this.broadcastBatchEnabled = broadcastBatchEnabled;
        this.broadcastBatchSize = Math.max(1, broadcastBatchSize);
        this.broadcastBatchGapMs = Math.max(0L, (broadcastBatchGap != null ? broadcastBatchGap.toMillis() : 0L));
        this.broadcastBatchWithinJitterMs = Math.max(0L, (broadcastBatchWithinJitter != null ? broadcastBatchWithinJitter.toMillis() : 0L));
        this.taskScheduler = taskScheduler;
    }

    // 支持同一用户多端/多标签页并发订阅，并存储用户角色以支持按角色推送
    private final Map<String, CopyOnWriteArrayList<EmitterEntry>> emitters = new ConcurrentHashMap<>();

    private static final class EmitterEntry {
        final SseEmitter emitter;
        final String role; // 用户角色（ADMIN/ROOT/USER/MONITOR）

        EmitterEntry(SseEmitter emitter, String role) {
            this.emitter = emitter;
            this.role = role;
        }
    }

    /** {@inheritDoc} */
    @Override
    public SseEmitter createEmitter(String userId, String role) {
        SseEmitter emitter = new SseEmitter(sseSessionTimeoutMs);
        // 多连接并存：为该用户维护一个连接列表
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(new EmitterEntry(emitter, role));

        emitter.onCompletion(() -> {
            log.info("用户 '{}' 的SSE连接已完成", userId);
            removeEmitter(userId, emitter);
        });
        emitter.onTimeout(() -> {
            log.warn("用户 '{}' 的SSE连接超时", userId);
            emitter.complete();
            removeEmitter(userId, emitter);
        });
        emitter.onError(e -> {
            log.error("用户 '{}' 的SSE连接发生错误", userId, e);
            emitter.complete();
            removeEmitter(userId, emitter);
        });

        // 发送一个初始连接成功的事件
        sendNotification(userId, Constants.SseEvent.CONNECTION_ESTABLISHED, "连接成功");

        return emitter;
    }

    /** {@inheritDoc} */
    @Override
    public void sendNotification(String userId, String eventName, Object data) {
        if (Constants.SseBroadcast.ALL_USERS.equals(userId)) {
            log.info("正在向所有在线用户广播事件 '{}'", eventName);
            // 快照并扁平化所有目标连接
            List<Target> targets = new ArrayList<>();
            emitters.forEach((id, list) -> {
                if (list == null || list.isEmpty()) return;
                list.forEach(entry -> targets.add(new Target(id, entry)));
            });

            if (targets.isEmpty()) return;

            final long now = System.currentTimeMillis();
            if (broadcastBatchEnabled) {
                // 分批 + 批内轻微抖动，先随机打散以避免同一批次长期固定同一批用户
                Collections.shuffle(targets);
                for (int i = 0; i < targets.size(); i++) {
                    int batchIndex = i / broadcastBatchSize;
                    long baseDelay = batchIndex * broadcastBatchGapMs;
                    long jitter = broadcastBatchWithinJitterMs > 0
                            ? ThreadLocalRandom.current().nextLong(0L, broadcastBatchWithinJitterMs + 1L)
                            : 0L;
                    long fireAt = now + baseDelay + jitter;
                    Target t = targets.get(i);
                    taskScheduler.schedule(() -> sendToEmitter(t.entry.emitter, t.userId, eventName, data),
                            Instant.ofEpochMilli(fireAt));
                }
            } else {
                // 仅抖动：每个目标在 [0, broadcastJitterMaxMs] 随机延迟内发送
                targets.forEach(t -> {
                    if (broadcastJitterEnabled && broadcastJitterMaxMs > 0) {
                        long delay = ThreadLocalRandom.current().nextLong(0L, broadcastJitterMaxMs + 1L);
                        taskScheduler.schedule(() -> sendToEmitter(t.entry.emitter, t.userId, eventName, data),
                                Instant.ofEpochMilli(now + delay));
                    } else {
                        sendToEmitter(t.entry.emitter, t.userId, eventName, data);
                    }
                });
            }
        } else {
            List<EmitterEntry> list = this.emitters.get(userId);
            sendToRole(Constants.UserRoleType.ROLE_MONITOR, eventName, data);
            if (list != null && !list.isEmpty()) {
                list.forEach(entry -> sendToEmitter(entry.emitter, userId, eventName, data));
            } else {
                log.warn("无法找到用户 '{}' 的SSE连接，可能已离线", userId);
            }
        }
    }

    private static final class Target {
        final String userId;
        final EmitterEntry entry;

        Target(String userId, EmitterEntry entry) {
            this.userId = userId;
            this.entry = entry;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void sendToRole(String role, String eventName, Object data) {
        if (role == null)
            return;
        emitters.forEach((userId, list) -> {
            if (list == null || list.isEmpty())
                return;
            list.forEach(entry -> {
                if (role.equalsIgnoreCase(entry.role) || entry.role.equalsIgnoreCase(Constants.UserRoleType.ROLE_MONITOR)) {
                    sendToEmitter(entry.emitter, userId, eventName, data);
                }
            });
        });
    }

    /** {@inheritDoc} */
    @Override
    public void sendToRoles(List<String> roles, String eventName, Object data) {
        if (roles == null)
            return;
        roles.add(new String(Constants.UserRoleType.ROLE_MONITOR));

        emitters.forEach((userId, list) -> {
            if (list == null || list.isEmpty())
                return;
            list.forEach(entry -> {
                if (roles.contains(entry.role)) {
                    sendToEmitter(entry.emitter, userId, eventName, data);
                }
            });
        });
    }

    /**
     * 实际向单个 Emitter 发送事件的私有方法。
     * 
     * @param emitter   SseEmitter 实例
     * @param userId    接收方的用户ID
     * @param eventName 事件名称
     * @param data      事件数据
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
            removeEmitter(userId, emitter);
        }
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        List<EmitterEntry> list = this.emitters.get(userId);
        if (list != null) {
            list.removeIf(entry -> Objects.equals(entry.emitter, emitter));
            if (list.isEmpty()) {
                this.emitters.remove(userId);
            }
        }
    }

    /**
     * 自适应心跳：下一次间隔 = max(base, 上次执行耗时 * P)。
     * 使用 TaskScheduler + Trigger 来动态计算调度间隔。
     */
    @PostConstruct
    public void startAdaptiveHeartbeat() {
        heartbeatFuture = taskScheduler.schedule(this::doHeartbeatOnce, triggerContext -> {
            Instant lastCompletion = triggerContext.lastCompletion();
            long cost = lastHeartbeatCostMs.get();
            long computedDelay = Math.max(heartbeatBaseMs, (long) Math.ceil(cost * heartbeatMultiplier));
            if (heartbeatMaxMs > 0) {
                computedDelay = Math.min(heartbeatMaxMs, computedDelay);
            }
            long nextTime = (lastCompletion != null ? lastCompletion.toEpochMilli() : System.currentTimeMillis()) + computedDelay;
            return Instant.ofEpochMilli(nextTime);
        });
        log.info("SSE 心跳调度已启动：base={}ms, P={}, max={}ms", heartbeatBaseMs, heartbeatMultiplier, heartbeatMaxMs);
    }

    @PreDestroy
    public void stopAdaptiveHeartbeat() {
        ScheduledFuture<?> future = heartbeatFuture;
        if (future != null) {
            future.cancel(true);
        }
    }

    private void doHeartbeatOnce() {
        long startNs = System.nanoTime();
        try {
            if (!emitters.isEmpty()) {
                sendNotification(Constants.SseBroadcast.ALL_USERS, Constants.SseEvent.PING, System.currentTimeMillis());
            }
        } finally {
            long costMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
            lastHeartbeatCostMs.set(costMs);
            if (costMs > 0) {
                log.debug("SSE 心跳耗时：{} ms（用于计算下一次调度间隔）", costMs);
            }
        }
    }
}
