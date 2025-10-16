package com.cqwu.cqwu_appointment_system.common.constant;

import java.util.Locale;

/**
 * 返回常量
 * 
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
     * www主域
     */
    public static final String WWW = "www.";

    /**
     * http请求
     */
    public static final String HTTP = "http://";

    /**
     * https请求
     */
    public static final String HTTPS = "https://";

    /**
     * 通用成功标识
     */
    public static final String SUCCESS = "0";

    /**
     * 通用失败标识
     */
    public static final String FAIL = "1";

    /**
     * 默认分页位置
     */
    public static final Integer DEFAULT_PAGE = 0;

    /**
     * 默认分页大小
     */
    public static final Integer DEFAULT_PAGE_SIZE = 5;

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
     * 查询失败消息
     */
    public static final String CREATE_FAIL = "创建失败";

    /**
     * 更新成功消息
     */
    public static final String UPDATE_SUCCESS = "更新成功";

    /**
     * 更新失败消息
     */
    public static final String UPDATE_FAIL = "更新失败或场所不存在";

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
     * 批量操作相关消息
     */
    public static final String BATCH_CREATE_SUCCESS = "批量创建成功";
    public static final String BATCH_CREATE_FAIL = "批量创建失败";
    public static final String BATCH_DELETE_SUCCESS = "批量删除成功";
    public static final String BATCH_DELETE_FAIL = "批量删除失败";

    /**
     * 参数错误消息
     */
    public static final String PARAM_IS_EMPTY = "请求的参数列表不能为空";
    public static final String PARAM_ID_IS_EMPTY = "至少需要提供一个ID";
}
