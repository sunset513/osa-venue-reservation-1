# Review 模組快速開始指南

**日期：** 2026-04-16

## 快速導航

### 核心檔案結構

```
src/main/java/tw/edu/ncu/osa/venue_reservation_service/
├── controller/
│   └── ReviewController.java           # 5個 API 端點
├── service/
│   ├── ReviewService.java              # 業務服務介面
│   └── impl/
│       └── ReviewServiceImpl.java       # 業務邏輯實現
├── mapper/
│   ├── ReviewMapper.java               # Mapper 接口
│   └── (BookingMapper.java已擴展)     # 新增 selectPendingConflictingBookings
├── model/
│   ├── dto/
│   │   └── ReviewRequestDTO.java       # 審核請求 DTO
│   ├── vo/
│   │   └── (BookingVO.java複用)        # 回應物件
│   └── entity/
│       └── (Booking.java複用)           # 實體對象

src/main/resources/mapper/
├── ReviewMapper.xml                    # 6 個 SQL 映射
└── (BookingMapper.xml已擴展)          # 新增衝突檢查 SQL

docs/Design/
├── API_Design/
│   └── Review_API_Design.md            # API 設計文檔
├── Service_design/
│   └── Review_Service_design.md        # Service 層設計
├── Module_Design/
│   └── Review_Module_Design.md         # 模組功能說明
├── Mapper_Design/
│   └── Review_Mapper_Design.md         # Mapper 層設計

docs/dev-process/Review_Module/
└── REVIEW_MODULE_COMPLETION_REPORT.md  # 開發完成報告
```

---

## 核心業務流程

### 1. 獲取待審核預約列表

**端點：** `GET /api/reviews/pending?venueId=1&startDate=2026-04-01&endDate=2026-04-30`

**業務邏輯：**
1. 接收查詢參數（場地ID、日期範圍），設定預設值。
2. 調用 `ReviewService.getPendingBookings()`。
3. 在 Mapper 中執行 `selectPendingBookingsByVenueAndDateRange()`，查詢狀態為「審核中(1)」的預約。
4. 返回 `List<BookingVO>` 列表，含設備清單。

**Mapper SQL：** `ReviewMapper.selectPendingBookingsByVenueAndDateRange()`
- LEFT JOIN 場地表、設備表。
- GROUP_CONCAT 聚集設備名稱。
- 過濾狀態為 1 且不為 4（已刪除）的預約。

---

### 2. 查詢預約詳細資訊

**端點：** `GET /api/reviews/bookings/{id}`

**業務邏輯：**
1. 驗證預約 ID 有效性。
2. 調用 `ReviewService.getBookingDetails()`。
3. 查詢預約詳細資訊（含設備清單）。
4. 返回 `BookingVO` 物件。

**Mapper SQL：** `ReviewMapper.selectBookingWithEquipments()`
- 同樣使用 LEFT JOIN 與 GROUP_CONCAT。
- 返回完整的預約資訊。

---

### 3. 審核預約（通過） ⭐ 核心業務

**端點：** `POST /api/reviews/bookings/{id}/approve`

**業務邏輯：**

```
1. 查詢預約原始資料 (BookingMapper.selectById)
   ↓
2. 檢查狀態是否為「審核中(1)」
   ↓ 若非 → 拋出異常
3. 再次進行衝突檢查 (BookingMapper.countConflictingApprovedBookings)
   ↓ 若有衝突 → 拋出異常
4. 更新狀態為「已通過(2)」，使用樂觀鎖 (BookingMapper.updateStatusWithVersion)
   ↓ 若版本不符 → 拋出異常
5. 查詢衝突的「審核中」預約 (BookingMapper.selectPendingConflictingBookings)
   ↓
6. 批量拒絕衝突的申請 (ReviewMapper.batchUpdateStatus)
   ↓
7. 事務提交，操作完成 ✓
```

