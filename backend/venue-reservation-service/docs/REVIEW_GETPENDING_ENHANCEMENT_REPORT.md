# Review 模組 getPendingBookings 功能增強報告

**日期：** 2026-04-16  
**版本：** 1.0  
**狀態：** 已完成

---

## 一、 修改概述

根據業務需求，對 Review 模組的 `getPendingBookings` 方法進行功能增強，使其更加通用和靈活。主要改動包括：

1. **支援場地 ID 過濾**：前端可傳遞特定場地的 ID（已支援，現改進為更加明確）
2. **支援狀態動態查詢**：新增 `status` 參數，讓前端自行決定要查詢的預約狀態
   - 預設行為：查詢全部狀態（除了已刪除的 status=4）
   - 指定狀態：查詢特定狀態（0-3）
3. **文檔同步更新**：同步更新 3 份設計文檔

---

## 二、 詳細修改內容

### 2.1 Controller 層 (`ReviewController.java`)

**修改文件**：
- `src/main/java/tw/edu/ncu/osa/venue_reservation_service/controller/ReviewController.java`

**修改內容**：

1. **方法簽名變更**
   - 新增 `status` 參數（可選）
   - 參數類型：`Integer`

2. **接口文檔更新**
   - 更新 `@Operation` 描述，說明新的狀態過濾功能
   - 新增 `status` 參數的詳細說明
   - 更新示例，展示多個狀態的預約返回

3. **日誌更新**
   - 新增 status 參數到日誌輸出

**修改後簽名**：
```java
public Result<List<BookingVO>> getPendingBookings(
    Long venueId,
    LocalDate startDate,
    LocalDate endDate,
    Integer status)
```

### 2.2 Service 層

#### ReviewService 接口 (`ReviewService.java`)

**修改文件**：
- `src/main/java/tw/edu/ncu/osa/venue_reservation_service/service/ReviewService.java`

**修改內容**：

1. **方法簽名變更**
   ```java
   // 舊簽名
   List<BookingVO> getPendingBookings(Long venueId, LocalDate startDate, LocalDate endDate);
   
   // 新簽名
   List<BookingVO> getPendingBookings(Long venueId, LocalDate startDate, LocalDate endDate, Integer status);
   ```

2. **JavaDoc 更新**
   - 新增 status 參數說明
   - 說明默認查詢行為

#### ReviewServiceImpl 實現 (`ReviewServiceImpl.java`)

**修改文件**：
- `src/main/java/tw/edu/ncu/osa/venue_reservation_service/service/impl/ReviewServiceImpl.java`

**修改內容**：

1. **方法實現更新**
   - 新增狀態參數驗證邏輯
   - 驗證狀態值必須在 0-3 之間或為 null
   - 添加詳細的日誌記錄

2. **業務邏輯調整**
   ```java
   // 驗證狀態值有效性
   if (status != null && (status < 0 || status > 3)) {
       log.warn("【ReviewService】[getPendingBookings] 無效的狀態值，status={}", status);
       throw new RuntimeException("狀態值無效，請輸入 0-3 或不填");
   }
   ```

3. **Mapper 方法調用變更**
   - 從 `selectPendingBookingsByVenueAndDateRange` 改為 `selectBookingsByVenueAndDateRange`
   - 新方法支援狀態參數

### 2.3 Mapper 層

#### ReviewMapper 接口 (`ReviewMapper.java`)

**修改文件**：
- `src/main/java/tw/edu/ncu/osa/venue_reservation_service/mapper/ReviewMapper.java`

**修改內容**：

1. **新增方法**
   ```java
   /**
    * 根據場地與日期範圍查詢預約列表
    * @param venueId 場地 ID
    * @param startDate 開始日期
    * @param endDate 結束日期
    * @param status 預約狀態 (0:未提交, 1:審核中, 2:已通過, 3:已拒絕，不填則查詢全部除了已刪除的)
    * @return 預約列表
    */
   List<BookingVO> selectBookingsByVenueAndDateRange(
       @Param("venueId") Long venueId,
       @Param("startDate") LocalDate startDate,
       @Param("endDate") LocalDate endDate,
       @Param("status") Integer status
   );
   ```

2. **保留舊方法**
   - 標記 `selectPendingBookingsByVenueAndDateRange` 為 `@Deprecated`
   - 保持向後相容性

#### ReviewMapper.xml (`ReviewMapper.xml`)

**修改文件**：
- `src/main/resources/mapper/ReviewMapper.xml`

**修改內容**：

1. **新增 SQL 查詢**
   ```xml
   <select id="selectBookingsByVenueAndDateRange" resultMap="bookingVOResultMap">
       SELECT
           b.id,
           v.name AS venue_name,
           b.booking_date,
           b.time_slots AS slots,
           b.status,
           b.created_at,
           b.purpose,
           b.p_count,
           b.contact_info,
           GROUP_CONCAT(e.name SEPARATOR ',') AS equipments
       FROM bookings b
       LEFT JOIN venues v ON b.venue_id = v.id
       LEFT JOIN booking_equipment be ON b.id = be.booking_id
       LEFT JOIN equipments e ON be.equipment_id = e.id
       WHERE b.venue_id = #{venueId}
       AND b.booking_date BETWEEN #{startDate} AND #{endDate}
       AND b.status != 4
       <if test="status != null">
           AND b.status = #{status}
       </if>
       GROUP BY b.id
       ORDER BY b.created_at ASC
   </select>
   ```

