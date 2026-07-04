# Equipment Service Design

來源：

- `venue_seed_v2.sql`
- 設備獨立借用功能修改指南

## Service 建議拆分

- `EquipmentService`：設備主檔與場地限制規則。
- `EquipmentBookingService`：使用者設備借用申請。
- `EquipmentReviewService`：管理端設備審核。
- `EquipmentAvailabilityService`：設備可用量與場地規則驗證，可作為共用 service。

## EquipmentService 責任

- 查詢設備主檔與允許場地規則。
- 查詢單一設備詳情。
- 新增設備。
- 修改設備名稱、總數量、說明與借用注意事項。
- 更新設備場地規則。
- 軟刪除設備。
- 恢復已軟刪除設備。

## 設備主檔規則

- 設備名稱不可重複。
- 軟刪除設備仍保留名稱唯一性，建立同名設備時可選擇恢復或回報名稱已存在。
- `totalQuantity` 不可小於 0。
- 降低 `totalQuantity` 時，需確認未來已通過申請中任一小時最大使用量不超過新數量。
- 刪除設備前，需確認今天以後沒有 `status IN (1, 2)` 的設備申請明細引用該設備。
- 更新場地規則時，空陣列代表不限場地；非空陣列代表限制於列出場地。

## EquipmentBookingService 責任

- 建立設備借用申請。
- 查詢自己的設備申請。
- 條件與分頁查詢自己的設備申請。
- 查詢自己的設備申請詳情。
- 修改自己的設備申請。
- 撤回自己的設備申請。
- 查詢設備可用性。
- 從關聯場地預約取得預填資訊。

## 建立設備申請

流程：

1. 從 `UserContext` 取得目前登入者 `userId`。
2. 驗證 `borrowDate`、`slots`、`purpose`、`contactInfo`、`items`。
3. 驗證每個設備存在且 `deleted_at IS NULL`。
4. 合併重複設備項目或拒絕重複 equipmentId。
5. 驗證每個 item `quantity > 0` 且不超過設備總量。
6. 檢查場地限制規則。
7. 若有 `relatedVenueBookingId`，驗證場地預約存在且屬於目前使用者。
8. 若申請含受限制設備，必須有合法關聯場地預約。
9. 寫入 `equipment_bookings`，狀態為 `1=審核中`。
10. 寫入 `equipment_booking_items`。

建立申請時只檢查格式、場地規則與基本數量。是否占用庫存可依現有場地預約政策，只由已通過申請占用；因此多筆 pending 可同時存在。

## 場地規則驗證

若設備沒有 `equipment_venue_rules`：

- 不限制場地。
- 可在 `relatedVenueBookingId = null` 情況下申請。

若設備有 `equipment_venue_rules`：

- `relatedVenueBookingId` 必填。
- 關聯場地預約必須屬於目前使用者。
- 關聯場地預約不可為 `status=4`。
- `borrowDate` 必須等於 `bookings.booking_date`。
- 設備借用時段必須是場地預約時段的子集合：

```java
(equipmentMask & venueBookingMask) == equipmentMask
```

- `bookings.venue_id` 必須存在於該設備允許場地清單。

審核核准時應再次執行同樣規則。較保守策略是受限制設備核准時要求關聯場地預約 `status=2`。

## 可用量檢查

只有 `status=2` 的設備申請占用庫存。

對每一筆申請明細與每一個 requested hour：

```text
已核准用量 + 本次申請數量 <= total_quantity
```

可用量查詢 response 應回傳每個設備在所有查詢時段中的最低可用量。

修改既有申請或審核既有申請時，可用量計算需排除該申請本身。

## 修改設備申請

規則：

- 只能修改自己的設備申請。
- 只有 `status=1` 或 `status=2` 可修改。
- 修改時需帶 `version`。
- 修改成功後狀態重置為 `1=審核中`。
- 修改時重新驗證設備、數量、場地規則。
- 使用樂觀鎖更新主表。
- 明細採刪除後重建。

## 撤回設備申請

規則：

- 只能撤回自己的設備申請。
- 只有 `status=1` 或 `status=2` 可撤回。
- 撤回時需使用 version 或查詢到的當前 version 做樂觀鎖。
- 狀態改為 `0=已撤回`。

## EquipmentReviewService 責任

- 管理端查詢設備申請列表。
- 管理端查看設備申請詳情。
- 核准設備申請。
- 拒絕設備申請。
- 刪除設備申請。

## 核准設備申請

流程：

1. 驗證申請存在且不是 `status=4`。
2. 驗證 request `version` 與資料庫 version。
3. 重新驗證設備未停用。
4. 重新驗證場地規則。
5. 重新逐小時檢查設備可用量。
6. 寫入 `status=2`。
7. 寫入 `reviewed_by`、`reviewed_at`。
8. 清空或保留 `reject_reason` 需明確決定，建議核准時清空。
9. `version = version + 1`。

核准時需防止併發超借。多設備申請建議依 `equipment_id ASC` 固定順序鎖定或查詢，避免死鎖。

## 拒絕設備申請

流程：

1. 驗證申請存在且不是 `status=4`。
2. 驗證 `reason`。
3. 使用樂觀鎖更新狀態為 `3=已拒絕`。
4. 寫入 `reviewed_by`、`reviewed_at`、`reject_reason`。
5. `version = version + 1`。

## 刪除設備申請

管理員刪除設備申請採軟刪除：

- 狀態改為 `4=已刪除`。
- 不刪除主表。
- 明細是否保留可查詢歷史，建議保留。
- 一般列表預設排除 `status=4`。

## 與場地預約脫鉤

場地預約 service 需移除設備寫入：

- 建立場地預約不再接收 `equipmentIds`。
- 修改場地預約不再刪除/重建 `booking_equipment`。
- 場地預約 VO 不再回傳 `equipments`。
- 場地審核不再顯示設備清單。

設備借用頁需要預填時，透過新增的使用者場地預約詳情 API 查資料，不從場地預約建立流程直接寫設備資料。
