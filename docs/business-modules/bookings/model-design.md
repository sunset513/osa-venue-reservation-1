# Bookings Model Design

來源：

- `backend/venue-reservation-service/src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/entity/Booking.java`
- `C:/Users/wl110/Desktop/osa_infra/mysql/init/02_seed_venue.sql`

## Tables

| Model / Table | 說明 |
| --- | --- |
| `Booking` / `bookings` | 預約申請主表。 |
| `booking_equipment` | 預約與設備借用關聯表。 |
| `venues` | 場地資料，預約以 `venue_id` 關聯。 |
| `users` | 使用者資料，預約以 Portal `user_id` 關聯。 |
| `equipments` | 設備主檔，透過 `booking_equipment` 關聯。 |
| `audit_logs` | 預留稽核日誌表，目前 booking/review service 未寫入。 |

## Bookings 欄位

| 欄位 | 型別 | 說明 |
| --- | --- | --- |
| `id` | bigint | 申請案編號。 |
| `venue_id` | bigint | 預約場地 ID。 |
| `user_id` | varchar(20) | 申請人 Portal identifier。 |
| `booking_date` | date | 預約日期。 |
| `time_slots` | int unsigned | 24-bit 時段遮罩，一小時一格。 |
| `status` | tinyint | 預約狀態。 |
| `purpose` | varchar(255) | 使用用途。 |
| `p_count` | int | 預估人數。 |
| `contact_info` | json | 聯絡人姓名、Email、電話。 |
| `version` | int | 樂觀鎖版本號。 |
| `created_at` | timestamp | 建立時間。 |
| `updated_at` | timestamp | 更新時間。 |

## 狀態值

- `0`：已撤回。
- `1`：審核中。
- `2`：已通過。
- `3`：已拒絕。
- `4`：已刪除，目前由管理端刪除預約案時使用。

## 關聯

- `bookings.venue_id` 對應 `venues.id`。
- `bookings.user_id` 對應 `users.user_id`。
- `booking_equipment.booking_id` 對應 `bookings.id`。
- `booking_equipment.equipment_id` 對應 `equipments.id`。

## 時段模型

`time_slots` 使用 24-bit mask 儲存時段：

- bit `0` 表示 `00:00-01:00`。
- bit `8` 表示 `08:00-09:00`。
- bit `23` 表示 `23:00-24:00`。

API request/response 通常使用 `List<Integer>` 表示時段。`BookingUtils.convertToMask` 負責 `List<Integer>` 到 mask 的轉換，`BookingUtils.parseMaskToList` 負責反向轉換。

## Equipment 關係

預約可選擇借用設備。後端不將設備 ID 存在 `bookings`，而是寫入 `booking_equipment`。查詢預約列表或詳情時，mapper 透過 `GROUP_CONCAT(e.name)` 彙整設備名稱，再由 `CommaSeparatedListTypeHandler` 轉成 `List<String>`。

## 樂觀鎖

`version` 用於狀態更新：

- 管理員審核通過時呼叫 `updateStatusWithVersion`。
- 管理員調整審核狀態時呼叫 `updateStatusWithVersion`。
- 更新成功時 `version = version + 1`。
- 若影響筆數為 `0`，表示版本已過期，service 會回報需重新查詢。

使用者修改預約的 `updateBooking` 也會遞增 `version`，但目前沒有帶舊版號做條件檢查。
