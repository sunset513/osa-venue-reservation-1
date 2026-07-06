# Equipment Frontend Interface Overview

本文說明目前 equipment 模組涉及的前端介面、組件、API 呼叫與元件關聯，供前端開發者接手與後續擴充時參考。

## Route Map

| Route name | Path | Component | Purpose |
| --- | --- | --- | --- |
| `EquipmentBorrowForm` | `/equipment-borrow` | `frontend/src/views/EquipmentBorrowForm.vue` | 建立單獨設備借用申請。 |
| `EquipmentBorrowHistory` | `/equipment-history` | `frontend/src/views/EquipmentBorrowHistory.vue` | 查看我的設備借用紀錄，可依設備 ID 篩選。 |
| `EquipmentStatus` | `/equipment-status` | `frontend/src/views/EquipmentStatus.vue` | 查看指定日期/小時各設備目前是否出借與 active booking 資訊。 |
| `ReviewCalendar` | `/review` | `frontend/src/views/ReviewCalendar.vue` | 審核工作台；包含場地預約審核與設備借用審核。 |

## Shared API Client

檔案：`frontend/src/api/equipment.js`

| Function | API | Used by | Purpose |
| --- | --- | --- | --- |
| `listEquipments` | `GET /api/equipments` | `BookingModal.vue`, `EquipmentBorrowForm.vue` | 取得設備主檔與場地限制規則。 |
| `getEquipmentStatuses` | `GET /api/equipments/status` | `EquipmentStatus.vue` | 查詢指定日期/小時所有設備狀態與 active booking。 |
| `createEquipmentBooking` | `POST /api/equipment-bookings` | `EquipmentBorrowForm.vue` | 建立單獨設備借用申請。 |
| `queryMyEquipmentBookings` | `POST /api/equipment-bookings/query` | `EquipmentBorrowHistory.vue` | 查詢自己的設備借用紀錄。 |
| `updateEquipmentBooking` | `PUT /api/equipment-bookings/{id}` | 目前尚未接到頁面 | 修改自己的設備借用申請。 |
| `withdrawEquipmentBooking` | `PUT /api/equipment-bookings/{id}/withdraw` | 目前尚未接到頁面 | 撤回自己的設備借用申請。 |
| `checkEquipmentAvailability` | `POST /api/equipment-bookings/availability` | 目前尚未接到頁面 | 預先檢查設備可用量與場地規則。 |
| `queryEquipmentReviews` | `POST /api/equipment-reviews/query` | `ReviewCalendar.vue` | 審核端查詢所有設備申請。 |
| `getEquipmentReviewsByVenueBooking` | `GET /api/equipment-reviews/by-venue-booking/{bookingId}` | `ReviewCalendar.vue` | 場地審核詳情中查詢關聯設備申請。 |
| `getStandaloneEquipmentPendingCount` | `GET /api/equipment-reviews/standalone/pending-count` | `ReviewCalendar.vue` | Review 切換按鈕 badge 顯示單獨設備待審數。 |
| `approveEquipmentReview` | `PUT /api/equipment-reviews/{id}/approve` | 相容保留，目前主要 UI 改用 status API | 核准設備申請。 |
| `rejectEquipmentReview` | `PUT /api/equipment-reviews/{id}/reject` | 相容保留，目前主要 UI 改用 status API | 拒絕設備申請，不送拒絕原因。 |
| `updateEquipmentReviewStatus` | `PUT /api/equipment-reviews/{id}/status` | `ReviewCalendar.vue` | 審核端彈性切換設備申請狀態。 |

場地合併建立 API 位於 `frontend/src/api/booking.js`：

| Function | API | Used by | Purpose |
| --- | --- | --- | --- |
| `createBookingWithEquipments` | `POST /api/bookings/with-equipments` | `BookingModal.vue` | 同一交易中建立場地預約與關聯設備借用申請。 |

## Shared Helpers

檔案：`frontend/src/utils/equipment.js`

