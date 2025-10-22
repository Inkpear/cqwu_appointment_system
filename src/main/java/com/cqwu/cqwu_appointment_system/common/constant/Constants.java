package com.cqwu.cqwu_appointment_system.common.constant;

import java.util.Locale;

/**
 * @description 通用常量信息，定义了整个应用中广泛使用的静态常量。
 */
public class Constants {
    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";

    /**
     * 系统默认语言环境
     */
    public static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    /**
     * HTTP协议头
     */
    public static final String HTTP = "http://";

    /**
     * HTTPS协议头
     */
    public static final String HTTPS = "https://";

    // ===================================================================
    // API 响应消息
    // ===================================================================

    public static final String QUERY_SUCCESS = "查询成功";
    public static final String QUERY_NOT_FOUND = "未查询到数据";
    public static final String QUERY_FAIL = "查询失败";
    public static final String CREATE_SUCCESS = "创建成功";
    public static final String CREATE_FAIL = "创建失败";
    public static final String UPDATE_SUCCESS = "更新成功";
    public static final String DATA_NOT_CHANGED = "数据未发生变化";
    public static final String DELETE_SUCCESS = "删除成功";
    public static final String DELETE_FAIL = "删除失败";
    public static final String BATCH_CREATE_SUCCESS = "批量创建成功";
    public static final String BATCH_CREATE_FAIL = "批量创建失败，可能由于数据重复或内容无效";
    public static final String BATCH_UPDATE_FAIL_NOT_FOUND = "批量更新失败，未找到任何匹配的记录";

    // ===================================================================
    // 认证与授权消息
    // ===================================================================

    public static final String LOGIN_SUCCESS = "登录成功";
    public static final String LOGIN_FAIL = "用户名或密码错误";
    public static final String LOGOUT_SUCCESS = "退出成功";
    public static final String REFRESH_SUCCESS = "令牌刷新成功";
    public static final String TOKEN_INVALID = "令牌无效或已过期，请重新登录";
    public static final String USER_NOT_LOGIN = "用户未登录或登录状态已失效";
    public static final String CAS_AUTH_FAIL = "认证失败";

    // ===================================================================
    // 参数及校验消息
    // ===================================================================

    public static final String PARAM_IS_EMPTY = "请求的参数列表不能为空";
    public static final String PARAM_ID_IS_EMPTY = "至少需要提供一个ID";
    public static final String USER_NOT_FOUND = "未查询到指定用户";
    public static final String UPDATE_FAIL_NOT_FOUND = "更新失败，未找到需要更新的目标资源";
    public static final String DELETE_FAIL_NOT_FOUND = "删除失败，没有找到与提供ID匹配的记录";

    // ===================================================================
    // 系统配置常量
    // ===================================================================

    public static final Integer DEFAULT_PAGE = 0;
    public static final Integer DEFAULT_PAGE_SIZE = 10;
    public static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
    public static final String CQWU_SYSTEM = "JIAOWU";

    /**
     * @description 用户角色类型定义。
     */
    public static final class UserRoleType {
        public static final String ROLE_ADMIN = "ADMIN";
        public static final String ROLE_USER = "USER";
        public static final String ROLE_ROOT = "ROOT";
    }

    /**
     * @description 场所的基础状态，由管理员在后台设置，决定了场所是否从根本上可用。
     */
    public static final class VenueBaseStatus {
        public static final String AVAILABLE = "AVAILABLE"; // 可用
        public static final String MAINTENANCE = "MAINTENANCE"; // 维修中
        public static final String DISABLED = "DISABLED"; // 已禁用
    }

    /**
     * @description 场所的实时状态，由系统根据基础状态、开放时间和当前占用情况周期性计算得出。
     */
    public static final class VenueRealtimeStatus {
        public static final String AVAILABLE = "AVAILABLE"; // 当前可用
        public static final String OCCUPIED = "OCCUPIED"; // 当前被占用
        public static final String CLOSED = "CLOSED"; // 当前未在开放时间
        public static final String UNKNOWN = "UNKNOWN"; // 初始未知状态
    }

    /**
     * @description 时间占用块 (TimeBlock) 的类型定义，用于区分占用原因。
     */
    public static final class TimeBlockType {
        public static final String APPOINTMENT = "APPOINTMENT"; // 用户预约
        public static final String COURSE = "COURSE"; // 教务课程
        public static final String MAINTENANCE = "MAINTENANCE"; // 维修/维护
    }

    /**
     * @description 预约申请 (Reservation) 的生命周期状态定义。
     */
    public static final class ReservationStatus {
        public static final String PENDING = "PENDING"; // 待审批
        public static final String CONFIRMED = "CONFIRMED"; // 已批准
        public static final String IN_PROGRESS = "IN_PROGRESS"; // 进行中
        public static final String COMPLETED = "COMPLETED"; // 已完成
        public static final String REJECTED = "REJECTED"; // 已拒绝
        public static final String CANCELLED = "CANCELLED"; // 已取消
        public static final String EXPIRED = "EXPIRED"; // 已过期（待审批的申请错过了开始时间）
    }
}