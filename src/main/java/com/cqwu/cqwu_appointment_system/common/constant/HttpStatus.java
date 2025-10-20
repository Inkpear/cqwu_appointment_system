package com.cqwu.cqwu_appointment_system.common.constant;

/**
 * @description 定义了应用中统一使用的 HTTP 响应状态码常量。
 *              这有助于避免在代码中使用“魔法数字”，并保持响应码的一致性。
 */
public class HttpStatus
{
    /**
     * 操作成功 (200 OK)
     */
    public static final int SUCCESS = 200;

    /**
     * 对象创建成功 (201 Created)
     */
    public static final int CREATED = 201;

    /**
     * 请求已被接受，将进行异步处理 (202 Accepted)
     */
    public static final int ACCEPTED = 202;

    /**
     * 操作成功，但响应体中没有内容 (204 No Content)
     */
    public static final int NO_CONTENT = 204;

    /**
     * 资源已被永久移动 (301 Moved Permanently)
     */
    public static final int MOVED_PERM = 301;

    /**
     * 重定向到其他 URI (303 See Other)
     */
    public static final int SEE_OTHER = 303;

    /**
     * 资源没有被修改 (304 Not Modified)
     */
    public static final int NOT_MODIFIED = 304;

    /**
     * 客户端请求错误，如参数列表错误（缺少、格式不匹配） (400 Bad Request)
     */
    public static final int BAD_REQUEST = 400;

    /**
     * 未授权，需要身份认证 (401 Unauthorized)
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * 禁止访问，服务器理解请求，但拒绝执行 (403 Forbidden)
     */
    public static final int FORBIDDEN = 403;

    /**
     * 请求的资源不存在 (404 Not Found)
     */
    public static final int NOT_FOUND = 404;

    /**
     * 不允许的 HTTP 方法 (405 Method Not Allowed)
     */
    public static final int BAD_METHOD = 405;

    /**
     * 资源冲突，通常发生在 PUT 请求中 (409 Conflict)
     */
    public static final int CONFLICT = 409;

    /**
     * 不支持的媒体类型 (415 Unsupported Media Type)
     */
    public static final int UNSUPPORTED_TYPE = 415;

    /**
     * 服务器内部错误 (500 Internal Server Error)
     */
    public static final int ERROR = 500;

    /**
     * 接口未实现 (501 Not Implemented)
     */
    public static final int NOT_IMPLEMENTED = 501;

    /**
     * 自定义警告消息状态码，非标准HTTP状态码
     */
    public static final int WARN = 601;
}