| Helper | Purpose |
| --- | --- |
| `normalizeEquipmentMasters` | 將設備主檔轉為前端穩定格式，包含 `allowedVenues` 與 `venueRestricted`。 |
| `isEquipmentAllowedForVenue` | 依設備場地規則過濾目前場地可選設備；後端仍會在送出時再次驗證。 |
| `buildEquipmentBookingItems` | 將前端選擇與數量轉為後端 `items` payload。 |
| `normalizeEquipmentBookingPage` | 正規化設備借用分頁結果，供我的紀錄與審核列表使用。 |
| `normalizeEquipmentBooking` | 正規化單筆設備申請，產出 `itemSummary`、`contact`、`timeRange` 等 UI 欄位。 |
| `normalizeEquipmentStatuses` | 正規化設備狀態資料與 active booking 聯絡資訊。 |
| `getEquipmentBookingStatusMeta` | 設備申請狀態文字與樣式 class。 |
| `getEquipmentStatusMeta` | 設備是否使用中的文字與樣式 class。 |

## Interface Details

### `VenueSelector.vue`

功能：

- 在使用者進入系統後的場地選擇頁加入「單獨設備借用」入口。
- 點擊後導向 `EquipmentBorrowForm`。

API：無直接 API 呼叫。

關聯：

- 路由：`EquipmentBorrowForm`
- 使用者可在不選擇場地的情況下建立單獨設備借用申請。

### `BookingModal.vue`

功能：

- 場地預約建立 modal。
- `mode === "create"` 時載入可用設備，讓使用者在場地預約表單中選擇設備與數量。
- 若有選設備，送出 `createBookingWithEquipments`；若未選設備，維持原本 `createBooking`。
- `mode === "edit"` 時只修改場地預約，不修改關聯設備申請。

API：

- `GET /api/equipments`
- `POST /api/bookings`
- `POST /api/bookings/with-equipments`
- `PUT /api/bookings/{id}`

關聯：

- 使用 `isEquipmentAllowedForVenue` 依場地規則過濾可選設備。
- 使用 `buildEquipmentBookingItems` 建立合併建立 payload。
- 與 `VenueCalendar.vue` 搭配，用於新增、編輯、撤回場地預約。

目前限制：

- 場地預約編輯不會同步修改設備申請，因為後端目前沒有合併更新 API。
- 設備可用量不在前端即時計算；送出時由後端驗證。

### `EquipmentBorrowForm.vue`

功能：

- 建立單獨設備借用申請。
- 使用者選擇日期、時段、設備、數量、用途與聯絡資訊。
- 送出後導向 `EquipmentBorrowHistory`。

API：

- `GET /api/equipments`
- `POST /api/equipment-bookings`

關聯：

- 使用 `normalizeEquipmentMasters` 呈現設備清單。
- 使用 `buildEquipmentBookingItems` 組成 `items` payload。
- 成功後導向 `EquipmentBorrowHistory`。

目前限制：

- 此頁目前固定建立 `relatedVenueBookingId: null` 的單獨設備借用。
- 若使用者選到受場地限制設備，後端會拒絕；前端尚未提供綁定既有場地預約的操作。

### `EquipmentBorrowHistory.vue`

功能：

- 顯示目前使用者自己的設備借用紀錄。
- 支援從設備狀態頁帶入 `equipmentId`、`equipmentName` query 進行篩選。
- 顯示設備明細、借用日期、時段、狀態、用途與關聯場地。

API：

- `POST /api/equipment-bookings/query`

關聯：

- 由 `NavBar.vue` 的「我的設備借用紀錄」進入。
- 由 `EquipmentStatus.vue` 的「查看借用紀錄」帶設備篩選進入。
- 使用 `normalizeEquipmentBookingPage` 與 `getEquipmentBookingStatusMeta`。

目前限制：

- 此頁目前只讀，尚未提供修改或撤回設備申請的 UI。

### `EquipmentStatus.vue`

功能：

