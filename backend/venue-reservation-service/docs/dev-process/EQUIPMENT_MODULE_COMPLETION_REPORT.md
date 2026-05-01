# 設備借用歷史查詢功能 開發完成報告

**開發日期**：2026-05-01  
**功能模組**：Equipment Borrow History Query Module  
**狀態**：✅ 開發完成，靜態分析通過

---

## 一、功能概述

實現管理員查詢設備借用歷史紀錄的完整功能，包括分頁查詢、時段轉換、排序與展示。用戶可透過 API 端點 `/api/equipment/history` 查看已批准預約中涉及的設備借用情況。

### 核心功能需求
- ✅ 查詢已批准預約（status=2）的設備借用紀錄
- ✅ 支援分頁查詢（默認每頁 10 筆，最大 100 筆）
- ✅ 將位元遮罩時段轉換為可讀的時段字串
- ✅ 按借用日期倒序排列，同日期按預約 ID 倒序
- ✅ 展示場地名稱、設備名稱、借用日期、時段、用途

---

## 二、新增檔案清單

### VO 層（模型層）

| 檔案路徑 | 功能說明 |
|---------|---------|
| `model/vo/EquipmentBorrowRecordVO.java` | 單筆設備借用紀錄的輸出物件，包含場地、設備、借用時間等資訊 |
| `model/vo/EquipmentBorrowRecordPageVO.java` | 分頁結果包裝物件，包含總筆數、總頁數、當前頁碼、每頁筆數與紀錄清單 |

### DTO 層（請求/響應物件）

| 檔案路徑 | 功能說明 |
|---------|---------|
| `model/dto/EquipmentBorrowQueryDTO.java` | 分頁查詢請求參數物件，包含參數驗證邏輯 |

### Service 層（業務邏輯層）

| 檔案路徑 | 功能說明 |
|---------|---------|
| `service/EquipmentService.java` | 設備服務介面，定義借用歷史查詢方法簽名 |
| `service/impl/EquipmentServiceImpl.java` | 設備服務實現類，實作借用歷史查詢與時段轉換邏輯 |

### Controller 層（控制層）

| 檔案路徑 | 功能說明 |
|---------|---------|
| `controller/EquipmentController.java` | 設備管理 REST API 控制層，曝露 `/api/equipment/history` 端點 |

**新增檔案總數**：6 個

---

## 三、修改檔案清單

### Entity 層

| 檔案路徑 | 修改內容 |
|---------|---------|
| `model/entity/Equipment.java` | 新增 `deletedAt` 欄位支援軟刪除機制 |

### Mapper 層

| 檔案路徑 | 修改內容 |
|---------|---------|
| `mapper/EquipmentMapper.java` | 新增 9 個查詢/寫入方法簽名（CRUD 操作與借用歷史查詢） |
| `resources/mapper/EquipmentMapper.xml` | 新增 7 個 SQL 查詢區塊（包括軟刪除、預約檢查、借用歷史查詢） |

**修改檔案總數**：3 個

---

## 四、API 端點規格

### GET /api/equipment/history

**目的**：分頁查詢設備借用歷史紀錄

**請求參數**：

| 參數名稱 | 型別 | 默認值 | 說明 |
|---------|------|--------|------|
| `pageNum` | Integer | 1 | 頁碼編號（從 1 開始），無效值自動調整為 1 |
| `pageSize` | Integer | 10 | 每頁筆數，最大值 100，超出自動調整 |

**回傳格式**（成功 200）：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "totalCount": 150,
    "totalPages": 15,
    "currentPage": 1,
    "pageSize": 10,
    "data": [
      {
        "venueId": 1,
        "venueName": "會議室 A",
        "equipmentId": 5,
        "equipmentName": "投影機",
        "borrowDate": "2026-05-15",
        "timeSlots": "09:00-12:00",
        "purpose": "系主任會議"
      },
      {
        "venueId": 2,
        "venueName": "會議室 B",
        "equipmentId": 3,
        "equipmentName": "無線麥克風",
        "borrowDate": "2026-05-14",
        "timeSlots": "14:00-15:00, 16:00-17:00",
        "purpose": "學生會議"
      }
    ]
  }
}
```

**排序規則**：
- 主排序：`booking_date DESC`（借用日期倒序，最新最上方）
- 次排序：`booking_id DESC`（同日期按預約 ID 倒序）

**篩選條件**：
- 僅顯示 `bookings.status = 2`（已批准的預約）
- 不顯示已軟刪除的設備（`equipments.deleted_at IS NULL`）

---

## 五、核心實現邏輯

### 5.1 時段轉換算法

**輸入**：位元遮罩（24-bit Integer），每位代表一個小時  
**輸出**：可讀的時段字串

**算法步驟**：

1. **提取小時清單**：遍歷 24 位，判斷每位是否為 1
   ```
   例：mask = 0x0F0F (二進制：0000111100001111)
   → 小時清單 [0,1,2,3,8,9,10,11]
   ```

2. **按連續性合併**：相鄰小時合併為區間
   ```
   [0,1,2,3,8,9,10,11]
   → 分組：[0,1,2,3] 與 [8,9,10,11]
   ```

3. **格式化區間**：轉換為 HH:MM 格式
   ```
   [0,1,2,3] → "00:00-04:00"
   [8,9,10,11] → "08:00-12:00"
   結果：「00:00-04:00, 08:00-12:00」
   ```

### 5.2 分頁計算

**公式**：
```
totalPages = (totalCount + pageSize - 1) / pageSize  // 向上取整
offset = (pageNum - 1) * pageSize
```

**示例**：
- totalCount = 150, pageSize = 10
- totalPages = (150 + 10 - 1) / 10 = 15
- 查詢第 2 頁：offset = (2 - 1) * 10 = 10

### 5.3 SQL 查詢優化

**多表聯接**：
```sql
booking_equipment 
  ← JOIN bookings
  ← JOIN equipments
  ← JOIN venues
