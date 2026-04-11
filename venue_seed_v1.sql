-- MySQL dump 10.13  Distrib 8.0.36, for Win64 (x86_64)
--
-- Host: localhost    Database: venue_reservation_system
-- ------------------------------------------------------
-- Server version	8.0.36

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '紀錄編號',
  `booking_id` bigint DEFAULT NULL COMMENT '關聯預約單號',
  `op_id` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作者 ID',
  `action` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '動作類型 (如：UPDATE_STATUS)',
  `old_data` json DEFAULT NULL COMMENT '變更前資料快照',
  `new_data` json DEFAULT NULL COMMENT '變更後資料快照',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '發生時間',
  PRIMARY KEY (`id`),
  KEY `idx_booking_id` (`booking_id`),
  KEY `idx_op_id` (`op_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='稽核日誌表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_logs`
--

LOCK TABLES `audit_logs` WRITE;
/*!40000 ALTER TABLE `audit_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `booking_equipment`
--

DROP TABLE IF EXISTS `booking_equipment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking_equipment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '唯一識別碼',
  `booking_id` bigint NOT NULL COMMENT '關聯預約 ID',
  `equipment_id` bigint NOT NULL COMMENT '關聯設備 ID',
  PRIMARY KEY (`id`),
  KEY `fk_be_booking` (`booking_id`),
  KEY `fk_be_equip` (`equipment_id`),
  CONSTRAINT `fk_be_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_be_equip` FOREIGN KEY (`equipment_id`) REFERENCES `equipments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='該筆預約借用設備紀錄表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `booking_equipment`
--

LOCK TABLES `booking_equipment` WRITE;
/*!40000 ALTER TABLE `booking_equipment` DISABLE KEYS */;
INSERT INTO `booking_equipment` VALUES (1,1,1),(2,1,2),(3,3,1),(4,5,1),(5,5,2),(6,8,1),(7,8,2),(8,9,2),(9,11,1),(10,12,1),(11,13,2);
/*!40000 ALTER TABLE `booking_equipment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bookings`
--

DROP TABLE IF EXISTS `bookings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bookings` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申請案編號',
  `venue_id` bigint NOT NULL COMMENT '關聯場地 ID',
  `user_id` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '申請人 NCU Portal ID',
  `booking_date` date NOT NULL COMMENT '預約日期',
  `time_slots` int unsigned NOT NULL COMMENT '24-bit 位元遮罩時段 (1小時一格)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0:撤回, 1:審核中, 2:通過, 3:拒絕',
  `purpose` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '使用用途',
  `p_count` int NOT NULL COMMENT '預估人數',
  `contact_info` json NOT NULL COMMENT '聯絡人姓名、電子郵件等 JSON 資訊',
  `version` int NOT NULL DEFAULT '1' COMMENT '樂觀鎖版本號',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_venue_date` (`venue_id`,`booking_date`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_booking_venue` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='預約申請表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bookings`
--

LOCK TABLES `bookings` WRITE;
/*!40000 ALTER TABLE `bookings` DISABLE KEYS */;
INSERT INTO `bookings` VALUES (1,1,'110502001','2026-04-10',3072,2,'系學會週會',20,'{\"name\": \"林同學\", \"email\": \"lin@ncu.edu.tw\", \"phone\": \"0912345678\"}',1,'2026-04-03 07:05:32','2026-04-03 07:05:32'),(2,1,'110502002','2026-04-10',6144,1,'專題小組討論',5,'{\"name\": \"王同學\", \"email\": \"wang@ncu.edu.tw\", \"phone\": \"0987654321\"}',1,'2026-04-03 07:05:32','2026-04-03 07:05:32'),(3,1,'110502003','2026-04-10',16384,1,'吉他社練習',10,'{\"name\": \"陳同學\", \"email\": \"chen@ncu.edu.tw\", \"phone\": \"0911222333\"}',1,'2026-04-03 07:05:32','2026-04-03 07:05:32'),(4,1,'110502004','2026-04-10',49152,1,'熱舞社討論',15,'{\"name\": \"李同學\", \"email\": \"lee@ncu.edu.tw\", \"phone\": \"0933444555\"}',1,'2026-04-03 07:05:32','2026-04-03 07:05:32'),(5,1,'110502000','2026-04-10',768,1,'畢業專題分組討論',5,'{\"name\": \"王小明\", \"email\": \"student@ncu.edu.tw\", \"phone\": \"0912345678\"}',1,'2026-04-03 09:48:13','2026-04-03 09:48:13'),(8,1,'110502000','2026-04-11',1920,1,'資管系專題討論會',10,'{\"name\": \"張小明\", \"email\": \"student123@ncu.edu.tw\", \"phone\": \"0912345678\"}',3,'2026-04-04 03:59:24','2026-04-11 02:45:28'),(9,1,'110502000','2026-04-12',16,1,'大在一起',10,'{\"name\": \"測試生\", \"email\": \"student@ncu.edu.tw\", \"phone\": \"0912345678\"}',1,'2026-04-04 15:27:42','2026-04-04 15:27:42'),(10,1,'110502000','2026-04-09',786432,1,'test',100,'{\"name\": \"測試生\", \"email\": \"student@ncu.edu.tw\", \"phone\": \"0912345678\"}',2,'2026-04-04 15:47:28','2026-04-05 07:19:54'),(11,1,'110502000','2026-05-07',1792,1,'測試',10,'{\"name\": \"測試生\", \"email\": \"student@ncu.edu.tw\", \"phone\": \"0912345678\"}',1,'2026-04-05 07:08:42','2026-04-05 07:08:42'),(12,1,'110502000','2026-04-08',6144,1,'面試1',5,'{\"name\": \"面試者\", \"email\": \"student@ncu.edu.tw\", \"phone\": \"0912345678\"}',3,'2026-04-05 07:10:14','2026-04-05 07:39:35'),(13,1,'110502000','2026-04-05',3840,1,'面試',3,'{\"name\": \"測試生\", \"email\": \"student@ncu.edu.tw\", \"phone\": \"0912345678\"}',1,'2026-04-05 07:39:19','2026-04-05 07:39:19'),(14,1,'110502000','2026-04-14',42,1,'zxczczx',1,'{\"name\": \"測試生\", \"email\": \"student@ncu.edu.tw\", \"phone\": \"0912345678\"}',1,'2026-04-09 01:12:51','2026-04-09 01:12:51');
/*!40000 ALTER TABLE `bookings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `equipments`
--

DROP TABLE IF EXISTS `equipments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `equipments` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '設備唯一識別碼',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '設備名稱 (如：無線麥克風)',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='設備主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `equipments`
--

LOCK TABLES `equipments` WRITE;
/*!40000 ALTER TABLE `equipments` DISABLE KEYS */;
INSERT INTO `equipments` VALUES (1,'麥克風','2026-04-03 07:05:32'),(2,'投影機','2026-04-03 07:05:32'),(3,'投影機遙控器','2026-04-03 07:05:32'),(4,'投影幕','2026-04-03 07:05:32');
/*!40000 ALTER TABLE `equipments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `units`
--

DROP TABLE IF EXISTS `units`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `units` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '單位唯一識別碼',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '單位名稱 (如：學務處本部)',
  `code` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '單位代碼 (如：STUA)，對接 Portal 身分用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理單位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `units`
--

LOCK TABLES `units` WRITE;
/*!40000 ALTER TABLE `units` DISABLE KEYS */;
INSERT INTO `units` VALUES (1,'學務處本部','STUA','2026-04-03 07:05:32','2026-04-03 07:05:32');
/*!40000 ALTER TABLE `units` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `venue_equipment_map`
--

DROP TABLE IF EXISTS `venue_equipment_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `venue_equipment_map` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `venue_id` bigint NOT NULL COMMENT '關聯場地',
  `equipment_id` bigint NOT NULL COMMENT '關聯設備',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_venue_equip` (`venue_id`,`equipment_id`),
  KEY `fk_map_equip` (`equipment_id`),
  CONSTRAINT `fk_map_equip` FOREIGN KEY (`equipment_id`) REFERENCES `equipments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_map_venue` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='場地與可借用設備對照表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `venue_equipment_map`
--

LOCK TABLES `venue_equipment_map` WRITE;
/*!40000 ALTER TABLE `venue_equipment_map` DISABLE KEYS */;
INSERT INTO `venue_equipment_map` VALUES (1,1,1),(2,1,2),(3,1,3),(4,1,4),(5,2,1),(6,2,4);
/*!40000 ALTER TABLE `venue_equipment_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `venues`
--

DROP TABLE IF EXISTS `venues`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `venues` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '場地唯一識別碼',
  `unit_id` bigint NOT NULL COMMENT '所屬單位 ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '場地名稱 (如：會議室 A)',
  `capacity` int NOT NULL COMMENT '容納人數上限',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '借用規則或場地介紹',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_unit_id` (`unit_id`),
  CONSTRAINT `fk_venue_unit` FOREIGN KEY (`unit_id`) REFERENCES `units` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='場地資訊表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `venues`
--

LOCK TABLES `venues` WRITE;
/*!40000 ALTER TABLE `venues` DISABLE KEYS */;
INSERT INTO `venues` VALUES (1,1,'志道樓 2 樓會議室 (A)',30,'配備完整視聽設備，適合社團大型會議','2026-04-03 07:05:32','2026-04-03 07:05:32'),(2,1,'志道樓 1 樓研討室 (B)',15,'適合小組討論，環境安靜','2026-04-03 07:05:32','2026-04-03 07:05:32');
/*!40000 ALTER TABLE `venues` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-11 12:02:06
