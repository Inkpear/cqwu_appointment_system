# **重庆文理学院场所预约平台 API 文档**
> 文档更新于：2025年10月23日

## **通用约定**

### 1. 基础 URL

所有 API 的基础路径为 `/api`。

### 2. 认证

*   除公开接口（如登录、刷新令牌）外，所有 API 都需要通过 `Authorization` 请求头提供认证信息。
*   认证方案为 `Bearer Token`。
*   **示例**: `Authorization: Bearer <Your-Access-Token>`

### 3. 标准响应格式

所有 API 响应都遵循统一的 `AjaxResult` 格式：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    // 业务数据
  }
}
```

*   `code`: HTTP 状态码或自定义业务状态码。
*   `msg`: 响应的文本消息。
*   `data`: 实际的业务数据对象。

---

## **1. 认证模块 (`/api/auth`)**

### **1.1 用户登录 (模拟CAS)**

*   **Endpoint**: `POST /api/auth/login`
*   **描述**: 用户通过认证后，调用此接口登录系统，获取双令牌。
*   **权限**: 公开
*   **Request Body**:
    ```json
    {
      "userId": "20210001",
      "authenticated": true
    }
    ```
    *   **`userId`** (`String`, `Required`): 用户的学号/工号。
    *   **`authenticated`** (`Boolean`, `Required`): 模拟的外部认证结果，必须为 `true`。
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "登录成功",
        "data": {
            "accessToken": "ey...",
            "refreshToken": "ey..."
        }
    }
    ```
*   **Error Response (`400 Bad Request`)**: 如果 `authenticated` 为 `false`。

### **1.2 刷新 Access Token**

*   **Endpoint**: `POST /api/auth/refresh`
*   **描述**: 使用有效的 Refresh Token 获取一个新的 Access Token。
*   **权限**: 公开
*   **Request Body**:
    ```json
    {
      "refreshToken": "ey..."
    }
    ```
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "令牌刷新成功",
        "data": {
            "accessToken": "ey..."
        }
    }
    ```
*   **Error Response (`401 Unauthorized`)**: 如果 Refresh Token 无效或已过期。

### **1.3 退出登录**

*   **Endpoint**: `DELETE /api/auth/logout`
*   **描述**: 用户退出登录，服务器将使 Refresh Token 失效。
*   **权限**: 已认证用户
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "退出成功"
    }
    ```

### **1.4 获取当前用户信息**

*   **Endpoint**: `GET /api/auth/me`
*   **描述**: 获取当前登录用户的完整信息。
*   **权限**: 已认证用户
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "获取用户信息成功",
        "data": {
            "id": "20210001",
            "userType": 1,
            "name": "张三",
            "sex": 1,
            "college": "计算机工程学院",
            "major": "软件工程",
            "role": "user",
            "lastLoginTime": "2025-10-21T10:00:00",
            "createTime": "2025-10-01T09:00:00",
            "updateTime": "2025-10-21T10:00:00"
        }
    }
    ```

---

## **2. 预约模块 (`/api/reservations`)**

### **2.1 创建预约申请**

*   **Endpoint**: `POST /api/reservations`
*   **描述**: 用户提交一个新的场所预约申请，初始状态为 `PENDING`。
*   **权限**: 已认证用户
*   **Request Body**:
        ```json
        {
            "venueId": 1,
            "startDatetime": "2025-11-10T14:00:00",
            "endDatetime": "2025-11-10T16:00:00",
            "title": "团队项目讨论",
            "purpose": "关于毕业设计的最终讨论"
        }
        ```
*   **Success Response (`201 Created`)**:
    ```json
    {
        "code": 201,
        "msg": "预约申请已提交，等待审批",
        "data": {
            "id": 101,
            "venueId": 1,
            "title": "团队项目讨论",
            "startDatetime": "2025-11-10T14:00:00",
            "endDatetime": "2025-11-10T16:00:00",
            "status": "PENDING"
        }
    }
    ```
*   **Error Responses**: `400 Bad Request` (参数错误), `403 Forbidden` (无权预约该场所)。

### **2.2 分页查询预约列表**

*   **Endpoint**: `GET /api/reservations`
*   **描述**: 分页查询预约列表。普通用户只能看到自己的申请，管理员可查看所有。
*   **权限**: 已认证用户
*   **Query Parameters**:
    *   `page` (`Integer`, `Optional`): 页码 (从0开始)，默认0。
    *   `size` (`Integer`, `Optional`): 每页数量，默认10。
    *   `status` (`String`, `Optional`): 按状态筛选 (e.g., `PENDING`, `CONFIRMED`)。
    *   `venueId` (`Long`, `Optional`): 按场所ID筛选。
    *   `date` (`String`, `Optional`): 按日期筛选 (格式: `YYYY-MM-DD`)。
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "查询成功",
        "data": {
            "content": [],
            "pageNumber": 0,
            "pageSize": 10,
            "totalElements": 25,
            "totalPages": 3,
            "last": false
        }
    }
    ```