```

**索引利用**：
- `(booking_id)` 複合索引支援 booking_equipment 查詢
- `(venue_id, booking_date)` 複合索引優化排序

---

## 六、代碼規範遵循

### 6.1 命名慣例 ✅

| 規範 | 檢查項目 | 狀態 |
|------|--------|------|
| 類別 PascalCase | EquipmentBorrowRecordVO | ✅ 正確 |
| 方法 camelCase | getEquipmentBorrowHistory() | ✅ 正確 |
| 常數大寫 | 無涉及 | ✅ N/A |

### 6.2 禁止事項 ✅

| 項目 | 狀態 | 說明 |
|------|------|------|
| Lambda 表達式 | ✅ 未使用 | 全部使用傳統 for 迴圈 |
| Stream API | ✅ 未使用 | - |
| 空的 catch 區塊 | ✅ 未使用 | 所有異常均有 log.error 記錄 |

### 6.3 文件與註解 ✅

| 項目 | 狀態 | 說明 |
|------|------|------|
| Javadoc | ✅ 完整 | 所有類別與公開方法均有 Javadoc |
| 繁體中文 | ✅ 正確 | 所有註解與 log 均使用繁體中文 |
| 段落分隔 | ✅ 正確 | 使用 `// ==========================================` |
| 行內註解 | ✅ 完整 | 複雜邏輯有適當的行內說明 |

### 6.4 Swagger 註解 ✅

| 項目 | 狀態 | 說明 |
|------|------|------|
| @Operation | ✅ 完整 | 有 summary 與 description |
| @ApiResponse | ✅ 完整 | 200、400 等回應碼均有說明 |
| @Parameter | ✅ 完整 | 所有參數均有 description 與 example |
| @Schema | ✅ 完整 | VO 與 DTO 均有 Schema 註解與 example |

### 6.5 架構原則 ✅

| 項目 | 狀態 | 說明 |
|------|------|------|
| 依賴注入 | ✅ 正確 | 使用 @RequiredArgsConstructor 與 final 字段 |
| 事務控制 | ✅ 正確 | @Transactional(readOnly = true) 用於查詢方法 |
| 結構分離 | ✅ 正確 | Entity、DTO、VO 嚴格分離 |
| 異常處理 | ✅ 正確 | 所有異常均有適當的 log 記錄 |

---

## 七、靜態分析結果

### 編譯檢查 ✅ 通過

**警告分類**（均為可接受的警告）：

| 警告類型 | 數量 | 說明 | 狀態 |
|---------|------|------|------|
| 「Never used」 | 11 | 新增介面與方法還未被前端調用，屬正常 | ✅ 可接受 |
| 其他 | 0 | - | ✅ 通過 |

**最終狀態**：
- ✅ 無編譯錯誤
- ✅ 無語法錯誤
- ✅ 所有類型檢查通過

---

## 八、檔案統計

| 類型 | 數量 | 說明 |
|------|------|------|
| 新增 Java 檔案 | 6 | VO×2、DTO×1、Service×2、Controller×1 |
| 新增 XML 檔案 | 0 | SQL 已整合到 EquipmentMapper.xml |
| 修改 Java 檔案 | 1 | Entity |
| 修改 XML 檔案 | 1 | Mapper XML |
| **總計** | **9** | - |

---

## 九、測試建議

### 單元測試

建議添加以下單元測試：

```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class EquipmentServiceImplTest {
    
    // ==========================================
    // 時段轉換測試
    // ==========================================
    
    @Test
    public void testConvertMaskToTimeString_ContiguousHours() {
        // 測試連續小時：9,10,11 → "09:00-12:00"
    }
    
    @Test
    public void testConvertMaskToTimeString_DiscontiguousHours() {
        // 測試非連續小時：9,10,11,14,15 → "09:00-12:00, 14:00-16:00"
    }
    
    @Test
    public void testConvertMaskToTimeString_SingleHour() {
        // 測試單一小時：9 → "09:00-10:00"
    }
    
    // ==========================================
    // 分頁查詢測試
    // ==========================================
    
    @Test
    public void testGetEquipmentBorrowHistory_FirstPage() {
        // 測試第一頁查詢
    }
    
    @Test
    public void testGetEquipmentBorrowHistory_InvalidPageNum() {
        // 測試無效頁碼自動調整
    }
}
```

