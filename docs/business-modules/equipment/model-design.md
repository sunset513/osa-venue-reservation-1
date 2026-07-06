# Equipment Model Design

來源：

- `venue_seed_v2.sql`
- 設備獨立借用功能修改指南

## Tables

| Table | 說明 |
| --- | --- |
| `equipments` | 設備主檔。 |
| `equipment_venue_rules` | 設備允許使用場地規則。 |
| `equipment_bookings` | 設備借用申請主表。 |
| `equipment_booking_items` | 設備借用申請明細。 |
| `bookings` | 場地預約，可由設備申請選擇性關聯。 |
| `venues` | 場地資料，供設備限制規則參照。 |
| `users` | 申請人與審核人 Portal 資料。 |

## Deprecated Tables

新設計不再使用：

- `venue_equipment_map`：舊語意為場地擁有哪些設備。
- `booking_equipment`：舊語意為場地預約附帶借用哪些設備。

`venue_seed_v2.sql` 保留 `DROP TABLE IF EXISTS` 以便舊環境重建，但不再建立這兩張表。

## Equipments

`equipments` 表示可借用的設備種類。

| 欄位 | 說明 |
| --- | --- |
| `id` | 設備種類 ID。 |
| `name` | 設備名稱，唯一。 |
| `total_quantity` | 設備總數量，同一時間最多可核准借出數。 |
| `description` | 設備介紹。 |
| `borrow_note` | 借用方式與限制說明。 |
| `deleted_at` | 設備主檔軟刪除時間。 |
| `created_at` | 建立時間。 |
| `updated_at` | 更新時間。 |

約束：

- `name` 唯一。
- `total_quantity >= 1`。

## Equipment Venue Rules

`equipment_venue_rules` 表示設備允許在哪些場地使用。

| 欄位 | 說明 |
| --- | --- |
| `id` | 規則 ID。 |
| `equipment_id` | 設備 ID。 |
| `venue_id` | 允許使用場地 ID。 |
| `rule_note` | 規則說明。 |
| `created_at` | 建立時間。 |
| `updated_at` | 更新時間。 |

語意：

- 某設備沒有任何 rule：不限制場地。
- 某設備有 rule：只能在列出的場地使用。

約束：

- `(equipment_id, venue_id)` 唯一。
- 設備刪除時 rule 連動刪除。
- 場地刪除時 rule 連動刪除。

## Equipment Bookings

`equipment_bookings` 是設備借用申請主表。

| 欄位 | 說明 |
| --- | --- |
| `id` | 設備借用申請編號。 |
| `user_id` | 申請人 Portal ID。 |
| `borrow_date` | 借用日期。 |
| `time_slots` | 24-bit 時段遮罩。 |
| `status` | 申請狀態。 |
| `purpose` | 借用用途。 |
| `contact_info` | 聯絡資訊 JSON。 |
| `related_venue_booking_id` | 選擇性關聯的場地預約 ID。 |
| `reviewed_by` | 審核人員 Portal ID。 |
| `reviewed_at` | 審核時間。 |
| `version` | 樂觀鎖版本號。 |
| `created_at` | 建立時間。 |
| `updated_at` | 更新時間。 |

狀態：

- `0`：已撤回。
- `1`：審核中。
- `2`：已通過。
- `3`：已拒絕。
- `4`：已刪除。

## Equipment Booking Items

`equipment_booking_items` 是設備借用明細表。

| 欄位 | 說明 |
| --- | --- |
| `id` | 明細 ID。 |
| `equipment_booking_id` | 設備借用申請 ID。 |
| `equipment_id` | 設備 ID。 |
| `quantity` | 申請數量。 |

約束：

- `(equipment_booking_id, equipment_id)` 唯一。
- `quantity > 0`。
- 主申請刪除時明細連動刪除。
- 設備被明細引用時不可實體刪除。

## 關聯

- `equipment_bookings.user_id` 對應 `users.user_id`。
- `equipment_bookings.related_venue_booking_id` 對應 `bookings.id`，場地預約刪除時設為 `NULL`。
- `equipment_booking_items.equipment_booking_id` 對應 `equipment_bookings.id`。
- `equipment_booking_items.equipment_id` 對應 `equipments.id`。
- `equipment_venue_rules.equipment_id` 對應 `equipments.id`。
- `equipment_venue_rules.venue_id` 對應 `venues.id`。

## 時段模型

設備借用時段沿用場地預約的 24-bit mask：

- bit `0` 表示 `00:00-01:00`。
- bit `8` 表示 `08:00-09:00`。
- bit `23` 表示 `23:00-24:00`。

API request/response 使用 `slots: List<Integer>`，repository 寫入資料庫時轉為 `time_slots` mask。

## Seed State

目前 `venue_seed_v2.sql` 已包含：

- 三個場地：會議室、交誼廳、學務長會議室。
- 五種設備。
- 投影機白/黑限制於會議室。
- 三筆設備借用申請範例。
- 五筆設備借用明細範例。
