# Bookings Service Design

來源：

- `backend/venue-reservation-service/src/main/java/tw/edu/ncu/osa/venue_reservation_service/service/impl/BookingServiceImpl.java`
- `backend/venue-reservation-service/src/main/java/tw/edu/ncu/osa/venue_reservation_service/service/impl/ReviewServiceImpl.java`

## BookingService 責任

- 建立場地預約申請。
- 查詢目前登入使用者的個人預約。
- 依篩選條件分頁查詢個人預約。
- 修改個人預約。
- 撤回個人預約。
- 查詢場地月、周、日曆視圖。
- 查詢指定日期三個場地的已通過預約。

## ReviewService 責任

- 管理端查詢預約列表。
- 管理端查詢單筆預約詳情。
- 審核通過預約。
- 更新預約審核狀態。
- 軟刪除預約案。

## 建立預約

流程：

1. 從 `UserContext` 取得目前登入者 Portal `userId`。
2. 將 request `slots` 轉為 24-bit mask。
3. 呼叫 `countConflictingApprovedBookings` 檢查已通過預約衝突。
4. 若有衝突，丟出 `該時段已被其他已通過之申請佔用`。
5. 建立 `Booking`，狀態設為 `1=審核中`。
6. 將 `contactInfo` 序列化為 JSON 字串。
7. 新增 `bookings`。
8. 若有 `equipmentIds`，逐筆新增 `booking_equipment`。

## 個人預約查詢

`getMyBookings`：

- 依目前登入者 `userId` 查詢所有預約。
- 目前 service 手動轉成 `BookingVO`。
- `venueName` 暫以 `"場地 " + venueId` 表示。
- 未填入 `purpose`、`pCount`、`contactInfo`、`equipments` 等完整查詢欄位。

`queryMyBookings`：

- 支援場地、狀態列表、日期範圍與分頁。
- `pageNo` 預設/修正為 1。
- `pageSize` 預設/修正為 20，上限 100。
- 先查總數，再查當前頁資料。
- 回傳 `BookingPageVO`。

## 修改預約

規則：

- 只能修改自己的預約。
- 預約必須存在。
- 只有 `status=1` 或 `status=2` 可修改。
- 修改後狀態重置為 `1=審核中`。
- 會重新檢查已通過預約衝突。
- 會更新預約基本欄位與聯絡資訊。
- 會刪除舊設備關聯，再寫入新的 `equipmentIds`。

注意：目前衝突檢查沒有排除被修改的 booking 本身。

## 撤回預約

規則：

- 只能撤回自己的預約。
- 預約必須存在。
- 只有 `status=1` 或 `status=2` 可撤回。
- 撤回會透過 `updateStatusWithVersion` 將狀態改為 `0=已撤回`。
- 若版本不符，回報預約案已被他人修改。

## 日曆視圖

### 月視圖

`getVenueCalendarMonth`：

- 驗證 `venueId`、`year`、`month`。
- 計算該月起訖日期。
- 查詢指定場地該月全部預約。
- 依 `status=2` 建立每日 `hasApprovedBooking`。
- 組裝 `VenueCalendarMonthVO`。

注意：`hasUserBooking` 目前固定為 `false`。

### 周視圖

`getVenueCalendarWeek`：

- 驗證 `venueId` 與 `weekStartDate`。
- `weekStartDate` 必須是星期一。
- 查詢該周已通過預約與目前使用者預約。
- 依日期合併並去重 `approvedSlots` 與 `userSlots`。
- 回傳周一至周日共七天資料。

### 日視圖

`getVenueCalendarDay`：

- 驗證 `venueId` 與 `date`。
- 查詢該日已通過預約與目前使用者預約。
- 合併並排序 `approvedSlots` 與 `userSlots`。
- 組裝使用者該日預約詳情。

注意：`venueName` 目前暫以 `"場地 " + venueId` 表示。

## 指定日期三場地已通過預約

`getApprovedBookingsForThreeVenues`：

- 驗證三個場地 ID 與日期不可為空。
- 三個場地不可相同。
- 檢查三個場地是否存在。
- 查詢指定日期 `status=2` 的預約。
- 依場地分組回傳，每筆只包含 `bookingId`、`slots`、`purpose`。

## 管理端預約列表

`getPendingBookings`：

- `venueId` 未提供時預設為 `1`。
- 日期範圍未完整提供時預設為當月第一天到最後一天。
- 驗證 `startDate <= endDate`。
- `status` 若提供，必須在 `0-3`。
- 查詢時排除 `status=4`。

## 預約詳情

`getBookingDetails`：

- 驗證 booking ID。
- 查詢預約、場地名稱、時段、狀態、用途、人數、聯絡資訊與設備名稱清單。
- 排除 `status=4`。

## 審核通過

`reviewBooking`：

1. 驗證 booking ID。
2. 查詢預約主檔。
3. 只有 `status=1` 可通過。
4. 檢查是否已有已通過預約衝突。
5. 使用樂觀鎖將該預約改為 `2=已通過`。
6. 查詢其他與此預約衝突的 `status=1` 預約。
7. 若存在衝突審核中預約，批量改為 `3=已拒絕`。

## 更新審核狀態

`updateReviewStatus`：

- 驗證 booking ID 與目標狀態 `0-4`。
- 查詢預約目前狀態。
- 從 `3=已拒絕` 改為 `2=已通過` 時，會重新檢查衝突，並拒絕其他衝突的審核中預約。
- 使用樂觀鎖更新狀態。

## 軟刪除預約

`deleteBooking`：

- 驗證 booking ID。
- 查詢預約是否存在。
- 刪除 `booking_equipment` 關聯。
- 將 `bookings.status` 改為 `4=已刪除`。

## Transaction

- 建立、修改、撤回、審核通過、更新狀態與刪除皆使用交易。
- 查詢方法使用 read-only transaction。