### **2.3 批准预约申请**

*   **Endpoint**: `PUT /api/reservations/{id}/approve`
*   **描述**: 管理员批准一个状态为 `PENDING` 的预约。
*   **权限**: `ADMIN` 或 `ROOT`
*   **Path Variable**: `id` (`Long`) - 预约ID。
*   **Request Body** (可选):
    ```json
    {
      "approvalComment": "同意"
    }
    ```
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "申请已批准",
        "data": { "status": "CONFIRMED" }
    }
    ```
*   **Error Responses**: `400 Bad Request` (状态不合法), `409 Conflict` (时间冲突)。

### **2.4 拒绝预约申请**

*   **Endpoint**: `PUT /api/reservations/{id}/reject`
*   **描述**: 管理员拒绝一个状态为 `PENDING` 的预约。
*   **权限**: `ADMIN` 或 `ROOT`
*   **Path Variable**: `id` (`Long`) - 预约ID。
*   **Request Body**:
    ```json
    {
      "approvalComment": "该时间段已有重要安排"
    }
    ```
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "申请已拒绝"
    }
    ```

### **2.5 取消预约**

*   **Endpoint**: `DELETE /api/reservations/{id}`
*   **描述**: 用户取消自己的预约，或管理员取消任意预约。
*   **权限**: 已认证用户
*   **Path Variable**: `id` (`Long`) - 预约ID。
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "预约已取消"
    }
    ```
*   **Error Responses**: `400 Bad Request` (状态不合法), `403 Forbidden` (无权限取消)。

### **2.6 根据预约码查询**

*   **Endpoint**: `GET /api/reservations/by-code/{code}`
*   **描述**: 根据唯一的预约确认码查询预约详情。
*   **权限**: 已认证用户
*   **Path Variable**: `code` (`String`) - 预约确认码。
*   **Success Response (`200 OK`)**: `data` 字段为单个 `Reservation` 实体。
*   **Error Response (`404 Not Found`)**: 如果记录未找到。

---

## **3. 场所模块 (`/api/venues`)**

### **3.1 分页搜索场所**

*   **Endpoint**: `GET /api/venues`
*   **描述**: 分页并按条件搜索场所列表。
*   **权限**: 已认证用户
*   **Query Parameters**:
    *   `page`, `size` (`Integer`, `Optional`): 分页参数。
    *   `name`, `location` (`String`, `Optional`): 名称/位置 (模糊匹配)。
    *   `baseStatus` (`String`, `Optional`): 基础状态 (e.g., `AVAILABLE`)。
    *   `realtimeStatus` (`String`, `Optional`): 实时状态 (e.g., `OCCUPIED`)。
    *   `floor`, `categoryId` (`Integer`, `Long`, `Optional`): 按楼层/分类ID筛选。
    *   `requiresApproval` (`Boolean`, `Optional`): 按是否需要审批筛选。
*   **Success Response (`200 OK`)**: `data` 字段为 `PageDTO<Venue>`。

### **3.2 获取场所详情**

*   **Endpoint**: `GET /api/venues/{id}`
*   **描述**: 获取单个场所的详细信息。
*   **权限**: 已认证用户
*   **Path Variable**: `id` (`Long`) - 场所ID。
*   **Success Response (`200 OK`)**: `data` 字段为单个 `Venue` 实体。

### **3.3 获取场所日程**

*   **Endpoint**: `GET /api/venues/{venueId}/time-blocks`
*   **描述**: 获取指定场所在特定时间范围内的所有日程安排（用于日历视图）。
*   **权限**: 已认证用户
*   **Path Variable**: `venueId` (`Long`) - 场所ID。
*   **Query Parameters**:
    *   `start` (`String`, `Required`): 开始时间 (ISO 8601: `YYYY-MM-DDTHH:mm:ss`)。
    *   `end` (`String`, `Required`): 结束时间 (ISO 8601)。
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "查询成功",
        "data": [
            {
                "id": 1,
                "title": "高等数学",
                "start": "2025-10-22T08:00:00",
                "end": "2025-10-22T10:00:00",
                "blockType": "COURSE",
                "ownedByCurrentUser": false
            }
        ]
    }
    ```