**關鍵機制：**
- **衝突檢查防呆**：通過前再檢查，防止時段被他人佔用。
- **連鎖拒絕**：自動拒絕其他衝突的「審核中」申請。
- **樂觀鎖**：version 不符時提示「預約案已被他人修改，請重新查詢」。
- **事務控制**：多表操作於事務中進行，失敗時全部回滾。

**Mapper SQL：**
- `countConflictingApprovedBookings`：COUNT 快速判定。
- `updateStatusWithVersion`：帶版本驗證的更新。
- `selectPendingConflictingBookings`：查詢衝突的「審核中」預約。
- `batchUpdateStatus`：批量拒絕。

---

### 4. 更新審核狀態

**端點：** `PUT /api/reviews/bookings/{id}/status`

**請求體：**
```json
{
  "bookingId": 10,
  "status": 3
}
```

**支援的狀態變更：**
- 1 → 3：「審核中」改「拒絕」
- 2 → 3：「已通過」改「拒絕」
- 3 → 1：「拒絕」改「審核中」
- 3 → 2：「拒絕」改「已通過」（需衝突檢查）
- 其他變更：1→0、2→0、0→1、0→2 等

**特殊規則：**
- 從「拒絕(3)」改為「通過(2)」時，執行衝突檢查與連鎖拒絕。
- 其他變更無額外限制。

---

### 5. 軟刪除預約

**端點：** `DELETE /api/reviews/bookings/{id}`

**業務邏輯：**

```
1. 查詢預約是否存在 (BookingMapper.selectById)
   ↓ 若不存在 → 拋出異常
2. 刪除相關設備紀錄 (ReviewMapper.deleteBookingEquipmentsByBookingId)
   ↓
3. 軟刪除預約，狀態改為「已刪除(4)」 (ReviewMapper.deleteSoftBooking)
   ↓
4. 事務提交，操作完成 ✓
```

**軟刪除說明：**
- 不直接刪除資料庫紀錄，而是將狀態標記為 4。
- 保留歷史紀錄用於審計追蹤。
- 已刪除的預約在查詢時自動過濾（`status != 4`）。

---

## 時段衝突檢查（Bitmasking）

**原理：** 使用 24-bit 位元遮罩表示一天的 24 小時時段。

**範例：**
```
時段列表 [8, 9]  →  位元遮罩 = 2^8 + 2^9 = 768 = 0x300

衝突檢查：(time_slots_A & time_slots_B) != 0 → 有重疊
例：
  預約A：時段 [8, 9]  →  mask_A = 0x300 (binary: 001100000000)
  預約B：時段 [9, 10] →  mask_B = 0x600 (binary: 011000000000)
  
  0x300 & 0x600 = 0x200 (非零) → 時段 9 重疊，存在衝突 ✓
```

**SQL 實現：**
```sql
WHERE (time_slots & #{mask}) != 0
```

**複用組件：**
- `BookingMapper.countConflictingApprovedBookings()`：快速計數。
- `BookingMapper.selectPendingConflictingBookings()`：查詢衝突清單。

---

## 樂觀鎖（Optimistic Locking）

**原理：** 使用版本號（version）確保併發更新安全。

**流程：**
```
步驟 1：查詢預約
SELECT ... WHERE id = ?
獲得 version = 5

步驟 2：更新預約
UPDATE bookings 
SET status = 2, version = version + 1 
WHERE id = ? AND version = 5

結果：
- 若 version 符合 → 更新成功，version 變為 6。
- 若 version 不符 → 更新失敗（0 行受影響）→ 拋出異常。
```

**SQL 實現：**
```xml
<update id="updateStatusWithVersion">
    UPDATE bookings
    SET status = #{newStatus},
        version = version + 1
    WHERE id = #{id} AND version = #{oldVersion}
</update>
```

**應用場景：**
- 管理員 A 與 B 同時修改同一筆預約。
- 若 A 先提交更新，version 變為 6。
- B 的更新（基於 version 5）將失敗，系統提示「預約案已被他人修改，請重新查詢」。

