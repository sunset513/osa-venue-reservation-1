-- Venue reservation system schema and seed data
-- Generated for Docker MySQL init/import
--
-- Equipment borrowing has been separated from venue booking.
-- equipment_venue_rules semantics:
--   1. No rule rows for an equipment = unrestricted equipment.
--   2. One or more rule rows = the equipment may only be used in the listed venues.
--   3. A restricted equipment application must reference a venue booking whose venue is allowed.
--
-- Booking status:
--   0 = withdrawn, 1 = pending, 2 = approved, 3 = rejected, 4 = soft deleted

SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `venue_reservation_system`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `venue_reservation_system`;

-- Drop child tables first, then parent tables
DROP TABLE IF EXISTS `audit_logs`;
DROP TABLE IF EXISTS `equipment_booking_items`;
DROP TABLE IF EXISTS `equipment_bookings`;
DROP TABLE IF EXISTS `equipment_venue_rules`;

-- Legacy tables: retained here only so an old database can be cleanly rebuilt
DROP TABLE IF EXISTS `booking_equipment`;
DROP TABLE IF EXISTS `venue_equipment_map`;

DROP TABLE IF EXISTS `bookings`;
DROP TABLE IF EXISTS `admin_role`;
DROP TABLE IF EXISTS `equipments`;
DROP TABLE IF EXISTS `venues`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `units`;

-- =========================================================
-- Table: units
-- =========================================================
CREATE TABLE `units` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '單位唯一識別碼',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '單位名稱，例如：學務處本部',
  `code` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '單位代碼，例如：STUA，對接 Portal 身分用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_unit_name` (`name`),
  UNIQUE KEY `uk_unit_code` (`code`)
) ENGINE=InnoDB
  AUTO_INCREMENT=2
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='管理單位表';

-- =========================================================
-- Table: users
-- =========================================================
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '系統內部唯一識別碼',
  `user_id` varchar(20) NOT NULL COMMENT 'Portal identifier，與借用申請關聯的鍵',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `role` enum('USER','ADMIN') NOT NULL DEFAULT 'USER' COMMENT '角色：USER、ADMIN',
  `email` varchar(255) DEFAULT NULL COMMENT '電子郵件',
  `unit_id` bigint DEFAULT NULL COMMENT '所屬單位 ID',
  `login_at` timestamp NULL DEFAULT NULL COMMENT '最後登入時間',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '停用或封鎖時間，有值代表禁止登入',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_user_id` (`user_id`),
  KEY `idx_portal_id` (`user_id`),
  KEY `idx_user_deleted_at` (`deleted_at`)
) ENGINE=InnoDB
  AUTO_INCREMENT=3
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='使用者資料表';

-- =========================================================
-- Table: admin_role
-- =========================================================
CREATE TABLE `admin_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '系統管理員角色唯一識別碼',
  `user_id` varchar(20) NOT NULL COMMENT 'Portal identifier',
  `level` tinyint NOT NULL DEFAULT 0 COMMENT '管理員等級：0=一般管理員，1=可管理其他管理員',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '刪除或停用時間，有值代表此管理員角色無效',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_role_user_id` (`user_id`),
  KEY `idx_admin_role_user_id` (`user_id`),
  KEY `idx_admin_role_deleted_at` (`deleted_at`),
  CONSTRAINT `chk_admin_role_level`
    CHECK (`level` IN (0, 1))
) ENGINE=InnoDB
  AUTO_INCREMENT=3
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系統管理員角色表';

-- =========================================================
-- Table: venues
-- =========================================================
CREATE TABLE `venues` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '場地唯一識別碼',
  `unit_id` bigint NOT NULL COMMENT '所屬單位 ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '場地名稱，例如：會議室',
  `capacity` int NOT NULL COMMENT '容納人數上限',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '借用規則或場地介紹',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_venue_unit_id` (`unit_id`),
  CONSTRAINT `fk_venue_unit`
    FOREIGN KEY (`unit_id`) REFERENCES `units` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB
  AUTO_INCREMENT=4
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='場地資訊表';