### **3.4 批量创建场所**

*   **Endpoint**: `POST /api/venues`
*   **描述**: 批量创建新的场所。
*   **权限**: `ADMIN` 或 `ROOT`
*   **Request Body**: `Venue` 实体数组 `[ { ... }, { ... } ]`。
*   **Success Response (`201 Created`)**: 返回成功创建的数量。

### **3.5 更新场所信息**

*   **Endpoint**: `PUT /api/venues/{id}`
*   **描述**: 更新指定场所的信息。
*   **权限**: `ADMIN` 或 `ROOT`
*   **Path Variable**: `id` (`Long`) - 场所ID。
*   **Request Body**: `UpdateVenueRequest` DTO (包含要更新的字段)。

        UpdateVenueRequest 字段说明 (均为可选，用于部分更新)：

        - `name` (String, max 100)
        - `location` (String, max 200)
        - `floor` (Integer)
        - `capacity` (Integer, >=0)
        - `categoryId` (Long)
        - `equipment` (String)
        - `baseStatus` (String, 可选值: AVAILABLE, MAINTENANCE, DISABLED)
        - `openTime` (String, 格式: `HH:mm:ss`)
        - `closeTime` (String, 格式: `HH:mm:ss`)
        - `requiresApproval` (Boolean)
        - `maxBookingDurationMinutes` (Integer)

        示例：
        ```json
        {
            "name": "多功能教室 A101",
            "requiresApproval": true,
            "baseStatus": "MAINTENANCE",
            "maxBookingDurationMinutes": 180
        }
        ```
*   **Success Response (`200 OK`)**。

### **3.6 批量更新场所信息**

*   **Endpoint**: `PUT /api/venues`
*   **描述**: 批量更新多个场所的通用信息。
*   **权限**: `ADMIN` 或 `ROOT`
*   **Request Body**:
    ```json
    {
      "ids": [1, 2, 3],
            "venueData": {
                "requiresApproval": true,
                "baseStatus": "MAINTENANCE"
            }
    }
    ```
*   **Success Response (`200 OK`)**。

### **3.7 批量删除场所**

*   **Endpoint**: `DELETE /api/venues`
*   **描述**: 根据ID列表批量删除场所。
*   **权限**: `ADMIN` 或 `ROOT`
*   **Request Body**:
    ```json
    {
      "ids": [1, 2, 3]
    }
    ```
*   **Success Response (`200 OK`)**。

---

## **4. 场所分类模块 (`/api/venue-categories`)**

### **4.1 获取所有分类**

*   **Endpoint**: `GET /api/venue-categories`
*   **描述**: 获取所有可用的场所分类列表。
*   **权限**: 已认证用户
*   **Success Response (`200 OK`)**: `data` 字段为 `VenueCategory` 实体数组。

### **4.2 获取分类详情**

*   **Endpoint**: `GET /api/venue-categories/{id}`
*   **描述**: 获取单个分类的详细信息。
*   **权限**: 已认证用户
*   **Path Variable**: `id` (`Long`) - 分类ID。
*   **Success Response (`200 OK`)**: `data` 字段为单个 `VenueCategory` 实体。

### **4.3 创建分类**

*   **Endpoint**: `POST /api/venue-categories`
*   **描述**: 创建一个新的场所分类。
*   **权限**: `ADMIN` 或 `ROOT`
*   **Request Body**: `VenueCategory` 实体。
*   **Success Response (`201 Created`)**。

### **4.4 更新分类**

*   **Endpoint**: `PUT /api/venue-categories/{id}`
*   **描述**: 更新一个已存在的场所分类。
*   **权限**: `ADMIN` 或 `ROOT`
*   **Path Variable**: `id` (`Long`) - 分类ID。
*   **Request Body**: `VenueCategory` 实体。
*   **Success Response (`200 OK`)**。

