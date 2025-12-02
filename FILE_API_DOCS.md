# 文件上传 API 文档

## API 端点快速索引

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 创建上传会话 | GET | `/api/files/upload-session` | 获取 sessionId |
| 获取预签名URL | POST | `/api/files/presigned-url` | 获取 MinIO 上传地址 |
| 上传文件 | PUT | `{uploadUrl}` | 直接上传到 MinIO |
| 创建预约+关联 | POST | `/api/reservations` | 创建预约并关联附件 |
| 场所关联附件 | POST | `/api/venues/{id}/attachments` | 为场所关联附件 |
| 场所关联轮播图 | POST | `/api/venues/{id}/carousel` | 为场所关联轮播图 |
| 查询场所文件 | GET | `/api/venues/{id}/files` | 查询场所轮播图或附件 |
| 查询预约附件 | GET | `/api/reservations/{id}/attachments` | 查询预约的附件列表 |
| 删除文件 | DELETE | `/api/files/{fileId}` | 删除指定文件 |

---

## 核心改进

### 1. SessionId 安全机制
- 服务端签发，存储在 Redis（10分钟有效期）
- Session 携带 `entityType` 和 `fileCategory` 信息
- 跟踪已上传文件数量，实现限额控制
- 参数一致性校验：URL 生成时的类型必须与 session 创建时一致

### 2. 自动清理机制（防止垃圾文件堆积）
**问题**：攻击者可以获取预签名 URL 后上传大文件占用存储，即使不调用确认接口。

**解决方案 - 定时清理任务**：
- ✅ **清理未关联文件**：超过 30 分钟未关联业务实体的临时文件自动删除
- ✅ **清理孤儿文件**：关联的预约/场所已被删除，其附件自动清理
- ✅ **双重删除**：同时删除 MinIO 物理文件和数据库记录
- ⏰ **执行频率**：每小时整点执行一次（`@Scheduled(cron = "0 0 * * * ?")`）
- 📝 **日志记录**：详细记录清理过程和结果

**防护效果**：
- 恶意上传的临时文件最多存活 30 分钟
- 正常用户如果上传后忘记确认，30 分钟后也会被清理（需重新上传）
- 即使在 DDoS 攻击下，垃圾文件也会被定期清理

### 3. 业务限制规则
- ✅ **预约附件**：最多 10 个文件（可配置）
- ✅ **场所文件**：无限制（仅管理员）
- ✅ **预约禁止轮播图**：创建 session 时自动拒绝
- ✅ **类型校验**：防止 session 混用
- ✅ **分离管理**：场所附件和轮播图使用不同接口

---

## API 端点详情

### 1. 创建上传会话
```http
GET /api/files/upload-session?entityType=RESERVATION&fileCategory=ATTACHMENT
Authorization: Bearer {token}

Response:
{
  "code": 200,
  "msg": "上传会话已创建",
  "data": "abc123xyz789"  // sessionId，10分钟有效
}
```

**参数说明**：
- `entityType`: `VENUE` 或 `RESERVATION`
- `fileCategory`: `ATTACHMENT` 或 `CAROUSEL`

**业务规则**：
- 预约 + 轮播图：❌ 直接拒绝
- 预约 + 附件：✅ 限制 10 个文件
- 场所 + 任意类型：✅ 无限制

---

### 2. 获取预签名上传 URL
```http
POST /api/files/presigned-url
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
  "sessionId": "abc123xyz789",
  "originalFilename": "document.pdf",
  "entityType": "RESERVATION",
  "fileCategory": "ATTACHMENT"
}

Response:
{
  "code": 200,
  "msg": "获取上传URL成功",
  "data": {
    "uploadUrl": "http://minio:9000/...",
    "objectName": "reservation/temp_abc123xyz789/attachment/xxx.pdf"
  }
}
```

**校验规则**：
1. Session 有效性（Redis）
2. entityType 一致性（与创建时对比）
3. fileCategory 一致性（与创建时对比）
4. **预约附件数量限制**（已上传 < 10）
5. 文件类型白名单
6. 文件大小限制（20MB）

**错误示例**：
```json
// 数量超限
{
  "code": 400,
  "msg": "预约附件数量超出限制，最多允许 10 个文件"
}

// 类型不匹配
{
  "code": 400,
  "msg": "实体类型不匹配：会话类型为 RESERVATION，请求类型为 VENUE"
}

// 预约使用轮播图
{
  "code": 400,
  "msg": "预约不支持轮播图类型的文件"
}
```

