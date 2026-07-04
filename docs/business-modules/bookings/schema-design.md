# Bookings Schema Design

來源：

- `backend/venue-reservation-service/src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/dto`
- `backend/venue-reservation-service/src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo`

## Request DTO

- `BookingRequestDTO`：建立與修改預約共用 payload。
- `BookingQueryDTO`：個人預約多維度查詢與分頁條件。
- `ReviewRequestDTO`：管理端更新預約審核狀態。

## BookingRequestDTO

| 欄位 | 必填 | 說明 |
| --- | --- | --- |
| `venueId` | Yes | 場地 ID。 |
| `bookingDate` | Yes | 預約日期，需為今天或未來日期。 |
| `slots` | Yes | 時段列表，元素範圍 `0-23`，至少一個。 |
| `purpose` | Yes | 使用用途，最多 255 字。 |
| `participantCount` | Yes | 預估人數，最少 1 人。 |
| `contactInfo` | Yes | 聯絡人資訊。 |
| `equipmentIds` | No | 借用設備 ID 列表，可省略或傳空陣列。 |

`contactInfo` 包含：

- `name`：聯絡人姓名。
- `email`：聯絡人 Email。
- `phone`：聯絡電話。

Service 會將 `contactInfo` 轉為 JSON 字串後寫入 `bookings.contact_info`。

## BookingQueryDTO

| 欄位 | 必填 | 說明 |
| --- | --- | --- |
| `venueId` | No | 場地 ID，空值表示不篩選場地。 |
| `statusList` | No | 狀態列表，空值表示不篩選狀態。 |
| `startDate` | No | 預約日期起日。 |
| `endDate` | No | 預約日期迄日。 |
| `pageNo` | No | 頁碼，小於 1 時 service 會修正為 1。 |
| `pageSize` | No | 每頁筆數，小於 1 時修正為 20，大於 100 時限制為 100。 |

`getOffset()` 會依 `pageNo` 與 `pageSize` 計算 SQL offset。

## ReviewRequestDTO

| 欄位 | 必填 | 說明 |
| --- | --- | --- |
| `bookingId` | Yes | 預約申請案 ID。Controller 會以 path id 覆蓋 request 中的 bookingId。 |
| `status` | Yes | 目標狀態，範圍 `0-4`。 |

## Response VO

- `BookingVO`：預約清單與詳情共用輸出。
- `BookingPageVO`：個人預約分頁查詢結果。
- `VenueCalendarMonthVO`：場地月曆視圖。
- `VenueCalendarWeekVO`：場地周曆視圖。
- `VenueCalendarDayVO`：場地日曆視圖。
- `ApprovedBookingsByVenueVO`：指定日期兩場地已通過預約分組結果。
- `ApprovedBookingSimpleVO`：已通過預約簡化資料。
- `ApprovedBookingQueryVO`：mapper 查詢承接物件。

## BookingVO

| 欄位 | 說明 |
| --- | --- |
| `id` | 預約申請案 ID。 |
| `venueName` | 場地名稱。 |
| `bookingDate` | 預約日期。 |
| `slots` | 預約時段列表，由 `time_slots` mask 轉換。 |
| `status` | 預約狀態。 |
| `createdAt` | 申請建立時間。 |
| `purpose` | 使用用途。 |
| `pCount` | 預估人數。 |
| `contactInfo` | 聯絡人 JSON 字串。 |
| `equipments` | 借用設備名稱列表。 |

## Calendar VO

- 月視圖回傳 `year`、`month`、每日摘要 `days` 與該月 `bookings`。
- 周視圖回傳 `weekStart`、`weekEnd` 與 7 天 `days`，每一天包含 `approvedSlots` 與 `userSlots`。
- 日視圖回傳 `venueId`、`venueName`、`date`、`dayOfWeek`、`approvedSlots`、`userSlots` 與 `userBookingDetails`。

## Type Handler

- `TimeSlotTypeHandler`：將 SQL 中的 `time_slots` mask 轉為 `List<Integer>`。
- `CommaSeparatedListTypeHandler`：將 SQL `GROUP_CONCAT` 的設備名稱字串轉為 `List<String>`。
