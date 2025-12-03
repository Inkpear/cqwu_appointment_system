# 场所统计与数据导出 API 文档

> 本文档补充 `api.md` 中未包含的场所统计和数据导出接口

---

## **场所统计与数据导出**

### **1. 获取场所统计信息**

*   **Endpoint**: `GET /api/venues/{id}/statistics`
*   **描述**: 查询指定场所的统计信息，用于展示场所热门程度和使用情况。
*   **权限**: 已认证用户
*   **Path Parameters**:
    *   `id` (`Long`, `Required`): 场所ID
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "查询成功",
        "data": {
            "venueId": 1,
            "venueName": "学术报告厅",
            "totalReservations": 156,
            "successfulReservations": 120,
            "pendingReservations": 8,
            "ongoingReservations": 2,
            "completedReservations": 118,
            "totalParticipants": 120,
            "uniqueUsers": 45,
            "recentReservations": 23,
            "approvalRate": 85.5,
            "averageDurationMinutes": 120.5,
            "lastReservationTime": "2025-12-02T14:30:00",
            "popularityScore": 78,
            "utilizationRate": 45.8
        }
    }
    ```
*   **统计指标说明**:
    *   `totalReservations`: 总预约数量
    *   `successfulReservations`: 成功的预约数（已完成）
    *   `pendingReservations`: 待审批数量
    *   `ongoingReservations`: 进行中数量
    *   `completedReservations`: 已完成数量
    *   `totalParticipants`: 总预约人次
    *   `uniqueUsers`: 独立用户数
    *   `recentReservations`: 近30天预约数
    *   `approvalRate`: 审批通过率（%）
    *   `averageDurationMinutes`: 平均预约时长（分钟）
    *   `lastReservationTime`: 最后一次预约时间
    *   `popularityScore`: 热门度评分（0-100分）
        - 算法：近期活跃度（40%）+ 用户多样性（30%）+ 审批通过率（20%）+ 历史累计（10%）
    *   `utilizationRate`: 利用率（%），近30天实际使用时间 / 可用时间
*   **Error Response (`404 Not Found`)**: 场所不存在

---

### **2. 导出所有场所统计信息**

*   **Endpoint**: `GET /api/venues/export/statistics`
*   **描述**: 导出所有场所的统计信息（JSON格式），由前端打包为Excel文件。
*   **权限**: 仅 `ADMIN` 和 `ROOT`
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "导出成功",
        "data": [
            {
                "venueId": 1,
                "venueName": "学术报告厅",
                "totalReservations": 156,
                "successfulReservations": 120,
                "pendingReservations": 8,
                "ongoingReservations": 2,
                "completedReservations": 118,
                "totalParticipants": 120,
                "uniqueUsers": 45,
                "recentReservations": 23,
                "approvalRate": 85.5,
                "averageDurationMinutes": 120.5,
                "lastReservationTime": "2025-12-02T14:30:00",
                "popularityScore": 78,
                "utilizationRate": 45.8
            },
            {
                "venueId": 2,
                "venueName": "会议室A201",
                "totalReservations": 89,
                "successfulReservations": 75,
                "pendingReservations": 3,
                "ongoingReservations": 1,
                "completedReservations": 74,
                "totalParticipants": 75,
                "uniqueUsers": 32,
                "recentReservations": 15,
                "approvalRate": 90.2,
                "averageDurationMinutes": 90.0,
                "lastReservationTime": "2025-12-01T10:00:00",
                "popularityScore": 65,
                "utilizationRate": 38.5
            }
        ]
    }
    ```
*   **Error Response (`403 Forbidden`)**: 无管理员权限

---

### **3. 导出所有预约数据**

*   **Endpoint**: `GET /api/reservations/export`
*   **描述**: 导出所有预约的数据库原始数据（JSON格式），由前端打包为Excel文件。按创建时间倒序排列。
*   **权限**: 仅 `ADMIN` 和 `ROOT`
*   **Success Response (`200 OK`)**:
    ```json
    {
        "code": 200,
        "msg": "导出成功",
        "data": [
            {
                "id": 1245,
                "venueId": 3,
                "creatorUserId": 20210001,
                "title": "学术讲座",
                "purpose": "邀请专家学者进行学术交流",
                "startDatetime": "2025-12-05T14:00:00",
                "endDatetime": "2025-12-05T16:00:00",
                "status": "CONFIRMED",
                "approvalComment": "同意",
                "rejectionReason": null,
                "code": "RES20251203ABC123",
                "createdAt": "2025-12-03T10:30:00",
                "updatedAt": "2025-12-03T11:00:00"
            },
            {
                "id": 1244,
                "venueId": 1,
                "creatorUserId": 20210002,
                "title": "班级活动",
                "purpose": "组织班级团建活动",
                "startDatetime": "2025-12-10T09:00:00",
                "endDatetime": "2025-12-10T12:00:00",
                "status": "PENDING",
                "approvalComment": null,
                "rejectionReason": null,
                "code": null,
                "createdAt": "2025-12-02T15:20:00",
                "updatedAt": "2025-12-02T15:20:00"
            }
        ]
    }
    ```
