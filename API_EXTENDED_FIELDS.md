# 扩展字段功能 API 文档

> 本文档详细说明预约参与人员和场所描述扩展字段的相关 API
> 
> 更新时间：2025年12月3日

---

## 概述

系统新增了两组扩展信息表：
- **预约扩展信息** (`reservation_detail`)：存储参与人员信息
- **场所扩展信息** (`venue_detail`)：存储场所详细描述

这些扩展字段通过独立的表存储，采用 1-to-1 关联，支持灵活扩展而不影响主表结构。

---

## 1. 预约模块扩展字段

### 1.1 创建预约（支持扩展字段）

**接口**: `POST /api/reservations`

**新增字段说明**:
- `participants` (String, 可选)：参与人员名单，格式由前端决定（如逗号分隔、JSON 字符串等）
- `participantCount` (Integer, 可选)：参与人数

**完整请求示例**:
```json
{
  "venueId": 1,
  "startDatetime": "2025-12-10T14:00:00",
  "endDatetime": "2025-12-10T16:00:00",
  "title": "学术讲座",
  "purpose": "邀请专家进行学术交流",
  "participants": "张三,李四,王五,赵六",
  "participantCount": 4,
  "attachmentSessionId": "session-xxx"
}
```

**字段详解**:
- `participants`: 
  - 类型：String (TEXT)
  - 可选字段
  - 无格式校验（信任机制）
  - 建议格式：逗号分隔的姓名列表
  - 最大长度：由数据库 TEXT 类型决定（约 65KB）

- `participantCount`:
  - 类型：Integer
  - 可选字段
  - 无范围校验（信任机制）
  - 由前端根据参与人员列表自动计算

**响应**: 与原有接口相同，返回基础预约信息

**兼容性**: 
- ✅ 完全向后兼容
- 旧版前端不传这两个字段，系统正常工作
- 新版前端传递这两个字段，系统自动保存到扩展表

---

### 1.2 查询预约详情（含扩展信息）

**接口**: `GET /api/reservations/{id}`

**描述**: 查询单个预约的详细信息，包含参与人员等扩展字段

**权限**: 已认证用户

**请求参数**:
- `id` (Long, Path Variable, 必填)：预约ID

**成功响应** (`200 OK`):
```json
{
  "code": 200,
  "msg": "查询成功",
  "data": {
    "id": 1,
    "venueId": 1,
    "title": "学术讲座",
    "purpose": "邀请专家进行学术交流",
    "startDatetime": "2025-12-10T14:00:00",
    "endDatetime": "2025-12-10T16:00:00",
    "status": "CONFIRMED",
    "creatorUserId": "20210001",
    "createTime": "2025-12-03T10:00:00",
    "updateTime": "2025-12-03T10:30:00",
    "approvalComment": null,
    "approvedBy": "admin",
    "approvedAt": "2025-12-03T10:30:00",
    
    // ====== 扩展字段 ======
    "participants": "张三,李四,王五,赵六",
    "participantCount": 4
    // ====================
  }
}
```

**扩展字段说明**:
- `participants`: 参与人员名单（如果创建时未提供，则为 `null`）
- `participantCount`: 参与人数（如果创建时未提供，则为 `null`）

**错误响应**:
- `404 Not Found`: 预约不存在

---

### 1.3 导出所有预约数据（含扩展信息）

**接口**: `GET /api/reservations/export`

**描述**: 导出所有预约的完整数据，包含参与人员等扩展字段，供前端生成 Excel

**权限**: `ADMIN` 或 `ROOT`

**成功响应** (`200 OK`):
```json
{
  "code": 200,
  "msg": "导出成功",
  "data": [
    {
      "id": 1,
      "venueId": 1,
      "title": "学术讲座",
      "purpose": "邀请专家进行学术交流",
      "startDatetime": "2025-12-10T14:00:00",
      "endDatetime": "2025-12-10T16:00:00",
      "status": "CONFIRMED",
      "creatorUserId": "20210001",
      "participants": "张三,李四,王五,赵六",
      "participantCount": 4,
      "createTime": "2025-12-03T10:00:00"
    },
    {
      "id": 2,
      "venueId": 2,
      "title": "团队会议",
      "purpose": "季度工作总结",
      "startDatetime": "2025-12-11T09:00:00",
      "endDatetime": "2025-12-11T11:00:00",
      "status": "PENDING",
      "creatorUserId": "20210002",
      "participants": "王经理,李主管,张员工",
      "participantCount": 3,
      "createTime": "2025-12-03T11:00:00"
    }
  ]
}
```