-- =========================================================
-- Table: equipments
-- Equipment is now an independent borrowable resource and is
-- no longer owned by a particular venue.
-- =========================================================
CREATE TABLE `equipments` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '設備種類 ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '設備名稱',
  `total_quantity` int NOT NULL DEFAULT 1 COMMENT '設備可管理的總數量',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '設備介紹',
  `borrow_note` text COLLATE utf8mb4_unicode_ci COMMENT '借用方式與使用限制說明',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '設備主檔軟刪除時間',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_equipment_name` (`name`),
  CONSTRAINT `chk_equipment_total_quantity`
    CHECK (`total_quantity` >= 0)
) ENGINE=InnoDB
  AUTO_INCREMENT=6
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='設備主檔';

-- =========================================================
-- Table: bookings
-- Existing venue booking table.
-- Status remains consistent with current application logic.
-- =========================================================
CREATE TABLE `bookings` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '場地申請案編號',
  `venue_id` bigint NOT NULL COMMENT '關聯場地 ID',
  `user_id` varchar(20) NOT NULL COMMENT '申請人 NCU Portal ID',
  `booking_date` date NOT NULL COMMENT '預約日期',
  `time_slots` int unsigned NOT NULL COMMENT '24-bit 位元遮罩時段，每小時一格',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0:撤回, 1:審核中, 2:通過, 3:拒絕, 4:刪除',
  `purpose` varchar(255) NOT NULL COMMENT '使用用途',
  `p_count` int NOT NULL COMMENT '預估人數',
  `contact_info` json NOT NULL COMMENT '聯絡人姓名、電子郵件等 JSON 資訊',
  `version` int NOT NULL DEFAULT 1 COMMENT '樂觀鎖版本號',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_booking_venue_date` (`venue_id`, `booking_date`),
  KEY `idx_booking_user_id` (`user_id`),
  KEY `idx_booking_date_status` (`booking_date`, `status`),
  CONSTRAINT `fk_booking_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_booking_venue`
    FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`)
    ON DELETE CASCADE,
  CONSTRAINT `chk_booking_status`
    CHECK (`status` IN (0, 1, 2, 3, 4))
) ENGINE=InnoDB
  AUTO_INCREMENT=4
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='場地預約申請表';

-- =========================================================
-- Table: equipment_venue_rules
--
-- Business semantics:
--   * No rows for an equipment: it can be borrowed without a venue.
--   * Rows exist for an equipment: it is restricted to the listed venues.
--   * Restricted equipment must reference a venue booking, and the venue
--     of that booking must match one of the allowed venues.
-- =========================================================
CREATE TABLE `equipment_venue_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '設備場地規則 ID',
  `equipment_id` bigint NOT NULL COMMENT '受限制的設備 ID',
  `venue_id` bigint NOT NULL COMMENT '允許使用此設備的場地 ID',
  `rule_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '規則補充說明',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_equipment_venue_rule` (`equipment_id`, `venue_id`),
  KEY `idx_equipment_venue_rule_venue` (`venue_id`),
  CONSTRAINT `fk_equipment_venue_rule_equipment`
    FOREIGN KEY (`equipment_id`) REFERENCES `equipments` (`id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_equipment_venue_rule_venue`
    FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB
  AUTO_INCREMENT=3
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='設備可使用場地規則；無規則代表不限制場地';

-- =========================================================
-- Table: equipment_bookings
-- Independent equipment borrowing application.
-- No deleted_at is added; status=4 is used consistently with bookings.
-- =========================================================
CREATE TABLE `equipment_bookings` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '設備借用申請編號',
  `user_id` varchar(20) NOT NULL COMMENT '申請人 NCU Portal ID',
  `borrow_date` date NOT NULL COMMENT '設備借用日期',
  `time_slots` int unsigned NOT NULL COMMENT '24-bit 位元遮罩時段，每小時一格',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0:撤回, 1:審核中, 2:通過, 3:拒絕, 4:刪除',
  `purpose` varchar(255) NOT NULL COMMENT '設備借用用途',
  `contact_info` json NOT NULL COMMENT '聯絡人姓名、電子郵件等 JSON 資訊',
  `related_venue_booking_id` bigint DEFAULT NULL COMMENT '選擇性關聯的場地預約 ID；受場地限制設備必須填寫',
  `reviewed_by` varchar(20) DEFAULT NULL COMMENT '審核人員 Portal ID',
  `reviewed_at` timestamp NULL DEFAULT NULL COMMENT '審核時間',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '拒絕原因',
  `version` int NOT NULL DEFAULT 1 COMMENT '樂觀鎖版本號',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_equipment_booking_user_id` (`user_id`),
  KEY `idx_equipment_booking_date_status` (`borrow_date`, `status`),
  KEY `idx_equipment_booking_related_venue` (`related_venue_booking_id`),
  CONSTRAINT `fk_equipment_booking_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_equipment_booking_related_venue`
    FOREIGN KEY (`related_venue_booking_id`) REFERENCES `bookings` (`id`)
    ON DELETE SET NULL,
  CONSTRAINT `chk_equipment_booking_status`
    CHECK (`status` IN (0, 1, 2, 3, 4))
) ENGINE=InnoDB
  AUTO_INCREMENT=4
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='獨立設備借用申請主表';

-- =========================================================
-- Table: equipment_booking_items
-- One equipment booking can contain multiple equipment types
-- and an explicit requested quantity for each type.
-- =========================================================
CREATE TABLE `equipment_booking_items` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '設備借用明細 ID',
  `equipment_booking_id` bigint NOT NULL COMMENT '設備借用申請 ID',
  `equipment_id` bigint NOT NULL COMMENT '設備 ID',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '申請數量',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_equipment_booking_item`
    (`equipment_booking_id`, `equipment_id`),
  KEY `idx_equipment_booking_item_equipment` (`equipment_id`),
  CONSTRAINT `fk_equipment_booking_item_booking`
    FOREIGN KEY (`equipment_booking_id`) REFERENCES `equipment_bookings` (`id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_equipment_booking_item_equipment`
    FOREIGN KEY (`equipment_id`) REFERENCES `equipments` (`id`)
    ON DELETE RESTRICT,
  CONSTRAINT `chk_equipment_booking_item_quantity`
    CHECK (`quantity` > 0)
) ENGINE=InnoDB
  AUTO_INCREMENT=6
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='設備借用申請明細表';

-- =========================================================
-- Table: audit_logs
-- Kept unchanged in this revision.
-- =========================================================
CREATE TABLE `audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '紀錄編號',
  `booking_id` bigint DEFAULT NULL COMMENT '關聯預約單號',
  `op_id` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作者 ID',
  `action` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '動作類型，例如：UPDATE_STATUS',
  `old_data` json DEFAULT NULL COMMENT '變更前資料快照',
  `new_data` json DEFAULT NULL COMMENT '變更後資料快照',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '發生時間',
  PRIMARY KEY (`id`),
  KEY `idx_booking_id` (`booking_id`),
  KEY `idx_op_id` (`op_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='稽核日誌表';

-- =========================================================
-- Seed data: units
-- =========================================================
INSERT INTO `units`
  (`id`, `name`, `code`, `created_at`, `updated_at`)
VALUES
  (1, '學務處本部', 'STUA',
   '2026-04-14 02:38:34', '2026-04-14 02:38:34');

-- =========================================================
-- Seed data: users
-- =========================================================
INSERT INTO `users`
  (`id`, `user_id`, `name`, `role`, `email`, `unit_id`,
   `login_at`, `deleted_at`, `created_at`)
VALUES
  (1, '110502000', '中央大學測試生A', 'USER',
   'student@ncu.edu.tw', 1, NULL, NULL, '2026-04-14 02:38:21'),
  (2, '110502001', '中央大學測試生B', 'USER',
   'student-b@ncu.edu.tw', 1, NULL, NULL, '2026-04-30 07:57:07');

-- =========================================================
-- Seed data: admin_role
-- =========================================================
INSERT INTO `admin_role`
  (`id`, `user_id`, `level`, `deleted_at`, `created_at`, `updated_at`)
VALUES
  (1, '114423011', 1, NULL,
   '2026-06-23 00:00:00', '2026-06-23 00:00:00'),
  (2, '110401520', 1, NULL,
   '2026-06-23 00:00:00', '2026-06-23 00:00:00');

-- =========================================================
-- Seed data: venues
-- =========================================================
INSERT INTO `venues`
  (`id`, `unit_id`, `name`, `capacity`, `description`,
   `created_at`, `updated_at`)
VALUES
  (1, 1, '會議室', 40,
   '配備完整視聽設備，適合正式會議',
   '2026-05-01 03:54:03', '2026-05-01 03:54:03'),
  (2, 1, '交誼廳', 30,
   '適合小組討論與團體活動',
   '2026-05-01 03:54:03', '2026-05-01 03:54:03'),
  (3, 1, '學務長會議室', 15,
   '適合小型會議使用',
   '2026-07-04 00:00:00', '2026-07-04 00:00:00');

-- =========================================================
-- Seed data: equipments
-- 麥克風總數量確認為 4 支。
-- =========================================================
INSERT INTO `equipments`
  (`id`, `name`, `total_quantity`, `description`, `borrow_note`,
   `deleted_at`, `created_at`, `updated_at`)
VALUES
  (1, '麥克風', 4,
   '活動、會議使用的無線麥克風',
   '借用與歸還時請確認設備數量及外觀。',
   NULL, '2026-05-01 03:41:28', '2026-05-01 03:41:28'),

  (2, '投影機(白)', 1,
   '白色無線投影設備',
   '僅限學務處本部會議室使用；申請時必須關聯該會議室的場地預約。',
   NULL, '2026-05-01 03:41:28', '2026-05-01 03:41:28'),

  (3, '投影機(黑)', 1,
   '黑色無線投影設備',
   '僅限學務處本部會議室使用；申請時必須關聯該會議室的場地預約。',
   NULL, '2026-05-01 03:41:28', '2026-05-01 03:41:28'),

  (4, '冷氣遙控器', 1,
   '場地空調使用的遙控器',
   '使用完畢後請依規定歸還。',
   NULL, '2026-05-01 03:41:28', '2026-05-01 03:41:28'),

  (5, 'HDMI 線', 1,
   '簡報設備使用的 HDMI 連接線',
   '借用與歸還時請確認接頭及線材完整。',
   NULL, '2026-05-01 03:41:28', '2026-05-01 03:41:28');

-- =========================================================
-- Seed data: bookings
-- =========================================================
INSERT INTO `bookings`
  (`id`, `venue_id`, `user_id`, `booking_date`, `time_slots`,
   `status`, `purpose`, `p_count`, `contact_info`, `version`,
   `created_at`, `updated_at`)
VALUES
  (1, 1, '110502000', '2026-05-07', 393216,
   2, '全大運籌備會議1', 10,
   '{"name": "黃先生", "email": "student@ncu.edu.tw", "phone": "0912345678"}',
   2, '2026-05-01 03:56:10', '2026-05-01 03:58:45'),

  (2, 1, '110502000', '2026-05-07', 2621440,
   1, '全大運籌備會議2', 4,
   '{"name": "李小姐", "email": "student@ncu.edu.tw", "phone": "0912345678"}',
   3, '2026-05-01 04:00:22', '2026-05-01 04:01:27'),

  (3, 1, '110502000', '2026-05-10', 229376,
   3, '全大運籌備會議3', 2,
   '{"name": "Sean", "email": "student@ncu.edu.tw", "phone": "0912345678"}',
   2, '2026-05-01 04:00:52', '2026-05-01 04:02:50');

-- =========================================================
-- Seed data: equipment_venue_rules
-- Only the black and white projectors are venue-restricted.
-- Equipment without rows in this table is unrestricted.
-- =========================================================
INSERT INTO `equipment_venue_rules`
  (`id`, `equipment_id`, `venue_id`, `rule_note`,
   `created_at`, `updated_at`)
VALUES
  (1, 2, 1,
   '投影機(白)僅限學務處本部會議室使用。',
   '2026-07-04 00:00:00', '2026-07-04 00:00:00'),

  (2, 3, 1,
   '投影機(黑)僅限學務處本部會議室使用。',
   '2026-07-04 00:00:00', '2026-07-04 00:00:00');

-- =========================================================
-- Seed data: equipment_bookings
--
-- ID 1 and 2 demonstrate equipment applications related to venue bookings.
-- ID 3 demonstrates a completely independent equipment application.
-- =========================================================
INSERT INTO `equipment_bookings`
  (`id`, `user_id`, `borrow_date`, `time_slots`, `status`,
   `purpose`, `contact_info`, `related_venue_booking_id`,
   `reviewed_by`, `reviewed_at`, `reject_reason`, `version`,
   `created_at`, `updated_at`)
VALUES
  (1, '110502000', '2026-05-07', 393216, 2,
   '全大運籌備會議1設備借用',
   '{"name": "黃先生", "email": "student@ncu.edu.tw", "phone": "0912345678"}',
   1, '114423011', '2026-05-01 03:58:30', NULL, 2,
   '2026-05-01 03:56:20', '2026-05-01 03:58:30'),

  (2, '110502000', '2026-05-07', 2621440, 1,
   '全大運籌備會議2設備借用',
   '{"name": "李小姐", "email": "student@ncu.edu.tw", "phone": "0912345678"}',
   2, NULL, NULL, NULL, 1,
   '2026-05-01 04:00:30', '2026-05-01 04:00:30'),

  (3, '110502001', '2026-05-08', 3072, 2,
   '校外簡報設備借用',
   '{"name": "中央大學測試生B", "email": "student-b@ncu.edu.tw", "phone": "0911222333"}',
   NULL, '114423011', '2026-05-02 09:10:00', NULL, 2,
   '2026-05-02 09:00:00', '2026-05-02 09:10:00');

-- =========================================================
-- Seed data: equipment_booking_items
-- =========================================================
INSERT INTO `equipment_booking_items`
  (`id`, `equipment_booking_id`, `equipment_id`, `quantity`)
VALUES
  (1, 1, 1, 1),
  (2, 1, 2, 1),
  (3, 2, 4, 1),
  (4, 2, 3, 1),
  (5, 3, 5, 1);

-- No seed data for audit_logs

SET UNIQUE_CHECKS = 1;
SET FOREIGN_KEY_CHECKS = 1;