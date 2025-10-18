package com.cqwu.cqwu_appointment_system.common.constant;

import java.util.Locale;

/**
 * 通用常量信息
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
     * 系统语言
     */
    public static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    /**
     * http请求
     */
    public static final String HTTP = "http://";

    /**
     * https请求
     */
    public static final String HTTPS = "https://";

    // ===================================================================
    // 业务操作消息
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

    // ===================================================================
    // 认证授权消息
    // ===================================================================

    public static final String LOGIN_SUCCESS = "登录成功";
    public static final String LOGIN_FAIL = "用户名或密码错误";
    public static final String LOGOUT_SUCCESS = "退出成功";
    public static final String REFRESH_SUCCESS = "令牌刷新成功";
    public static final String TOKEN_INVALID = "令牌无效或已过期，请重新登录";
    public static final String USER_NOT_LOGIN = "用户未登录或登录状态已失效";
    public static final String CAS_AUTH_FAIL = "认证失败";

    // ===================================================================
    // 参数及特定场景校验消息
    // ===================================================================

    public static final String PARAM_IS_EMPTY = "请求的参数列表不能为空";
    public static final String PARAM_ID_IS_EMPTY = "至少需要提供一个ID";
    public static final String USER_NOT_FOUND = "未查询到指定用户";

    // [优化] 常量名更清晰，与 UPDATE_FAIL 解耦
    public static final String UPDATE_FAIL_NOT_FOUND = "更新失败，未找到需要更新的目标资源";

    // [新增] 用于替代Controller中的硬编码字符串
    public static final String DELETE_FAIL_NOT_FOUND = "删除失败，没有找到与提供ID匹配的记录";

    // ===================================================================
    // 系统配置常量
    // ===================================================================

    public static final Integer DEFAULT_PAGE = 0;
    public static final Integer DEFAULT_PAGE_SIZE = 10;
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ROOT = "ROOT";
    public static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
}