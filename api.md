# **重庆文理学院场所预约平台 API 文档**

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
*   **Query Parameters**:
    *   `page`, `size` (`Integer`, `Optional`): 分页参数。
    *   `id` (`String`, `Optional`): 学号/工号 (模糊匹配)。
    *   `name` (`String`, `Optional`): 姓名 (模糊匹配)。
    *   `role` (`String`, `Optional`): 角色 (e.g., `user`, `admin`)。
    *   `userType` (`Byte`, `Optional`): 用户类型 (e.g., `1` for 学生)。
*   **Success Response (`200 OK`)**: `data` 字段为 `PageDTO<User>`。

### **6.2 获取用户详情**

*   **Endpoint**: `GET /api/users/{id}`
*   **权限**: `ROOT`
*   **Path Variable**: `id` (`String`) - 用户ID (学号/工号)。
*   **Success Response (`200 OK`)**: `data` 字段为单个 `User` 实体。

### **6.3 更新用户信息**

*   **Endpoint**: `PUT /api/users/{id}`
*   **权限**: `ROOT`
*   **Path Variable**: `id` (`String`) - 用户ID。
*   **Request Body**: `UpdateUserRequest` DTO (可包含 `name`, `userType`, `college`, `major`)。
*   **Success Response (`200 OK`)**。

### **6.4 更新用户角色**

*   **Endpoint**: `PUT /api/users/{id}/role`
*   **权限**: `ROOT`
*   **Path Variable**: `id` (`String`) - 用户ID。
*   **Request Body**:
    ```json
    {
      "role": "ADMIN"
    }
    ```
*   **Success Response (`200 OK`)**。

---

## **7. 日程同步模块 (`/api/sync/schedule`)**

**注意：此模块下所有接口都需要 `ROOT` 权限，且为高危操作。**

### **7.1 同步/增量更新课程安排**

*   **Endpoint**: `POST /api/sync/schedule/courses`
*   **描述**: 强制插入或更新课程安排。此操作会覆盖所有时间冲突的旧课程和用户预约。
*   **权限**: `ROOT`
*   **Request Body**:
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
*   **Success Response (`200 OK`)**: 返回成功同步的数量。

### **7.2 按场所清空课程安排**

*   **Endpoint**: `DELETE /api/sync/schedule/courses`
*   **描述**: 清空一个或多个指定场所的所有课程安排。
*   **权限**: `ROOT`
*   **Request Body**:
    ```json
    {
      "venueIds": [1, 2]
    }
    ```
*   **Success Response (`200 OK`)**: 返回成功清空的数量。

### **7.3 按范围清空课程安排**

*   **Endpoint**: `DELETE /api/sync/schedule/courses/by-range`
*   **描述**: 清空单个指定场所在特定时间范围内的所有课程安排。
*   **权限**: `ROOT`
*   **Request Body**:
    ```json
    {
      "venueId": 1,
      "start": "2025-11-01T00:00:00",
      "end": "2025-11-08T00:00:00"
    }
    ```
*   **Success Response (`200 OK`)**: 返回成功清空的数量。