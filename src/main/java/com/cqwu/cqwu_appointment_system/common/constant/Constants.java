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

    /**
     * 查询成功消息
     */
    public static final String QUERY_SUCCESS = "查询成功";

    /**
     * 查询为空消息
     */
    public static final String QUERY_NOT_FOUND = "未查询到数据";

    /**
     * 查询失败消息
     */
    public static final String QUERY_FAIL = "查询失败";

    /**
     * 创建成功消息
     */
    public static final String CREATE_SUCCESS = "创建成功";

    /**
     * 创建失败消息
     */
    public static final String CREATE_FAIL = "创建失败";

    /**
     * 更新成功消息
     */
    public static final String UPDATE_SUCCESS = "更新成功";

    /**
     * 更新失败消息
     */
    public static final String UPDATE_FAIL = "更新失败或资源不存在";

    /**
     * 数据未变化警告
     */
    public static final String DATA_NOT_CHANGED = "数据未发生变化";

    /**
     * 删除成功消息
     */
    public static final String DELETE_SUCCESS = "删除成功";

    /**
     * 删除失败消息
     */
    public static final String DELETE_FAIL = "删除失败";

    /**
     * 批量创建成功消息
     */
    public static final String BATCH_CREATE_SUCCESS = "批量创建成功";

    /**
     * 批量创建失败消息
     */
    public static final String BATCH_CREATE_FAIL = "批量创建失败";

    // ===================================================================
    // 认证授权消息
    // ===================================================================

    /**
     * 登录成功消息
     */
    public static final String LOGIN_SUCCESS = "登录成功";

    /**
     * 登录失败消息
     */
    public static final String LOGIN_FAIL = "登录失败";

    /**
     * 退出成功消息
     */
    public static final String LOGOUT_SUCCESS = "退出成功";

    /**
     * 令牌刷新成功消息
     */
    public static final String REFRESH_SUCCESS = "令牌刷新成功";

    /**
     * 令牌无效或过期消息
     */
    public static final String TOKEN_INVALID = "令牌无效或已过期，请重新登录";

    /**
     * 用户未登录消息
     */
    public static final String USER_NOT_LOGIN = "用户未登录或登录状态已失效";

    /**
     * 
     * 模拟CAS认证失败消息
     */
    public static final String CAS_AUTH_FAIL = "模拟CAS认证失败";

    // ===================================================================
    // 参数校验消息
    // ===================================================================

    /**
     * 参数列表为空消息
     */
    public static final String PARAM_IS_EMPTY = "请求的参数列表不能为空";

    /**
     * ID参数为空消息
     */
    public static final String PARAM_ID_IS_EMPTY = "至少需要提供一个ID";

    // ===================================================================
    // 系统配置常量
    // ===================================================================

    /**
     * 默认分页页码
     */
    public static final Integer DEFAULT_PAGE = 0;

    /**
     * 默认分页大小
     */
    public static final Integer DEFAULT_PAGE_SIZE = 10; // (建议将 5 改为 10，更常用)

    /**
     * 管理员角色标识
     */
    public static final String ROLE_ADMIN = "admin";

    /**
     * 普通用户角色标识
     */
    public static final String ROLE_USER = "user";

    /**
     * 存储 Refresh Token 的 Redis Key 前缀
     */
    public static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
}