### **4.5 删除分类**

*   **Endpoint**: `DELETE /api/venue-categories/{id}`
*   **描述**: 删除一个场所分类。
*   **权限**: `ADMIN` 或 `ROOT`
*   **Path Variable**: `id` (`Long`) - 分类ID。
*   **Success Response (`200 OK`)**。
*   **Error Response (`400 Bad Request`)**: 如果分类仍被场所使用。

### **4.6 获取分类下的准入规则**

*   **Endpoint**: `GET /api/venue-categories/{categoryId}/rules`
*   **描述**: 获取指定分类下的所有准入规则。
*   **权限**: 已认证用户
*   **Path Variable**: `categoryId` (`Long`) - 分类ID。
*   **Success Response (`200 OK`)**: `data` 字段为 `VenueCategoryRule` 实体数组。

---

## **5. 场所分类规则模块 (`/api/venue-category-rules`)**

### **5.1 添加准入规则**

*   **Endpoint**: `POST /api/venue-category-rules`
*   **描述**: 为场所分类添加新的准入规则。
*   **权限**: `ADMIN` 或 `ROOT`
*   **Request Body**:
    ```json
    {
      "categoryId": 1,
      "allowedUserType": 2
    }
    ```
*   **Success Response (`201 Created`)**。
*   **Error Response (`400 Bad Request`)**: 如果分类不存在或规则已重复。

### **5.2 删除准入规则**

*   **Endpoint**: `DELETE /api/venue-category-rules/{ruleId}`
*   **描述**: 根据规则ID删除准入规则。
*   **权限**: `ADMIN` 或 `ROOT`
*   **Path Variable**: `ruleId` (`Long`) - 规则ID。
*   **Success Response (`200 OK`)**。

---

## **6. 用户管理模块 (`/api/users`)**

**注意：此模块下所有接口都需要 `ROOT` 权限。**

### **6.1 分页查询用户**

*   **Endpoint**: `GET /api/users`
*   **权限**: `ROOT`
*   **Query Parameters** (`UserQuery` DTO)：
    *   `page` (`Integer`, `Optional`): 页码，默认 `0`，必须 ≥ 0。
    *   `size` (`Integer`, `Optional`): 每页数量，默认 `10`，必须 ≥ 1。
    *   `id` (`String`, `Optional`): 学号/工号，支持模糊匹配。
    *   `name` (`String`, `Optional`): 姓名，支持模糊匹配。
    *   `role` (`String`, `Optional`): 角色，取值示例：`USER`、`ADMIN`、`ROOT`。
    *   `userType` (`Byte`, `Optional`): 用户类型，常用取值 `0`(其他)、`1`(学生)、`2`(教师)。
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "查询成功",
        "data": {
            "content": [
                {
                    "id": "20230001",
                    "userType": 1,
                    "name": "张三",
                    "sex": 1,
                    "college": "计算机工程学院",
                    "major": "软件工程",
                    "role": "USER",
                    "lastLoginTime": "2025-10-20T23:12:00",
                    "createTime": "2024-09-01T08:00:00",
                    "updateTime": "2025-10-15T09:30:00"
                }
            ],
            "pageNumber": 0,
            "pageSize": 10,
            "totalElements": 128,
            "totalPages": 13,
            "last": false
        }
    }
    ```

### **6.2 获取用户详情**

*   **Endpoint**: `GET /api/users/{id}`
*   **权限**: `ROOT`
*   **Path Variable**: `id` (`String`) - 用户ID (学号/工号)。
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "查询成功",
        "data": {
            "id": "20230001",
            "userType": 1,
            "name": "张三",
            "sex": 1,
            "college": "计算机工程学院",
            "major": "软件工程",
            "role": "USER",
            "lastLoginTime": "2025-10-20T23:12:00",
            "createTime": "2024-09-01T08:00:00",
            "updateTime": "2025-10-15T09:30:00"
        }
    }
    ```

### **6.3 更新用户信息**