*   **字段说明**:
    *   `id`: 预约ID
    *   `venueId`: 场所ID
    *   `creatorUserId`: 创建者用户ID
    *   `title`: 预约标题
    *   `purpose`: 预约目的/事由
    *   `startDatetime`: 开始时间
    *   `endDatetime`: 结束时间
    *   `status`: 预约状态
        - `PENDING`: 待审批
        - `CONFIRMED`: 已确认
        - `REJECTED`: 已驳回
        - `CANCELED`: 已取消
        - `ONGOING`: 进行中
        - `COMPLETED`: 已完成
    *   `approvalComment`: 审批意见（仅审批通过时有值）
    *   `rejectionReason`: 驳回理由（仅驳回时有值）
    *   `code`: 预约码（审批通过后生成的唯一凭证）
    *   `createdAt`: 创建时间
    *   `updatedAt`: 更新时间
*   **Error Response (`403 Forbidden`)**: 无管理员权限

---

## **前端集成建议**

### **1. Excel 生成库推荐**

推荐使用以下 JavaScript 库将 JSON 数据转换为 Excel 文件：

- **xlsx** (SheetJS): 轻量级，功能全面
- **exceljs**: 功能更强大，支持样式和公式

### **2. 示例代码（使用 xlsx）**

```javascript
import * as XLSX from 'xlsx';

// 导出场所统计
const exportVenueStatistics = async () => {
  try {
    const response = await fetch('/api/venues/export/statistics', {
      headers: { 
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) {
      throw new Error('导出失败');
    }
    
    const result = await response.json();
    
    // 转换为工作表
    const ws = XLSX.utils.json_to_sheet(result.data);
    
    // 创建工作簿
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, '场所统计');
    
    // 下载文件
    const fileName = `场所统计_${new Date().toLocaleDateString()}.xlsx`;
    XLSX.writeFile(wb, fileName);
  } catch (error) {
    console.error('导出失败:', error);
    alert('导出失败，请重试');
  }
};

// 导出预约数据
const exportReservations = async () => {
  try {
    const response = await fetch('/api/reservations/export', {
      headers: { 
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) {
      throw new Error('导出失败');
    }
    
    const result = await response.json();
    
    // 转换为工作表
    const ws = XLSX.utils.json_to_sheet(result.data);
    
    // 创建工作簿
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, '预约数据');
    
    // 下载文件
    const fileName = `预约数据_${new Date().toLocaleDateString()}.xlsx`;
    XLSX.writeFile(wb, fileName);
  } catch (error) {
    console.error('导出失败:', error);
    alert('导出失败，请重试');
  }
};
```

### **3. 数据展示建议**

#### **场所统计**
- 适合生成多列对比表格
- 可按热门度评分（`popularityScore`）排序
- 建议字段顺序：场所名称 → 总预约数 → 成功预约数 → 独立用户数 → 热门度评分 → 利用率
- 可以添加图表展示热门场所 TOP 10

#### **预约数据**
- 适合生成详细记录清单
- 可按日期/场所/状态分组
- 建议添加筛选功能（按状态、日期范围、场所等）
- 状态字段可用颜色标识（绿色：已完成，黄色：待审批，红色：已驳回）

### **4. 性能优化建议**

1. **加载提示**
   ```javascript
   const exportWithLoading = async (exportFn) => {
     showLoading('正在导出数据...');
     try {
       await exportFn();
     } finally {
       hideLoading();
     }
   };
   ```

2. **防抖处理**
   ```javascript
   import { debounce } from 'lodash';
   
   const debouncedExport = debounce(exportVenueStatistics, 1000, {
     leading: true,
     trailing: false
   });
   ```

3. **错误处理**
   - 检查用户权限（403 错误提示"无权限"）
   - 网络错误提示用户重试
   - 数据为空时给出友好提示

4. **未来扩展**
   - 可考虑添加日期范围筛选参数
   - 可添加分页导出功能（针对大数据量）
   - 可提供多种格式（CSV、PDF等）

---

## **使用场景**

### **场所统计导出**
- 管理员定期生成场所使用报告
- 分析场所热门程度，优化资源配置
- 向上级汇报场所利用情况

### **预约数据导出**
- 生成月度/年度预约记录归档
- 数据分析和审计
- 导出特定时间段的预约记录进行统计

---

## **注意事项**

1. **权限控制**: 两个导出接口仅限管理员（ADMIN、ROOT）使用，普通用户无权访问
2. **数据量**: 导出所有数据可能较大，建议在前端显示加载进度
3. **浏览器兼容性**: xlsx 库支持所有现代浏览器
4. **文件命名**: 建议在文件名中包含导出时间，避免覆盖
5. **安全性**: 导出的数据可能包含敏感信息，请妥善保管生成的 Excel 文件
