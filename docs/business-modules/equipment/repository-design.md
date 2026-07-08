# Equipment Repository Design

來源：

- `venue_seed_v2.sql`
- 設備獨立借用功能修改指南

## Repository / Mapper 現況

目前後端主要使用兩個 mapper 承擔 equipment 模組資料存取：

- `EquipmentMapper`：設備主檔與場地規則。
- `EquipmentBookingMapper`：使用者設備借用申請、管理端設備審核查詢、狀態更新與可用量查詢。

文件仍以責任區塊描述 SQL 邊界；若後續規模擴大，可再拆出獨立 `EquipmentReviewMapper`。

## EquipmentMapper

### 責任

- 查詢所有可借設備。
- 依 ID 查詢設備主檔。
- 依名稱查詢設備，用於唯一性檢查。
- 新增、修改、軟刪除、恢復設備。
- 查詢設備允許場地規則。
- 整批更新設備允許場地規則。
- 檢查設備是否可刪除。
- 檢查降低總數量是否安全。
- 查詢指定日期/小時所有設備的 active booking 狀態列，供 `/api/equipments/status` 使用。

### 主要 SQL

查詢設備與規則：

```sql
SELECT e.*
FROM equipments e
WHERE e.deleted_at IS NULL
ORDER BY e.id ASC;
```

```sql
SELECT r.equipment_id, r.venue_id, v.name AS venue_name, r.rule_note
FROM equipment_venue_rules r
JOIN venues v ON v.id = r.venue_id
WHERE r.equipment_id IN (...);
```

軟刪除檢查：

```sql
SELECT COUNT(*)
FROM equipment_booking_items ebi
JOIN equipment_bookings eb
  ON eb.id = ebi.equipment_booking_id
WHERE ebi.equipment_id = #{equipmentId}
  AND eb.status IN (1, 2)
  AND eb.borrow_date >= CURRENT_DATE;
```

降低數量檢查需查未來已通過申請，在每個小時的最大使用量。

## EquipmentBookingMapper

### 責任

- 新增 `equipment_bookings` 主表。
- 新增、刪除、重建 `equipment_booking_items`。
- 查詢目前使用者的設備申請。
- 條件與分頁查詢設備申請。
- 查詢單筆設備申請詳情。
- 修改設備申請。
- 撤回設備申請。
- 查詢關聯場地預約。
- 查詢設備場地規則。
- 查詢指定設備在指定日期與小時的已核准用量。

### 建立流程 SQL

新增主申請：

```sql
INSERT INTO equipment_bookings (
  user_id, borrow_date, time_slots, status, purpose, contact_info,
  related_venue_booking_id, version
)
VALUES (..., 1, ..., 1);
```

新增明細：

```sql
INSERT INTO equipment_booking_items (
  equipment_booking_id, equipment_id, quantity
)
VALUES (...);
```

修改申請時建議：

- 使用 `id + version` 更新主表。
- 刪除舊明細。
- 重新寫入新明細。

### 可用量查詢

逐小時計算已通過用量：

```sql
SELECT COALESCE(SUM(ebi.quantity), 0)
FROM equipment_booking_items ebi
JOIN equipment_bookings eb
  ON eb.id = ebi.equipment_booking_id
WHERE ebi.equipment_id = #{equipmentId}
  AND eb.borrow_date = #{borrowDate}
  AND eb.status = 2
  AND ((eb.time_slots >> #{hour}) & 1) = 1
  AND (#{excludeEquipmentBookingId} IS NULL OR eb.id != #{excludeEquipmentBookingId});
```

每個 requested slot 都要查或以 SQL 聚合後在 service 逐小時比對，避免誤把不同小時的數量加總。

## Equipment Review Query And Status SQL

### 責任

- 管理端依條件查詢設備申請，目前由 `EquipmentBookingMapper.countReviewBookings` 與 `selectReviewBookings` 實作。
- 查詢設備申請詳情與明細。
- 使用樂觀鎖更新審核狀態。
- 查詢核准後可能受影響的 pending 設備申請。
- 批次拒絕已不可核准的 pending 設備申請。
- 寫入 `reviewed_by`、`reviewed_at`；系統不保存拒絕原因。

### 查詢條件

管理端列表建議支援：

- `status`
- `equipmentId`
- `userId`
- `relatedVenueId`
- `startDate`
- `endDate`
- `pageNo`
- `pageSize`

列表應排除 `status=4`，除非明確查詢刪除資料。

目前額外支援：

- `relatedVenueBookingId`：查詢指定場地預約關聯的設備申請。
- `standaloneOnly`：只查詢沒有關聯場地預約的設備申請。

狀態更新目前使用同一個 mapper 方法：

```sql
UPDATE equipment_bookings
SET status = #{newStatus},
    reviewed_by = #{reviewedBy},
    reviewed_at = NOW(),
    version = version + 1
WHERE id = #{id}
  AND version = #{oldVersion};
```

核准後自動拒絕流程需先查候選 pending 申請，再由 service 逐筆做 availability 判斷：

```sql
SELECT DISTINCT eb.*
FROM equipment_bookings eb
JOIN equipment_booking_items ebi
  ON ebi.equipment_booking_id = eb.id
WHERE eb.status = 1
  AND eb.id != #{approvedBookingId}
  AND eb.borrow_date = #{borrowDate}
  AND (eb.time_slots & #{approvedTimeSlots}) != 0
  AND ebi.equipment_id IN (...)
```

不可直接把所有候選申請拒絕，因為設備具有數量；只有 availability 檢查失敗的申請才會被自動改為 `status=3`。

## 場地預約輔助查詢

設備借用需要查關聯場地預約：

- `bookings.id`
- `bookings.user_id`
- `bookings.venue_id`
- `venues.name`
- `bookings.booking_date`
- `bookings.time_slots`
- `bookings.status`
- `bookings.purpose`
- `bookings.contact_info`

目前設備申請驗證直接透過 `BookingMapper.selectById` 查詢關聯場地預約，並透過 `VenueMapper.selectVenueById` 補場地名稱。

## Transaction

以下操作必須在交易中完成：

- 建立設備申請與明細。
- 修改設備申請與明細。
- 核准設備申請。
- 拒絕設備申請。
- 審核端更新設備申請狀態。
- 更新設備主檔與場地規則。

核准多設備申請時，需避免併發超借。建議固定依 `equipment_id ASC` 查詢或鎖定相關設備資料，再逐項做可用量檢查。

## 舊 Mapper 清理

切換至新 schema 後，需移除或重寫舊 SQL：

- `EquipmentMapper` 中所有 `venue_equipment_map` 查詢與寫入。
- `EquipmentMapper` 中所有 `booking_equipment` 歷史與使用中查詢。
- `BookingMapper` 中 `insertBookingEquipment` 與 `deleteBookingEquipmentByBookingId`。
- `ReviewMapper` 中 booking 的設備 join。
- `VenueMapper` 中場地設備 collection join。