*   **Endpoint**: `PUT /api/users/{id}`
*   **权限**: `ROOT`
*   **Path Variable**: `id` (`String`) - 用户ID。
*   **Request Body** (`UpdateUserRequest` DTO，所有字段可选)：
        *   `name` (`String`, ≤ 50)
        *   `userType` (`Byte`)
        *   `college` (`String`, ≤ 100)
        *   `major` (`String`, ≤ 100)
        ```json
        {
            "name": "李四",
            "userType": 2,
            "college": "继续教育学院",
            "major": "行政管理"
        }
        ```
*   **Success Response (`200 OK`)**: 当数据有变动时，`msg` 为 “更新成功”；否则返回 “数据未发生变化”。

### **6.4 更新用户角色**

*   **Endpoint**: `PUT /api/users/{id}/role`
*   **权限**: `ROOT`
*   **Path Variable**: `id` (`String`) - 用户ID。
*   **Request Body** (`UpdateUserRoleRequest` DTO)：
        ```json
        {
            "role": "ADMIN"
        }
        ```
        角色合法值：`ROOT`、`ADMIN`、`USER`。
*   **Success Response (`200 OK`)**: 当角色已修改时，`msg` 为 “角色更新成功”；若角色未变化，`msg` 为 “数据未发生变化”。

---

## **7. 日程同步模块 (`/api/sync/schedule`)**

**注意：此模块下所有接口都需要 `ROOT` 权限，且为高危操作。**

### **7.1 同步/增量更新课程安排**

*   **Endpoint**: `POST /api/sync/schedule/courses`
*   **描述**: 强制插入或更新课程安排。此操作会覆盖所有时间冲突的旧课程和用户预约。
*   **权限**: `ROOT`
*   **Request Body**: `SyncCourseRequestDTO` 数组，每个元素字段均为必填：
        *   `venueId` (`Long`): 场所 ID。
        *   `startDatetime` (`String`, ISO-8601): 开始时间。
        *   `endDatetime` (`String`, ISO-8601): 结束时间。
        *   `title` (`String`): 课程标题。
        ```json
        [
            {
                "venueId": 1,
                "startDatetime": "2025-10-22T08:00:00",
                "endDatetime": "2025-10-22T10:00:00",
                "title": "高等数学"
            }
        ]
        ```
        > 后端会将 `blockType` 强制写为 `COURSE`，无需前端传入。
*   **Success Response (`200 OK`)**:
        ```json
        {
                "code": 200,
                "msg": "成功同步 12 条课程记录"
        }
        ```

### **7.2 按场所清空课程安排**

*   **Endpoint**: `DELETE /api/sync/schedule/courses`
*   **描述**: 清空一个或多个指定场所的所有课程安排。
*   **权限**: `ROOT`
*   **Request Body** (`ClearCoursesRequest` DTO)：
        *   `venueIds` (`Long[]`): 需清空课程的场所 ID 列表，不能为空。
        ```json
        {
            "venueIds": [1, 2]
        }
        ```
*   **Success Response (`200 OK`)**:
        ```json
        {
                "code": 200,
                "msg": "成功清空 5 条课程记录"
        }
        ```

### **7.3 按范围清空课程安排**

*   **Endpoint**: `DELETE /api/sync/schedule/courses/by-range`
*   **描述**: 清空单个指定场所在特定时间范围内的所有课程安排。
*   **权限**: `ROOT`
*   **Request Body** (`ClearCoursesByRangeRequest` DTO)：
        *   `venueId` (`Long`): 场所 ID。
        *   `start` (`String`, ISO-8601): 起始时间。
        *   `end` (`String`, ISO-8601): 结束时间。
        ```json
        {
            "venueId": 1,
            "start": "2025-11-01T00:00:00",
            "end": "2025-11-08T00:00:00"
        }
        ```
*   **Success Response (`200 OK`)**:
        ```json
        {
                "code": 200,
                "msg": "成功清空 3 条范围内的课程记录"
        }
        ```

---

## **8. 日志模块 (`/api/logs`)**

权限说明：
- 上报前端日志（单条/批量）需要“已认证用户”。
- 查询接口（后端日志、前端日志分页查询）需要 `ROOT` 权限。

配置说明：
- `logging.frontend.max-per-user`（默认 `20`）：单个用户在数据库中允许保留的前端日志最大条数，超出后按“最旧优先”淘汰；当单次批量本身超过上限时，仅保留本批中“最新的上限条”。

### **8.1 查询后端日志文件**