**导出数据特点**:
- 包含所有预约的基础信息和扩展信息
- 按创建时间倒序排列
- 所有字段都会返回（即使为 `null`）
- 前端可直接用于 Excel 生成

---

## 2. 场所模块扩展字段

### 2.1 查询场所详情（含扩展信息）

**接口**: `GET /api/venues/{id}`

**描述**: 查询单个场所的详细信息，现在包含场所描述扩展字段

**权限**: 已认证用户

**请求参数**:
- `id` (Long, Path Variable, 必填)：场所ID

**成功响应** (`200 OK`):
```json
{
  "code": 200,
  "msg": "查询成功",
  "data": {
    "id": 1,
    "name": "学术报告厅",
    "location": "教学楼A栋3层",
    "floor": 3,
    "capacity": 200,
    "categoryId": 1,
    "equipment": "投影仪,音响设备,话筒",
    "baseStatus": "AVAILABLE",
    "realtimeStatus": "AVAILABLE",
    "openTime": "08:00:00",
    "closeTime": "22:00:00",
    "requiresApproval": true,
    "maxBookingDurationMinutes": 240,
    "createTime": "2025-01-01T00:00:00",
    "updateTime": "2025-12-03T15:00:00",
    
    // ====== 扩展字段 ======
    "description": "# 学术报告厅使用说明\n\n## 场所概况\n\n学术报告厅位于教学楼A栋3层，是学校最大的多功能学术活动场所。\n\n## 设备清单\n\n- 高清投影仪（4K分辨率）\n- 专业音响系统\n- 无线话筒 x 4\n- 激光笔 x 2\n- 白板 x 2\n\n## 座位安排\n\n- 总座位数：200个\n- 前排VIP座位：20个\n- 普通座位：180个\n- 轮椅通道：2条\n\n## 使用须知\n\n1. 请提前30分钟到场调试设备\n2. 活动结束后请关闭所有设备\n3. 保持场地整洁\n4. 禁止饮食\n\n## 联系方式\n\n- 管理员：张老师\n- 电话：023-12345678\n- 紧急联系：138-0000-0000"
    // ====================
  }
}
```

**扩展字段说明**:
- `description`: 
  - 类型：String (TEXT)
  - 场所详细描述（支持 Markdown 格式）
  - 如果未设置，则为 `null`
  - 前端应使用 Markdown 渲染器显示此字段

**向后兼容**:
- ✅ 响应结构向后兼容
- 旧版前端会忽略 `description` 字段
- 新版前端可以显示 Markdown 格式的场所说明

**错误响应**:
- `404 Not Found`: 场所不存在

---

### 2.2 更新场所信息（支持描述字段）

**接口**: `PUT /api/venues/{id}`

**描述**: 更新场所信息，现在支持更新场所描述扩展字段

**权限**: `ADMIN` 或 `ROOT`

**请求参数**:
- `id` (Long, Path Variable, 必填)：场所ID

**请求体示例**:
```json
{
  "name": "学术报告厅",
  "location": "教学楼A栋3层",
  "capacity": 200,
  "requiresApproval": true,
  "description": "# 学术报告厅使用说明\n\n## 场所概况\n\n这是一个支持Markdown格式的详细描述...\n\n## 设备清单\n\n- 设备1\n- 设备2\n\n## 使用须知\n\n请遵守相关规定..."
}
```

**扩展字段说明**:
- `description` (String, 可选):
  - 场所详细描述
  - 支持 Markdown 格式
  - 如果不传此字段，不会修改现有描述
  - 如果传空字符串 `""`，会清空描述
  - 最大长度：由数据库 TEXT 类型决定（约 65KB）
  - 使用 **upsert 模式**：如果扩展信息不存在则插入，存在则更新