- 查詢指定日期與小時所有設備狀態。
- 顯示設備總量、借出數、可用數與使用中/閒置。
- 若設備目前有 active booking，可展開查看借用申請 ID、用途、日期時段、數量、申請人、聯絡電話與關聯場地。
- 可導向 `EquipmentBorrowForm` 新增設備借用。
- 可導向 `EquipmentBorrowHistory` 並帶入設備 ID 篩選。

API：

- `GET /api/equipments/status?date=YYYY-MM-DD&hour=H`

關聯：

- 使用 `normalizeEquipmentStatuses` 與 `getEquipmentStatusMeta`。
- 使用 `formatSlotGroupsAsTimeRange` 顯示時段。

目前限制：

- 頁面已移除總設備數、使用中、可用總量三張總覽卡。
- 此頁是狀態檢視，不負責設備主檔 CRUD。

### `ReviewCalendar.vue`

功能：

- 審核工作台，提供「場地預約 / 設備借用」切換。
- 場地預約模式維持原本月曆與列表審核流程。
- 開啟場地預約詳情時，同步查詢該場地預約關聯的設備申請，並傳給 `ReviewBookingModal.vue` 顯示與審核。
- 設備借用模式查詢所有設備申請，包含單獨設備借用與關聯場地設備借用。
- 「設備借用」切換按鈕 badge 顯示單獨設備待審數。
- 設備申請可在 `pending / approved / rejected` 間切換；核准會由後端重新檢查數量與場地規則。

API：

- `GET /api/equipment-reviews/standalone/pending-count`
- `POST /api/equipment-reviews/query`
- `GET /api/equipment-reviews/by-venue-booking/{bookingId}`
- `PUT /api/equipment-reviews/{id}/status`
- 原本場地審核 API：`GET /api/reviews/pending`、`GET /api/reviews/bookings/{id}`、`POST /api/reviews/bookings/{id}/approve`、`PUT /api/reviews/bookings/{id}/status`

關聯：

- 使用 `ReviewBookingModal.vue` 顯示場地預約詳情與關聯設備申請。
- 使用 `normalizeEquipmentBookingPage`、`normalizeEquipmentBooking` 與 `getEquipmentBookingStatusMeta`。

目前限制：

- 設備借用列表目前一次查 `pageSize: 100`，尚未做完整分頁 UI。
- 設備審核不收拒絕原因；承辦人如需說明，於系統外通知使用者。

### `ReviewBookingModal.vue`

功能：

- 場地預約審核詳情 drawer。
- 顯示場地申請資訊、申請人聯絡資料與關聯設備申請。
- 關聯設備申請顯示設備明細、借用時段、用途、借用者聯絡方式與審核狀態。
- 依設備申請目前狀態顯示可切換動作：
  - `pending(1)`：拒絕、核准。
  - `approved(2)`：改為拒絕。
  - `rejected(3)`：改為審核中、改為核准。

API：

- 本組件不直接呼叫 API。
- 透過 emit `update-equipment-status` 交由 `ReviewCalendar.vue` 呼叫 `PUT /api/equipment-reviews/{id}/status`。

關聯：

- 父層：`ReviewCalendar.vue`
- Props：`equipmentBookings`、`equipmentLoading`、`equipmentProcessingId`
- Events：`update-equipment-status`、原場地審核相關事件。

## Navigation

`NavBar.vue` 的使用者選單目前只保留個人功能：

- `我的場地借用紀錄`：導向 `MyBookingHistory`。
- `我的設備借用紀錄`：導向 `EquipmentBorrowHistory`。

設備狀態管理不再放在個人選單中，避免個人功能與管理/查詢工具混在一起。

## Known Gaps

- `EquipmentBorrowHistory.vue` 尚未提供修改與撤回設備申請的 UI，雖然 API client 已保留 `updateEquipmentBooking` 與 `withdrawEquipmentBooking`。
- `EquipmentBorrowForm.vue` 尚未支援選擇既有場地預約來申請受場地限制設備。
- 設備主檔 CRUD API 已存在，但目前前端沒有完整主檔管理介面。
- Review 設備列表目前沒有狀態篩選與分頁控制，仍是第一階段可操作版本。