*   **Endpoint**: `GET /api/logs/backend`
*   **权限**: `ROOT`
*   **描述**: 读取后端日志文件的最新内容，支持按级别筛选、限制行数以及倒序游标查询。
*   **Query Parameters**:
    *   `level` (`String`, `Required`): 日志级别，支持 `debug`、`info`、`warn`、`error`。
    *   `lines` (`Integer`, `Optional`): 返回的日志行数，默认 `100`。
    *   `beforeTimestamp` (`String`, `Optional`): 仅返回该时间点之前的日志，格式 `YYYY-MM-DDTHH:mm:ss`。
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "查询成功",
        "data": [
            "2025-10-21 10:22:33.123  INFO  ... 请求已开始",
            "2025-10-21 10:22:34.456 ERROR ... 数据库连接失败"
        ]
    }
    ```

### **8.2 上报前端日志（单条）**

*   **Endpoint**: `POST /api/logs/frontend`
*   **权限**: 已认证用户
*   **描述**: 前端主动上报运行时错误或关键埋点。后端会记录上报用户以及 `User-Agent`，并对单用户总量做上限控制（超过会淘汰最旧日志）。
*   **Headers**:
    *   `Authorization` (`String`, `Required`): `Bearer <access-token>`
    *   `User-Agent` (`String`, `Required`)
*   **Request Body** (`FrontendLogRequestDTO` DTO)：
    *   `logLevel` (`String`, Required): 日志级别，合法值来自 `Constants.LogLevel`：`DEBUG`、`INFO`、`WARN`、`ERROR`。
    *   `logMessage` (`String`, Required): 详细日志内容，≤ 65535 字符。
    *   `logTimestamp` (`String`, Required, ISO-8601): 前端产生日志的时间。
    ```json
    {
      "logLevel": "ERROR",
      "logMessage": "chunk-xxx.js 加载失败",
      "logTimestamp": "2025-10-21T10:25:30"
    }
    ```
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "前端日志上报成功。"
    }
    ```
*   **Error Responses**:
  - `400 Bad Request`：参数校验未通过（例如 `logLevel` 非法、`logMessage` 为空、`logTimestamp` 为空）。
  - `401 Unauthorized`：未登录或凭证无效。

### **8.2.1 批量上报前端日志**

*   **Endpoint**: `POST /api/logs/frontend/batch`
*   **权限**: 已认证用户
*   **描述**: 批量上报多条日志；后端会在单用户维度严格执行“最大存储条数”上限（配置 `logging.frontend.max-per-user`，默认 20）。
    - 若“现有数量 + 本次批量”超过上限且历史可腾出空间，则删除最旧的若干条旧日志，再插入本次批量。
    - 若本次批量条数本身超过上限，则清空旧日志，仅保留本次批量中“最新的上限条”。
*   **Headers**:
    *   `Authorization` (`String`, `Required`): `Bearer <access-token>`
    *   `User-Agent` (`String`, `Required`)
*   **Request Body**: `FrontendLogRequestDTO[]`
    ```json
    [
      { "logLevel": "INFO",  "logMessage": "init ok", "logTimestamp": "2025-10-21T10:00:00" },
      { "logLevel": "ERROR", "logMessage": "chunk-xxx.js 加载失败", "logTimestamp": "2025-10-21T10:25:30" }
    ]
    ```
*   **Success Response (`200 OK`)**:
    ```json
    { "code": 200, "msg": "前端日志批量上报成功。" }
    ```
*   **Error Responses**:
    - `400 Bad Request`：数组为空或元素校验失败。
    - `401 Unauthorized`：未登录或凭证无效。

合法日志级别（后端常量）
- 源自 `Constants.LogLevel`：`DEBUG`、`INFO`、`WARN`、`ERROR`


### **8.3 分页查询前端日志**