**成功响应** (`200 OK`):
```json
{
  "code": 200,
  "msg": "更新成功",
  "data": {
    "id": 1,
    "name": "学术报告厅",
    "location": "教学楼A栋3层",
    "capacity": 200,
    "baseStatus": "AVAILABLE",
    "description": "# 学术报告厅使用说明\n\n...",
    // ... 其他字段
  }
}
```

**响应说明**:
- 返回更新后的完整场所信息（包含扩展字段）
- 会触发 SSE 事件 `venue_updated`，通知所有在线用户

**错误响应**:
- `404 Not Found`: 场所不存在
- `400 Bad Request`: 参数校验失败

**兼容性**:
- ✅ 完全向后兼容
- 旧版前端不传 `description` 字段，系统正常更新其他字段
- 新版前端传递 `description` 字段，系统自动保存到扩展表

---

## 3. 数据模型

### 3.1 预约扩展信息 (ReservationDetail)

```json
{
  "id": 1,
  "reservationId": 101,
  "participants": "张三,李四,王五",
  "participantCount": 3,
  "createTime": "2025-12-03T10:00:00",
  "updateTime": "2025-12-03T10:00:00"
}
```

**字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 主键 |
| `reservationId` | Long | 关联的预约ID（外键，CASCADE DELETE） |
| `participants` | String | 参与人员名单 |
| `participantCount` | Integer | 参与人数 |
| `createTime` | LocalDateTime | 创建时间 |
| `updateTime` | LocalDateTime | 更新时间 |

---

### 3.2 场所扩展信息 (VenueDetail)

```json
{
  "id": 1,
  "venueId": 1,
  "description": "# 场所说明\n\n支持Markdown格式...",
  "createTime": "2025-12-03T10:00:00",
  "updateTime": "2025-12-03T15:00:00"
}
```

**字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 主键 |
| `venueId` | Long | 关联的场所ID（外键，CASCADE DELETE） |
| `description` | String | 场所详细描述（支持Markdown） |
| `createTime` | LocalDateTime | 创建时间 |
| `updateTime` | LocalDateTime | 更新时间 |

---

## 4. 设计特点

### 4.1 信任机制

- **无格式校验**：后端不校验 `participants` 字段的格式，完全信任前端提供的数据
- **无范围校验**：`participantCount` 无最小/最大值限制
- **灵活格式**：前端可以自由决定参与人员的存储格式（逗号分隔、JSON、XML 等）

### 4.2 独立扩展表

- **不影响主表**：扩展字段存储在独立表中，不污染主表结构
- **1-to-1 关联**：每个预约/场所最多对应一条扩展信息
- **可选性**：扩展信息完全可选，不影响核心功能

### 4.3 级联删除

- **数据库级别**：外键配置 `ON DELETE CASCADE`
- **应用层级别**：Service 层显式删除扩展信息（双重保障）
- **批量支持**：支持批量删除预约/场所时自动清理扩展信息

### 4.4 向后兼容

所有扩展字段均为**可选字段**，确保：
- 旧版 API 调用完全兼容
- 旧版前端无需修改即可正常使用
- 新版前端可以选择性地使用扩展功能

---

## 5. 前端集成建议

### 5.1 预约模块

**创建预约时**:
```javascript
// 收集参与人员信息
const participants = ["张三", "李四", "王五"].join(",");
const participantCount = 3;

// 提交预约请求
const response = await fetch('/api/reservations', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    venueId: 1,
    title: "学术讲座",
    startDatetime: "2025-12-10T14:00:00",
    endDatetime: "2025-12-10T16:00:00",
    participants: participants,  // 扩展字段
    participantCount: participantCount  // 扩展字段
  })
});
```

**查询预约详情时**:
```javascript
const response = await fetch('/api/reservations/1', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const data = await response.json();
console.log(data.data.participants);  // "张三,李四,王五"
console.log(data.data.participantCount);  // 3
```

**导出预约数据时**:
```javascript
const response = await fetch('/api/reservations/export', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const data = await response.json();
// data.data 是包含所有扩展字段的预约列表
// 可以直接用于生成 Excel
exportToExcel(data.data);
```

---

### 5.2 场所模块

