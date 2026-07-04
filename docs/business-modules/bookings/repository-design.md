# Bookings Repository Design

來源：

- `backend/venue-reservation-service/src/main/java/tw/edu/ncu/osa/venue_reservation_service/mapper/BookingMapper.java`
- `backend/venue-reservation-service/src/main/resources/mapper/BookingMapper.xml`
- `backend/venue-reservation-service/src/main/java/tw/edu/ncu/osa/venue_reservation_service/mapper/ReviewMapper.java`
- `backend/venue-reservation-service/src/main/resources/mapper/ReviewMapper.xml`

## 責任

- 建立、查詢、更新預約主表 `bookings`。
- 新增與刪除預約設備關聯 `booking_equipment`。
- 檢查已通過預約的時段衝突。
- 查詢使用者個人預約清單與分頁結果。
- 查詢月、周、日曆視圖需要的預約資料。
- 查詢管理端預約列表、預約詳情與設備清單。
- 批量更新衝突預約狀態。
- 以 `status=4` 軟刪除預約案。

## BookingMapper

### 衝突檢查

`countConflictingApprovedBookings` 查詢同場地、同日期、`status=2` 且時段重疊的預約數量。

```sql
AND (time_slots & #{mask}) != 0
```

此方法用於建立預約、修改預約、審核通過與拒絕改通過。

### 基本資料操作

- `insertBooking`：新增預約主表，初始 `version=1`。
- `selectById`：依 ID 查詢預約實體。
- `selectByUserId`：依使用者查詢所有預約，依 `created_at DESC` 排序。
- `updateBooking`：更新預約內容，並讓 `version + 1`。
- `updateStatusWithVersion`：使用 `id + oldVersion` 更新狀態，並讓 `version + 1`。

### 設備關聯

- `insertBookingEquipment`：新增 `booking_equipment` 關聯。
- `deleteBookingEquipmentByBookingId`：刪除某預約的全部設備關聯。

使用者修改預約時採「先刪除全部舊關聯，再重新寫入新 equipmentIds」策略。

### 日曆查詢

- `selectApprovedBookingsByDateRange`：查詢指定場地、日期範圍內已通過預約。
- `selectUserBookingsByDateRange`：查詢指定使用者、場地、日期範圍內所有個人預約。
- `selectBookingsByDateRangeForCalendar`：查詢指定場地、日期範圍內全部預約，並 join 場地與設備。
- `selectApprovedBookingsForTwoVenues`：查詢指定日期兩個場地的已通過預約。

### 個人分頁查詢

`queryMyBookingsWithFilters` 支援：

- `userId` 固定為當前登入者。
- 可選 `venueId`。
- 可選 `statusList`。
- 可選 `startDate` / `endDate`。
- `LIMIT` / `OFFSET` 分頁。

回傳使用 `bookingVOResultMap`，透過 `TimeSlotTypeHandler` 和 `CommaSeparatedListTypeHandler` 做欄位轉換。

## ReviewMapper

### 管理端查詢

- `selectBookingsByVenueAndDateRange`：依場地、日期範圍與可選狀態查詢預約；預設排除 `status=4`。
- `selectPendingBookingsByVenueAndDateRange`：舊方法，只查 `status=1`，目前標為 deprecated。
- `selectBookingWithEquipments`：依預約 ID 查詢預約詳情與設備清單，排除 `status=4`。

### 狀態更新

- `batchUpdateStatus`：批量更新多筆預約狀態，並遞增版本。
- `deleteSoftBooking`：將預約狀態更新為 `4`。
- `deleteBookingEquipmentsByBookingId`：刪除預約設備關聯。

## 查詢排序

- `selectByUserId`：`created_at DESC`。
- `queryMyBookingsWithFilters`：`created_at DESC`。
- `selectBookingsByVenueAndDateRange`：`created_at ASC`。
- `selectBookingsByDateRangeForCalendar`：`booking_date ASC, created_at ASC`。
- `selectApprovedBookingsForTwoVenues`：`venue_id ASC, created_at ASC`。

## 注意事項

- `ReviewMapper.selectConflictingApprovedBookings` 有排除指定 `bookingId` 的 SQL，但目前主要審核流程使用的是 `BookingMapper.countConflictingApprovedBookings`。
- 使用者修改預約時的衝突檢查未排除自身，可能影響已通過預約的原時段修改。
- `bookings` 表沒有 `deleted_at` 欄位，軟刪除以 `status=4` 表示。