*   **Endpoint**: `GET /api/logs/frontend/query`
*   **权限**: `ROOT`
*   **描述**: 按条件分页检索前端上报日志，返回 `PageDTO<FrontendLogWithBLOBs>`。
*   **Query Parameters** (`FrontendLogQuery` DTO)：
    *   `page` (`Integer`, `Optional`): 页码，默认 `0`，≥ 0。
    *   `size` (`Integer`, `Optional`): 每页数量，默认 `10`，≥ 1。
    *   `logLevel` (`String`, `Optional`): 日志级别精确匹配。
    *   `userId` (`String`, `Optional`): 上报人学号/工号。
    *   `startTime` / `endTime` (`String`, `Optional`): 时间范围过滤，ISO-8601 格式。
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "查询成功",
        "data": {
            "content": [
                {
                    "id": 101,
                    "logLevel": "ERROR",
                    "logTimestamp": "2025-10-21T10:25:30",
                    "userId": "20230001",
                    "logMessage": "chunk-xxx.js 加载失败",
                    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...",
                    "createTime": "2025-10-21T10:25:31"
                }
            ],
            "pageNumber": 0,
            "pageSize": 10,
            "totalElements": 56,
            "totalPages": 6,
            "last": false
        }
    }
    ```

---

## **9. 系统监控模块 (`/api/monitor`)**

### **9.1 获取系统综合信息**

*   **Endpoint**: `GET /api/monitor/system-info`
*   **权限**: `ADMIN` 或 `ROOT`
*   **描述**: 返回服务器、CPU、内存、磁盘的实时监控数据，对应 `SystemInfoDTO`。
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "操作成功",
        "data": {
            "serverInfo": {
                "serverName": "cqwu-app",
                "osName": "Linux",
                "osArch": "amd64",
                "osVersion": "5.15.0",
                "jvmName": "OpenJDK 64-Bit Server VM",
                "jvmVersion": "17.0.9",
                "javaHome": "/usr/lib/jvm/java-17-openjdk",
                "projectDir": "/srv/cqwu_appointment_system"
            },
            "cpuInfo": {
                "cpuModel": "Intel(R) Xeon(R)",
                "physicalCoreCount": 4,
                "logicalCoreCount": 8,
                "systemUsage": 12.3,
                "userUsage": 10.8,
                "ioWait": 1.1,
                "idle": 75.8,
                "totalUsage": 24.2
            },
            "memoryInfo": {
                "total": 16384.0,
                "used": 8042.5,
                "available": 8341.5,
                "usageRate": 49.1
            },
            "diskInfo": [
                {
                    "mountPoint": "/",
                    "fileSystemType": "ext4",
                    "totalSpace": 512000.0,
                    "usedSpace": 301245.6,
                    "freeSpace": 210754.4,
                    "usageRate": 58.8
                }
            ]
        }
    }
    ```

---

## **通知与实时更新（基于 SSE）**

系统使用 Server-Sent Events (SSE) 向前端推送实时事件（单点或广播）。前端可订阅以下事件以获取系统状态变更通知。

### **SSE 订阅**

*   **Endpoint**: `GET /api/notifications/subscribe`
*   **响应类型**: `text/event-stream`
*   **描述**: 建立一个 SSE 连接，服务器会为当前认证用户返回一个持续的事件流。前端需在 `Authorization: Bearer <token>` 头中携带访问令牌。
*   **权限**: 已认证用户

### **常见事件名称 (Constants.SseEvent)**

以下事件名称来自后端常量 `Constants.SseEvent`，事件名保持不变，payload 根据事件类型可能为对象或简单标识：

- `sse-connection-established`：连接建立时服务器发送的握手事件，payload 为连接确认信息（字符串）。
- `sse-ping`：服务器定期发送的心跳事件，用于保活连接；payload 为服务器时间戳（Number）。前端可忽略。
- `new-reservation`：新预约已创建（管理员需处理），payload 推荐为简短对象：{ "reservationId": 123, "venueId": 1, "title": "..." }
- `reservation-approved`：预约被管理员批准，payload 为被批准的预约简要信息或完整 `Reservation` 对象。
- `reservation-rejected`：预约被拒绝，payload 通常包含预约 id 与拒绝原因。
- `reservation-cancelled-by-user`：用户主动取消预约，payload 为预约 id。
- `reservation-cancelled-by-admin`：管理员取消预约，payload 为预约 id 与原因。
- `venue-updated`：单个场所更新或批量更新后广播。payload 可为单个 `Venue` 对象或字符串 `"list_updated"`（表示列表已变化，前端建议重新拉取列表）。
- `venue-deleted`：批量删除场所后广播。payload 为被删除的场所 ID 列表，例如 `[1,2,3]`。
- `venue-category-created`：场所分类创建，payload 为新创建的 `VenueCategory` 对象。
- `venue-category-updated`：场所分类更新，payload 为更新后的 `VenueCategory` 对象。
- `venue-category-deleted`：场所分类删除，payload 为被删除的分类 ID 列表，例如 `[1,2]`。
- `venue-category-rule-updated`：分类规则变更（新增/删除），payload 常为 `"list_updated"`。
- `reservation-auto-updated`：定时任务触发的预约状态自动变更（例如过期、状态推进），payload 为简短描述字符串或受影响的预约 ID 列表。