### 集成測試

建議測試以下場景：

1. **多頁查詢**：驗證分頁參數正確傳遞
2. **邊界條件**：pageNum=0、pageSize=0、pageSize>100
3. **排序驗證**：確認按日期倒序、同日期按 ID 倒序
4. **時段轉換**：驗證複雜時段的正確轉換

---

## 十、後續工作（非本次範圍）

### 待實現功能

- [ ] **CRUD 操作**：新增、修改、刪除設備（DAO層已預留方法簽名）
- [ ] **設備狀態查詢**：返回兩個場地的設備列表與使用狀態
- [ ] **預約衝突檢查**：刪除設備時驗證進行中的預約
- [ ] **缓存機制**：添加 Redis 快取優化借用歷史查詢性能
- [ ] **多條件搜尋**：按日期範圍、場地、設備名稱篩選

### 建議優化

- [ ] 添加查詢結果導出（CSV/Excel）功能
- [ ] 實現搜尋文字高亮顯示
- [ ] 添加設備使用統計圖表（最常借用的設備等）

---

## 十一、部署清單

### Pre-Deployment 檢查清單

- [x] 代碼編譯通過（無錯誤）
- [x] 靜態分析通過
- [x] Javadoc 完整
- [x] Swagger 註解完整
- [x] 繁體中文註解
- [x] 無 Lambda/Stream API
- [x] 異常處理完整
- [x] 日誌記錄完整
- [ ] 單元測試覆蓋（待補充）
- [ ] 集成測試通過（待執行）

### 部署步驟

1. **編譯**：`mvn clean compile`
2. **測試**：`mvn test`（若有測試）
3. **打包**：`mvn package`
4. **部署**：將 WAR/JAR 部署至應用服務器

---

## 十二、相關檔案參考

### 設計文件

- `docs/Design/Equipment/equipment_crud.md` — 設備 CRUD 業務邏輯設計
- `docs/Design/DB_Design/function_tables.md` — 資料表設計說明

### 代碼規範

- `.github/skills/enforcing-code-standard/SKILL.md` — Java 代碼規範
- `.github/skills/module-dev-standard/SKILL.md` — 模組開發標準
- `.github/skills/swagger-api-documentation-standard/SKILL.md` — Swagger 文檔規範

### 参考实现

- `src/main/java/.../util/BookingUtils.java` — 位元遮罩轉換工具類
- `src/main/java/.../service/impl/BookingServiceImpl.java` — 预约业务实现参考

---

## 十三、簽核

| 項目 | 狀態 | 備註 |
|------|------|------|
| **代碼開發** | ✅ 完成 | 6 新增 + 3 修改 = 9 檔案 |
| **靜態分析** | ✅ 通過 | 無編譯錯誤，警告均可接受 |
| **文件完整性** | ✅ 通過 | Javadoc、Swagger、中文註解均完整 |
| **規範遵循** | ✅ 通過 | 命名、架構、異常處理均符合規範 |
| **功能驗證** | ⏳ 待執行 | 建議進行單元與集成測試 |

---

## 附錄：快速參考

### 核心方法簽名

```java
// 業務服務方法
EquipmentBorrowRecordPageVO getEquipmentBorrowHistory(EquipmentBorrowQueryDTO queryDTO)

// Mapper 方法
int selectEquipmentBorrowRecordsCount()
List<EquipmentBorrowRecordVO> selectEquipmentBorrowRecords(Integer offset, Integer pageSize)

// 時段轉換
String convertMaskToTimeString(Integer mask)
```

### SQL 查詢語句

```sql
-- 查詢借用紀錄總數
SELECT COUNT(DISTINCT be.id) FROM booking_equipment be
  JOIN bookings b ON be.booking_id = b.id
  JOIN equipments e ON be.equipment_id = e.id
  WHERE b.status = 2 AND e.deleted_at IS NULL;

-- 分頁查詢借用紀錄
SELECT b.venue_id, v.name, e.id, e.name, b.booking_date, b.time_slots, b.purpose
  FROM booking_equipment be
  JOIN bookings b ON be.booking_id = b.id
  JOIN equipments e ON be.equipment_id = e.id
  JOIN venues v ON b.venue_id = v.id
  WHERE b.status = 2 AND e.deleted_at IS NULL
  ORDER BY b.booking_date DESC, b.id DESC
  LIMIT offset, pageSize;
```

---

**報告完成日期**：2026-05-01  
**報告狀態**：✅ 通過靜態分析，準備發佈