**查询场所详情时**:
```javascript
const response = await fetch('/api/venues/1', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const data = await response.json();

// 使用 Markdown 渲染器显示描述
if (data.data.description) {
  const html = marked.parse(data.data.description);
  document.getElementById('venue-description').innerHTML = html;
}
```

**更新场所信息时**:
```javascript
// 使用 Markdown 编辑器收集描述内容
const markdownContent = editor.getValue();

const response = await fetch('/api/venues/1', {
  method: 'PUT',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    name: "学术报告厅",
    capacity: 200,
    description: markdownContent  // 扩展字段
  })
});
```

---

## 6. SSE 实时通知

扩展字段的修改会触发相应的 SSE 事件：

### 6.1 预约相关事件

- **事件名**: `new-reservation`
- **触发时机**: 创建预约时（包含扩展字段）
- **接收对象**: ADMIN、ROOT、MONITOR 角色

### 6.2 场所相关事件

- **事件名**: `venue-updated`
- **触发时机**: 更新场所信息时（包含描述字段）
- **接收对象**: 所有在线用户
- **数据内容**: 完整的 `VenueDetailDto`（包含 `description`）

---

## 7. 常见问题 (FAQ)

### Q1: 参与人员字段支持什么格式？

**A**: 后端采用信任机制，不对格式进行校验。建议格式：
- 简单列表：`"张三,李四,王五"`
- JSON 数组：`"[\"张三\",\"李四\",\"王五\"]"`
- 带详情：`"张三(13800000001),李四(13800000002)"`

前端可以自由选择最适合的格式。

### Q2: 如果不传扩展字段会怎样？

**A**: 完全正常工作，扩展字段为可选字段。
- 创建预约时不传：扩展表不会插入记录
- 查询时：扩展字段返回 `null`
- 更新场所时不传 `description`：不会修改现有描述

### Q3: 旧版前端需要适配吗？

**A**: 不需要。所有改动都是向后兼容的：
- 旧版前端不传扩展字段：系统正常工作
- 查询接口返回的扩展字段：旧版前端会自动忽略

### Q4: 场所描述支持哪些 Markdown 语法？

**A**: 支持标准 Markdown 语法，包括但不限于：
- 标题：`#`、`##`、`###`
- 列表：`-`、`*`、`1.`
- 粗体：`**text**`
- 斜体：`*text*`
- 链接：`[text](url)`
- 代码块：` ```code``` `
- 表格、引用等

建议前端使用 `marked.js` 或类似库进行渲染。

### Q5: 扩展字段的最大长度是多少？

**A**: 
- `participants`: TEXT 类型，约 65KB（约 65,000 字符）
- `description`: TEXT 类型，约 65KB

实际使用中，这些长度已经足够存储大量内容。

### Q6: 删除预约/场所时，扩展信息会自动删除吗？

**A**: 会。采用双重保障机制：
1. 数据库级别：外键 `ON DELETE CASCADE`
2. 应用层级别：Service 层显式删除

确保不会留下孤立的扩展数据。

---

## 8. 数据库迁移

### 执行 SQL 脚本

```bash
# 连接到数据库
mysql -h192.168.31.199 -P3306 -uroot -padmin123 cqwu

# 执行建表脚本
source sql/add_detail_tables.sql;

# 或者
mysql -h192.168.31.199 -P3306 -uroot -padmin123 cqwu < sql/add_detail_tables.sql
```

### 表结构

```sql
-- 预约扩展信息表
CREATE TABLE reservation_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id BIGINT NOT NULL UNIQUE,
    participants TEXT COMMENT '参与人员名单',
    participant_count INT COMMENT '参与人数',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (reservation_id) REFERENCES reservation(id) ON DELETE CASCADE
);

-- 场所扩展信息表
CREATE TABLE venue_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    venue_id BIGINT NOT NULL UNIQUE,
    description TEXT COMMENT '场所详细描述（支持Markdown）',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (venue_id) REFERENCES venue(id) ON DELETE CASCADE
);
```

---

## 9. 相关文档

- [主 API 文档](./api.md) - 完整的 API 接口文档
- [场所统计与导出 API](./api_export.md) - 场所统计功能文档
- [功能设计文档](./FEATURE_DETAIL_TABLES.md) - 扩展字段的设计说明和技术细节

---

**文档结束**