### **示例：使用 JavaScript 订阅 SSE**

```javascript
const evtSource = new EventSource('/api/notifications/subscribe');

evtSource.addEventListener('sse-connection-established', (e) => console.log('connected', e.data));
evtSource.addEventListener('sse-ping', () => {/* 心跳，可忽略 */});
evtSource.addEventListener('venue-updated', (e) => {
    const payload = JSON.parse(e.data);
    // 如果 payload === 'list_updated' 则重新请求场所列表，否则更新单个场所 UI
});

// 通用错误/关闭处理
evtSource.onerror = (err) => { console.error('SSE error', err); };
// 可选：断线重连策略（伪代码）
// evtSource.onclose = () => setTimeout(connect, backoffMs);

```

### **推送范围与权限约定**

后端支持“按用户”与“按角色”推送：

- 广播（全员）：`sendNotification(Constants.SseBroadcast.ALL_USERS, event, payload)`
- 指定用户：`sendNotification(userId, event, payload)`
- 指定角色：`sendToRole(role, event, payload)` / `sendToRoles([role1, role2], event, payload)`

事件范围（约定）：

- `new-reservation`（新预约待审批）：仅推送给 `ADMIN`、`ROOT`。
- `reservation-cancelled-by-user`（用户取消）：仅推送给 `ADMIN`、`ROOT`。
- `reservation-approved` / `reservation-rejected` / `reservation-cancelled-by-admin`：定向推送给申请人（按用户ID）。
- `venue-updated` / `venue-deleted` / `venue-category-*/rule-updated`：一般广播给所有在线用户，用于提示列表刷新。

为避免“所有客户端同时发起数据刷新”的并发尖峰，服务端对广播采用“分批 + 批内轻微抖动”的分散推送（默认每批 50 个连接、批间隔 300ms、批内抖动 ≤ 200ms；也可退化为纯抖动模式 ≤ 3s）。客户端不需要感知该细节，仅需在收到事件后按自身逻辑拉取最新数据即可。
```

> 注意：SSE 连接在某些代理或负载均衡器下可能需要特殊配置以保持连接；前端应实现自动重连策略并能容忍重复事件。

### 心跳与会话保持

- 服务器心跳策略（自适应）：
    - 下一次心跳间隔 = max(base, ceil(lastCost × P))，并受 max 上限约束。
    - 可配置项：
        - `logging.sse.heartbeat.base`（Duration，默认 `PT25S`）
        - `logging.sse.heartbeat.multiplier`（Double，默认 `1.0`）
        - `logging.sse.heartbeat.max`（Duration，默认 `PT45S`）
    - 目的：在负载升高时适度放缓心跳以减轻压力，同时通过 `max` 上限避免客户端/代理因过长空闲误判掉线。

- 客户端保持与断线检测：
    - 浏览器 `EventSource` 不会向服务器发心跳；连接保持依赖于服务器持续发送事件（如 `sse-ping`）和底层 TCP keepalive。
    - 客户端应实现“软超时”检测：若超过阈值仍未收到任何事件（建议阈值略大于 `heartbeat.max`，例如 `heartbeat.max × 1.5`），则主动关闭并重建 `EventSource`。
    - `EventSource` 内置自动重连（默认约 3s，或由服务器通过 `retry:` 指令调整）。后端可在首次事件中下发合适的重连时间以避免重连风暴。

- 反压与避免重连风暴：
    - 建议在生产网关（如 Nginx）上将 `proxy_read_timeout` 设置为 ≥ `heartbeat.max × 2`，负载均衡的空闲超时也应高于该值。
    - 客户端重连应采用指数退避（例如 3s、6s、12s、…，上限 60s），并在恢复后进行状态校正（例如拉取最新列表）。