---

### 3. 上传文件到 MinIO
```http
PUT {uploadUrl}
Content-Type: application/octet-stream
Body: <file binary data>

# 直接上传，无需经过后端
```

**重要**：上传到 MinIO 后，**不需要**调用独立的确认接口。文件关联会在调用业务接口（创建预约/更新场所）时自动完成。

---

### 4. 创建业务实体并关联文件

#### 创建预约并关联附件
```http
POST /api/reservations
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
  "venueId": 1,
  "startDatetime": "2025-12-10T10:00:00",
  "endDatetime": "2025-12-10T12:00:00",
  "purpose": "会议",
  "attachmentSessionId": "abc123xyz789"  // 可选：关联附件
}
```

#### 为场所关联附件
```http
POST /api/venues/{venueId}/attachments?sessionId=abc123xyz789
Authorization: Bearer {token}
```

#### 为场所关联轮播图
```http
POST /api/venues/{venueId}/carousel?sessionId=abc123xyz789
Authorization: Bearer {token}
```

**后端自动处理**：
1. 验证 session 有效性和类型一致性
2. 从 MinIO 查询该 session 的所有已上传文件
3. 为每个文件创建数据库记录并关联到实体
4. 清理 Redis session

---

### 5. 查询文件列表

#### 查询场所文件（轮播图或附件）
```http
GET /api/venues/{venueId}/files?fileCategory=CAROUSEL
Authorization: Bearer {token}

Response:
{
  "code": 200,
  "msg": "查询成功",
  "data": [
    {
      "id": 1,
      "originalFilename": "carousel1.jpg",
      "fileCategory": "CAROUSEL",
      "displayOrder": 0,
      "url": "http://minio:9000/..."  // 7天有效预签名URL
    }
  ]
}
```

#### 查询预约附件
```http
GET /api/reservations/{reservationId}/attachments
Authorization: Bearer {token}

Response:
{
  "code": 200,
  "msg": "查询成功",
  "data": [
    {
      "id": 2,
      "originalFilename": "document.pdf",
      "fileCategory": "ATTACHMENT",
      "displayOrder": 0,
      "url": "http://minio:9000/..."
    }
  ]
}
```

---

### 6. 删除文件
```http
DELETE /api/files/{fileId}
Authorization: Bearer {token}

Response:
{
  "code": 200,
  "msg": "文件删除成功"
}
```

**权限规则**：
- 预约附件：创建者或管理员
- 场所文件：仅管理员

---

## 完整上传流程示例

### 场景一：用户上传预约附件并创建预约

```javascript
// 1. 创建会话（指定类型）
const sessionRes = await fetch(
  '/api/files/upload-session?entityType=RESERVATION&fileCategory=ATTACHMENT',
  { headers: { 'Authorization': `Bearer ${token}` } }
);
const sessionId = await sessionRes.json().data;

// 2. 为每个文件获取预签名URL并上传到MinIO
const files = [file1, file2, file3];

for (const file of files) {
  // 2.1 获取预签名URL
  const urlRes = await fetch('/api/files/presigned-url', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      sessionId,
      originalFilename: file.name,
      entityType: 'RESERVATION',
      fileCategory: 'ATTACHMENT'
    })
  });
  
  const { uploadUrl } = await urlRes.json().data;

  // 2.2 直接上传到MinIO
  await fetch(uploadUrl, {
    method: 'PUT',
    body: file
  });
}

// 3. 创建预约并自动关联所有文件
await fetch('/api/reservations', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    venueId: 1,
    startDatetime: '2025-12-10T10:00:00',
    endDatetime: '2025-12-10T12:00:00',
    purpose: '会议',
    attachmentSessionId: sessionId
  })
});
```

### 场景二：管理员为场所上传轮播图

