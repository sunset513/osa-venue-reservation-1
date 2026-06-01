# 公開查詢：當天兩場地已通過活動 API 實作報告

**日期：** 2026-06-01  
**模組：** Booking  
**範圍：** 兩場地已通過預約公開查詢

---

## 一、實作範圍

- 新增公開查詢 API（兩場地、指定日期、依場地分組）
- 新增 VO 與 Mapper 查詢
- 更新 Service/Mapper/API 設計文件

---

## 二、變更檔案

### 2.1 程式碼
- `src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/ApprovedBookingSimpleVO.java`
- `src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/ApprovedBookingsByVenueVO.java`
- `src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/ApprovedBookingQueryVO.java`
- `src/main/java/tw/edu/ncu/osa/venue_reservation_service/mapper/BookingMapper.java`
- `src/main/resources/mapper/BookingMapper.xml`
- `src/main/java/tw/edu/ncu/osa/venue_reservation_service/service/BookingService.java`
- `src/main/java/tw/edu/ncu/osa/venue_reservation_service/service/impl/BookingServiceImpl.java`
- `src/main/java/tw/edu/ncu/osa/venue_reservation_service/controller/BookingController.java`

### 2.2 設計文件
- `docs/Design/Service_design/Booking_Service_Design.md`
- `docs/Design/Mapper_Design/Booking_Mapper_Design.md`
- `docs/Design/API_Design/Booking_API_Design.md`
- `docs/Design/API_Design/Booking_API_Detailed_Documentation.md`

---

## 三、設計與實作摘要

- API 路徑：`GET /api/bookings/approved/two-venues`
- 參數：`venueIdA`、`venueIdB`、`date`
- 回傳：`List<ApprovedBookingsByVenueVO>`（含 `venueName`、`purpose`）
- 只回傳 `status = 2` 的預約
- 依場地分組回傳，即使無資料亦回傳空陣列

---

## 四、資料交互流程

1. Controller 解析參數並呼叫 Service
2. Service 驗證參數、查場地、呼叫 Mapper
3. Mapper 以 SQL 取得兩場地當日已通過預約
4. Service 組裝回傳 VO 並回傳 Result

---

## 五、自我檢查

- [x] 無 Lambda / Stream 使用
- [x] 註解使用繁體中文
- [x] 回傳 Result<T> 統一格式
- [x] 依場地分組且回傳最小必要欄位

---

## 六、測試建議

- 兩場地皆有已通過預約
- 其中一場地無預約
- 兩場地皆無預約
- 兩場地相同（回傳錯誤）
- 日期為空或格式錯誤（回傳錯誤）

