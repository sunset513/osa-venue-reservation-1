# Equipment Service Design

來源：

- `venue_seed_v2.sql`
- 設備獨立借用功能修改指南

## Service 建議拆分

- `EquipmentService`：設備主檔與場地限制規則。
- `EquipmentBookingService`：使用者設備借用申請。
- `EquipmentReviewService`：管理端設備審核。
- `EquipmentBookingSupport`：目前承擔設備可用量、場地規則驗證與 VO 轉換等共用邏輯。

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
- `totalQuantity` 不可小於 1。
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
- 前端目前不需帶 `version`；service 會先查詢目前申請，再用資料庫目前版本進行樂觀鎖更新。
- 修改成功後狀態重置為 `1=審核中`。
- 修改時重新驗證設備、數量、場地規則。
- 使用樂觀鎖更新主表。
- 明細採刪除後重建。

## 撤回設備申請

規則：

- 只能撤回自己的設備申請。
- 只有 `status=1` 或 `status=2` 可撤回。
- 撤回時使用查詢到的當前 version 做樂觀鎖。
- 狀態改為 `0=已撤回`。

## EquipmentReviewService 責任

- 管理端查詢設備申請列表。
- 管理端查看設備申請詳情。
- 管理端查詢指定場地預約關聯的設備申請。
- 管理端查詢單獨設備待審數量。
- 核准設備申請。
- 拒絕設備申請。
- 彈性更新審核狀態為 `1=審核中`、`2=已通過`、`3=已拒絕`。

## 核准設備申請

流程：

1. 驗證申請存在且不是 `status=4`。
2. 已撤回的申請 `status=0` 不可由審核端重新啟用。
3. 重新驗證設備未停用。
4. 重新驗證場地規則。
5. 重新逐小時檢查設備可用量。
6. 寫入 `status=2`。
7. 寫入 `reviewed_by`、`reviewed_at`。
8. 以查詢到的當前 version 做樂觀鎖，成功後 `version = version + 1`。

核准時需防止併發超借。多設備申請建議依 `equipment_id ASC` 固定順序鎖定或查詢，避免死鎖。

## 拒絕設備申請

流程：

1. 驗證申請存在且不是 `status=4`。
2. 已撤回的申請 `status=0` 不可由審核端重新啟用。
3. 使用樂觀鎖更新狀態為 `3=已拒絕`。
4. 寫入 `reviewed_by`、`reviewed_at`。
5. 不保存拒絕原因；承辦人如需說明，於系統外通知使用者。
6. `version = version + 1`。

## 更新設備審核狀態

`PUT /api/equipment-reviews/{id}/status` 統一處理審核端彈性狀態切換：

- 允許目標狀態：`1=審核中`、`2=已通過`、`3=已拒絕`。
- 目標為 `2=已通過` 時，必須重新檢查設備可用量與場地規則。
- 已撤回申請 `status=0` 不可由審核端重新啟用。
- 每次成功更新都寫入 `reviewed_by`、`reviewed_at` 並遞增 `version`。

## 刪除設備申請

資料模型保留 `status=4` 表示系統刪除，但目前後端尚未提供 equipment review delete endpoint。若後續新增管理端刪除設備申請，建議採軟刪除：

- 狀態改為 `4=已刪除`。
- 不刪除主表。
- 明細是否保留可查詢歷史，建議保留。
- 一般列表預設排除 `status=4`。

## 與場地預約整合與脫鉤

一般場地預約建立與修改仍不直接寫入設備資料；設備與場地預約的整合改由明確的合併建立 API 處理：

- `POST /api/bookings`：只建立場地預約。
- `POST /api/bookings/with-equipments`：在同一交易中建立場地預約與關聯設備借用申請。
- 修改場地預約時不修改關聯設備申請，因為目前沒有合併更新 API。
- 場地 review modal 會透過 `GET /api/equipment-reviews/by-venue-booking/{bookingId}` 讀取關聯設備申請，並可一併審核設備狀態。

舊的 `booking_equipment` 不再作為設備借用資料來源。