2. **動態 SQL 特性**
   - 使用 MyBatis 的 `<if>` 標籤實現動態 SQL
   - 當 status 為 null 時不添加狀態過濾條件
   - 當 status 不為 null 時只查詢指定狀態

3. **保留舊查詢**
   - 保留原有的 `selectPendingBookingsByVenueAndDateRange` 以維持向後相容性

---

## 三、 文檔更新

### 3.1 Review_API_Design.md

**修改位置**：`docs/Design/API_Design/Review_API_Design.md`

**修改內容**：

1. **終端點描述更新**
   - 標題從「獲取待審核預約列表」改為「獲取預約列表」
   - 功能描述更新為支援靈活過濾

2. **查詢參數文檔**
   - 新增 `status` 參數說明表格項目
   - 詳細說明各狀態值含義

3. **示例響應**
   - 更新示例返回多個不同狀態的預約案

### 3.2 Review_Service_design.md

**修改位置**：`docs/Design/Service_design/Review_Service_design.md`

**修改內容**：

1. **方法說明更新**
   - 方法名保持不變但功能描述更新
   - 新增 status 參數說明
   - 詳細解釋默認查詢行為

2. **參數列表**
   - 補充 status 參數的詳細說明

### 3.3 Review_Module_Design.md

**修改位置**：`docs/Design/Module_Design/Review_Module_Design.md`

**修改內容**：

1. **流程圖更新（2.1 節）**
   - 更新狀態篩選邏輯說明
   - 補充條件判斷的詳細內容

2. **核心方法設計（6.1 節）**
   - 更新 `getPendingBookings` 方法的參數說明
   - 更新業務邏輯描述
   - 補充異常處理說明

---

## 四、 API 使用示例

### 4.1 查詢所有狀態的預約（默認行為）

```bash
GET /api/reviews/pending?venueId=1&startDate=2026-04-01&endDate=2026-04-30
```

**返回結果**：包含所有狀態 (0, 1, 2, 3) 的預約案，除了已刪除的 (status=4)

### 4.2 查詢審核中的預約

```bash
GET /api/reviews/pending?venueId=1&startDate=2026-04-01&endDate=2026-04-30&status=1
```

**返回結果**：只返回狀態為 1 (審核中) 的預約案

### 4.3 查詢已通過的預約

```bash
GET /api/reviews/pending?venueId=1&startDate=2026-04-01&endDate=2026-04-30&status=2
```

**返回結果**：只返回狀態為 2 (已通過) 的預約案

### 4.4 查詢已拒絕的預約

```bash
GET /api/reviews/pending?venueId=1&startDate=2026-04-01&endDate=2026-04-30&status=3
```

**返回結果**：只返回狀態為 3 (已拒絕) 的預約案

---

## 五、 向後相容性

### 5.1 保留舊方法

為確保向後相容性，以下舊方法已標記為 `@Deprecated` 但仍可使用：

- `ReviewMapper#selectPendingBookingsByVenueAndDateRange`

### 5.2 遷移建議

建議將現有使用舊方法的代碼逐步遷移至新方法，但目前不強制要求。

---

## 六、 測試建議

### 6.1 單元測試

1. **參數驗證測試**
   - 測試 status 參數為 null（應返回全部狀態）
   - 測試 status 參數為 0-3（應只返回該狀態）
   - 測試 status 參數為 -1 或 4（應拋出異常）

2. **查詢結果測試**
   - 測試不同 status 值下的返回結果
   - 測試日期範圍過濾
   - 測試場地 ID 過濾

### 6.2 集成測試

1. 完整的 API 調用測試
2. 邊界情況測試（空結果、多條結果等）
3. 性能測試（大量數據情況下的查詢性能）

---

## 七、 修改清單

| 文件 | 修改類型 | 備註 |
| :--- | :--- | :--- |
| ReviewController.java | 修改 | 新增 status 參數 |
| ReviewService.java | 修改 | 更新方法簽名 |
| ReviewServiceImpl.java | 修改 | 實現新邏輯 |
| ReviewMapper.java | 修改 | 新增方法，舊方法標記為 @Deprecated |
| ReviewMapper.xml | 修改 | 新增 SQL 查詢，保留舊查詢 |
| Review_API_Design.md | 修改 | 更新文檔描述 |
| Review_Service_design.md | 修改 | 更新方法說明 |
| Review_Module_Design.md | 修改 | 更新流程和設計細節 |

---

## 八、 總結

本次修改成功將 `getPendingBookings` 方法從「查詢待審核預約」的單一功能，升級為「根據多維度條件動態查詢預約」的通用功能。通過新增 `status` 參數，前端現可：

1. **靈活地查詢不同狀態的預約案**
   - 支援查詢單一狀態
   - 支援查詢全部狀態
   - 自動排除已刪除案件

2. **保持系統穩定**
   - 完整的參數驗證機制
   - 詳細的日誌記錄
   - 向後相容性保障

3. **簡化前端邏輯**
   - 統一的 API 接口
   - 靈活的查詢參數
   - 一致的返回格式

所有代碼修改均遵循既有的設計規範和命名約定，保證代碼質量和可維護性。


