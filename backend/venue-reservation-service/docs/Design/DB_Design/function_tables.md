這份最新的資料表設計說明已經根據您提供的 `seed.sql` 進行了更新。本次更新的主要亮點包含：新增了 `audit_logs`（稽核日誌表）以追蹤操作紀錄、在設備字典中加入了軟刪除機制 (`deleted_at`)，以及在場地設備對照表中增加了數量欄位 (`quantity`)。

以下為最新的業務資料表設計說明：

# 系統業務資料表設計說明

---

### 0. 使用者資料表 (`users`)
**設計理念：** 存放系統使用者資訊，並透過 `user_id` (Portal ID) 與其他業務表（如預約表）進行關聯，支援軟刪除。

| 欄位名稱 | 資料型別 | 屬性 / 預設值 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 系統內部唯一識別碼 |
| `user_id` | VARCHAR(20) | UNIQUE, NOT NULL | Portal ID (與 bookings 關聯的鍵) |
| `name` | VARCHAR(50) | NOT NULL | 姓名 |
| `role` | ENUM | DEFAULT 'USER', NOT NULL | 角色: USER, ADMIN |
| `unit_id` | BIGINT | NULL | 所屬單位 ID |
| `is_deleted` | TINYINT(1) | DEFAULT 0, NOT NULL | 軟刪除 (0:否, 1:是) |
| `deleted_at` | TIMESTAMP | NULL | 刪除時間 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 建立時間 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新時間 |

---

### 1. 管理單位表 (`units`)
**設計理念：** 支援多租戶擴展，區分不同行政單位，並對接 Portal 的身分代碼。

| 欄位名稱 | 資料型別 | 屬性 / 預設值 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 單位唯一識別碼 |
| `name` | VARCHAR(50) | UNIQUE, NOT NULL | 單位名稱 (如：學務處本部) |
| `code` | VARCHAR(10) | UNIQUE, NOT NULL | 單位代碼 (如：STUA)，對接 Portal 身分用 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 建立時間 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新時間 |

---

### 2. 場地資訊表 (`venues`)
**設計理念：** 記錄可供預約的場地基本資訊，透過 `unit_id` 綁定管理單位以優化權限與查詢效能。

| 欄位名稱 | 資料型別 | 屬性 / 預設值 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 場地唯一識別碼 |
| `unit_id` | BIGINT | FK, NOT NULL | 所屬單位 ID (關聯 `units.id`) |
| `name` | VARCHAR(50) | NOT NULL | 場地名稱 (如：會議室 A) |
| `capacity` | INT | NOT NULL | 容納人數上限 |
| `description` | TEXT | NULL | 借用規則或場地介紹 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 建立時間 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新時間 |

---

### 3. 設備字典表 (`equipments`)
**設計理念：** 存放全系統通用的設備名稱清單，避免字串重複儲存。**新增了軟刪除功能**，以應對設備汰換的狀況。

| 欄位名稱 | 資料型別 | 屬性 / 預設值 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 唯一識別碼 |
| `name` | VARCHAR(50) | UNIQUE, NOT NULL | 設備名稱 (字典) |
| `deleted_at` | TIMESTAMP | NULL | 軟刪除 (若設備汰換不再提供可押時間) |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 建立時間 |

---

### 4. 場地預設擁有設備清單 (`venue_equipment_map`)
**設計理念：** 定義 M:N 關係，標註特定場地預設擁有哪些設備，並**新增數量紀錄**供前端展示。

| 欄位名稱 | 資料型別 | 屬性 / 預設值 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 唯一識別碼 |
| `venue_id` | BIGINT | FK, NOT NULL | 關聯場地 ID (關聯 `venues.id`) |
| `equipment_id` | BIGINT | FK, NOT NULL | 關聯設備 ID (關聯 `equipments.id`) |
| `quantity` | INT | DEFAULT 1, NOT NULL | 純前端顯示用 (該場地擁有數量) |

*(註：具備 `(venue_id, equipment_id)` 唯一複合索引)*

---

### 5. 預約申請表 (`bookings`)
**設計理念：** 預約系統核心表。利用位元遮罩 (`time_slots`) 高效儲存時段，樂觀鎖 (`version`) 確保高併發下的狀態安全。

| 欄位名稱 | 資料型別 | 屬性 / 預設值 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 申請案編號 |
| `venue_id` | BIGINT | FK, NOT NULL | 關聯場地 ID |
| `user_id` | VARCHAR(20) | FK, NOT NULL | 申請人 NCU Portal ID (關聯 `users.user_id`) |
| `booking_date` | DATE | NOT NULL | 預約日期 |
| `time_slots` | INT UNSIGNED| NOT NULL | 24-bit 位元遮罩時段 (1小時一格) |
| `status` | TINYINT | DEFAULT 1, NOT NULL | 0:撤回, 1:審核中, 2:通過, 3:拒絕 |
| `purpose` | VARCHAR(255)| NOT NULL | 使用用途 |
| `p_count` | INT | NOT NULL | 預估人數 |
| `contact_info` | JSON | NOT NULL | 聯絡人姓名、電子郵件等 JSON 資訊 |
| `version` | INT | DEFAULT 1, NOT NULL | 樂觀鎖版本號 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 建立時間 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新時間 |

*(註：具備 `(venue_id, booking_date)` 複合索引優化月曆渲染)*

---

### 6. 該筆預約借用設備紀錄表 (`booking_equipment`)
**設計理念：** 記錄單筆預約申請中，申請人實際勾選借用的設備紀錄。

| 欄位名稱 | 資料型別 | 屬性 / 預設值 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 唯一識別碼 |
| `booking_id` | BIGINT | FK, NOT NULL | 關聯預約 ID (關聯 `bookings.id`) |
| `equipment_id` | BIGINT | FK, NOT NULL | 關聯設備 ID (關聯 `equipments.id`) |

---

### 7. 稽核日誌表 (`audit_logs`)
**設計理念：** **(全新資料表)** 用於追蹤系統內針對預約單的重要操作紀錄，保留變更前後的資料快照以便日後追查。

| 欄位名稱 | 資料型別 | 屬性 / 預設值 | 說明 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 紀錄編號 |
| `booking_id` | BIGINT | NULL | 關聯預約單號 |
| `op_id` | VARCHAR(20) | NOT NULL | 操作者 ID |
| `action` | VARCHAR(20) | NOT NULL | 動作類型 (如：UPDATE_STATUS) |
| `old_data` | JSON | NULL | 變更前資料快照 |
| `new_data` | JSON | NULL | 變更後資料快照 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 發生時間 |