```javascript
// 1. 创建会话（场所+轮播图）
const sessionRes = await fetch(
  '/api/files/upload-session?entityType=VENUE&fileCategory=CAROUSEL',
  { headers: { 'Authorization': `Bearer ${token}` } }
);
const sessionId = await sessionRes.json().data;

// 2. 上传文件到 MinIO
for (const file of carouselImages) {
  const urlRes = await fetch('/api/files/presigned-url', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      sessionId,
      originalFilename: file.name,
      entityType: 'VENUE',
      fileCategory: 'CAROUSEL'
    })
  });
  
  const { uploadUrl } = await urlRes.json().data;
  await fetch(uploadUrl, { method: 'PUT', body: file });
}

// 3. 关联到场所
await fetch(`/api/venues/${venueId}/carousel?sessionId=${sessionId}`, {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` }
});
```

### 场景三：管理员为场所上传附件

```javascript
// 流程与轮播图类似，只是：
// - 创建会话时：entityType=VENUE, fileCategory=ATTACHMENT
// - 关联接口：POST /api/venues/{venueId}/attachments?sessionId={sessionId}
```

### 关键改进

**旧流程的问题**：
```javascript
// ❌ 旧流程：confirm 后文件仍是孤儿
await fetch('/api/files/confirm', { /* ... */ });  // 创建孤儿文件
// 如果用户此时关闭页面，文件就成了垃圾
await fetch('/api/reservations', { /* ... */ });   // 再关联
```

**新流程的优势**：
```javascript
// ✅ 新流程：一步到位，直接关联
// 预约场景
await fetch('/api/reservations', { 
  body: JSON.stringify({ 
    attachmentSessionId: sessionId  // 直接从 MinIO 读取并关联
  })
});

// 场所轮播图场景
await fetch(`/api/venues/${venueId}/carousel?sessionId=${sessionId}`, {
  method: 'POST'
});

// 场所附件场景
await fetch(`/api/venues/${venueId}/attachments?sessionId=${sessionId}`, {
  method: 'POST'
});
```

**优势**：
- ✅ 不会产生孤儿文件
- ✅ 减少 HTTP 请求
- ✅ 更符合业务语义
- ✅ 场所的两种文件类型（附件/轮播图）分离管理

---

## 配置说明

### application.properties
```properties
# 文件上传配置
file.upload.max-size-mb=20
file.upload.presigned-url-expiry-minutes=5
file.upload.max-attachments-per-reservation=10
file.upload.allowed-extensions=.jpg,.jpeg,.png,.pdf,.doc,.docx,.xls,.xlsx,.zip,.rar,.7z,.tar,.gz,.bz2

# MinIO配置
minio.endpoint=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin
minio.bucket-name=cqwu-appointment
```

---

## 常见问题

### Q1: 为什么取消了独立的 confirm 接口？
**A**: 因为 confirm 只创建孤儿文件（`related_entity_id` 为 null），没有实际意义。新设计直接在业务接口（创建预约/更新场所）中完成文件关联，避免产生垃圾数据。

### Q2: 上传到 MinIO 后，如果不调用业务接口会怎样？
**A**: 文件会在 MinIO 中保留，但不会有数据库记录。自动清理任务会在 30 分钟后删除这些无主文件。

### Q3: 为什么要在创建 session 时指定类型？
**A**: 实现业务规则校验和数量限制。预约附件限制 10 个，场所无限制；预约禁止轮播图。

### Q4: 如果超过 10 个文件怎么办？
**A**: 第 11 次调用 `presigned-url` 时会直接拒绝，返回错误提示。

### Q5: Session 过期后已上传的文件会怎样？
**A**: 文件仍在 MinIO 中，但无法通过业务接口关联（session 验证失败）。30 分钟后自动清理任务会删除这些临时文件。

### Q6: 可以在一个 session 中混用不同类型吗？
**A**: 不可以。Session 创建时确定类型，关联时必须使用相同类型，否则拒绝。

### Q7: 管理员上传场所文件为什么不限制？
**A**: 场所由管理员维护，轮播图和附件数量取决于业务需求，不做硬性限制。

### Q8: 如果攻击者上传大量文件怎么办？
**A**: 有多重防护：
1. Session 10 分钟过期，限制攻击窗口
2. 预约附件限制 10 个
3. **自动清理任务每小时删除超过 30 分钟的无主文件**
4. 文件大小限制 20MB
5. 常规带宽限制自然防护

### Q9: 预约或场所删除后，附件会自动删除吗？
**A**: 是的，**自动清理任务每小时检查并删除孤儿文件**（关联实体已不存在的文件）。

### Q10: 清理任务会影响性能吗？
**A**: 不会。任务每小时执行一次，批量处理，且有详细日志。正常情况下待清理文件很少。