---

## 複用組件一覽

### BookingVO（回應物件）

所有 Review API 均使用 BookingVO，無需定義額外的 VO。

**欄位：**
- `id`、`venueName`、`bookingDate`、`slots`、`status`、`createdAt`
- `purpose`、`pCount`、`contactInfo`、`equipments`

**優勢：**
- 前端統一的數據結構，無需適配多種物件。
- 與 Booking 模組數據結構一致，避免不同步。

---

### BookingMapper（基礎操作）

| 方法 | 用途 |
| :--- | :--- |
| `selectById()` | 查詢預約基礎資料 |
| `countConflictingApprovedBookings()` | 衝突計數（快速判定） |
| `selectPendingConflictingBookings()` | 查詢衝突的「審核中」預約 |
| `updateStatusWithVersion()` | 樂觀鎖狀態更新 |

---

## 常見錯誤處理

| 錯誤訊息 | 原因 | 解決方案 |
| :--- | :--- | :--- |
| 「查詢的預約案不存在」 | 預約 ID 無效或不存在 | 檢查預約 ID，重新查詢 |
| 「該預約案已被審核，無法重複審核」 | 預約已被審核（非審核中狀態） | 使用「更新狀態」端點進行狀態變更 |
| 「該時段已被其他已通過之申請佔用」 | 時段衝突 | 修改申請時段或拒絕該申請 |
| 「預約案已被他人修改，請重新查詢」 | 樂觀鎖版本不符 | 重新查詢預約資訊後重試 |
| 「審核狀態值無效」 | 狀態值超出範圍 (0-4) | 檢查請求中的狀態值 |

---

## 日誌追蹤範例

```
【ReviewService】[reviewBooking] 開始審核預約申請，bookingId=10
【ReviewService】[reviewBooking] 成功查詢預約原始資料，bookingId=10, 當前狀態=1
【ReviewService】[reviewBooking] 進行時段衝突檢查，venueId=1, bookingDate=2026-04-10, mask=0x300
【ReviewService】[reviewBooking] 衝突檢查結果：0筆衝突預約
【ReviewService】[reviewBooking] 成功更新預約狀態為「已通過(2)」，bookingId=10
【ReviewService】[reviewBooking] 查詢衝突的其他「審核中」預約
【ReviewService】[reviewBooking] 發現 2 筆衝突的「審核中」預約，準備批量拒絕
【ReviewService】[reviewBooking] 成功批量拒絕衝突的預約
【ReviewService】[reviewBooking] 審核預約申請完成，bookingId=10
```

---

## 與 GlobalExceptionHandler 的集成

所有 Review 模組異常均由 GlobalExceptionHandler 統一處理：

```java
try {
    reviewService.reviewBooking(bookingId);
} catch (Exception e) {
    // GlobalExceptionHandler 自動捕獲
    // 轉換為 Result.error(message) 格式
    // 返回給客戶端
}
```

**回應格式：**
```json
{
  "success": false,
  "message": "該時段已被其他已通過之申請佔用",
  "data": null
}
```

---

## 性能優化建議

1. **索引：** 確保 `idx_venue_date (venue_id, booking_date)` 已建立。
2. **批量操作：** 使用 `batchUpdateStatus()` 批量拒絕，而非逐筆更新。
3. **計數查詢：** 使用 `countConflictingApprovedBookings()` 快速判定衝突。
4. **集合操作：** 避免在 Java 層迴圈，盡量在 SQL 層完成聚集。

---

## 開發下一步

1. **集成測試：** 編寫 JUnit 測試，驗證各個業務流程。
2. **性能測試：** 進行壓測，驗證併發場景下的系統穩定性。
3. **權限控制：** 加入 Spring Security，限制僅管理員可訪問審核端點。
4. **郵件通知：** 審核完成後發送郵件通知申請人。

---

**快速開始指南完成。** 如有問題，請參考完整的設計文檔或開發完成報告。

