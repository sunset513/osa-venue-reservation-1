# 公開查詢：當天兩場地已通過活動 API 設計文件

**日期：** 2026-06-01  
**模組：** Booking  
**狀態：** 設計中  
**文件位置：** docs/dev-process/Booking_Module/BOOKING_APPROVED_TWO_VENUES_API_DESIGN.md

---

## 一、功能說明

提供公開查詢 API，針對**指定日期**與**固定兩個場地**，回傳兩場地中**已審核通過（status=2）**的預約活動。回傳需**依場地分組**，且僅包含**最小必要欄位**（包含 purpose 與 venueName）。

---

## 二、API 規格

### 2.1 端點資訊
- **方法：** GET
- **路徑：** /api/bookings/approved/two-venues
- **認證：** 不需要（公開 API）

### 2.2 請求參數（Query）
| 參數名稱 | 類型 | 必填 | 說明 |
| :--- | :--- | :--- | :--- |
| venueIdA | Long | 是 | 第一個場地 ID |
| venueIdB | Long | 是 | 第二個場地 ID（不可與 venueIdA 相同） |
| date | LocalDate | 是 | 查詢日期（YYYY-MM-DD） |

### 2.3 成功回應
- **狀態碼：** 200
- **回應格式：** Result<List<ApprovedBookingsByVenueVO>>

#### ApprovedBookingsByVenueVO
| 欄位 | 類型 | 說明 |
| :--- | :--- | :--- |
| venueId | Long | 場地 ID |
| venueName | String | 場地名稱 |
| items | List<ApprovedBookingSimpleVO> | 已通過預約清單 |

#### ApprovedBookingSimpleVO
| 欄位 | 類型 | 說明 |
| :--- | :--- | :--- |
| bookingId | Long | 預約 ID |
| slots | List<Integer> | 已通過時段清單（0-23） |
| purpose | String | 使用用途 |

### 2.4 失敗回應
- **狀態碼：** 200（依據系統統一回應格式）
- **回應格式：** Result<null>

#### 可能錯誤訊息
- "場地 ID 不可為空"
- "兩個場地不可相同"
- "日期不可為空"

### 2.5 請求範例
```http
GET /api/bookings/approved/two-venues?venueIdA=1&venueIdB=2&date=2026-06-01
```

### 2.6 回應範例
```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "venueId": 1,
      "venueName": "會議室 A",
      "items": [
        {
          "bookingId": 501,
          "slots": [8, 9],
          "purpose": "專案討論"
        }
      ]
    },
    {
      "venueId": 2,
      "venueName": "會議室 B",
      "items": []
    }
  ]
}
```

---

## 三、資料交互與分層責任

### 3.1 Controller 層
- **檔案：** src/main/java/tw/edu/ncu/osa/venue_reservation_service/controller/BookingController.java
- **責任：**
  - 解析 query 參數（venueIdA、venueIdB、date）
  - 基本參數檢核（空值、兩場地相同）
  - 呼叫 Service 回傳 Result

### 3.2 Service 層
- **檔案：** src/main/java/tw/edu/ncu/osa/venue_reservation_service/service/BookingService.java
- **實作：** src/main/java/tw/edu/ncu/osa/venue_reservation_service/service/impl/BookingServiceImpl.java
- **責任：**
  - 組裝兩個場地 ID 清單
  - 呼叫 Mapper 取得已通過預約
  - 依場地分組並組裝 ApprovedBookingsByVenueVO
  - 透過 BookingUtils 將 time_slots 轉為 List<Integer>

### 3.3 Mapper 層
- **介面：** src/main/java/tw/edu/ncu/osa/venue_reservation_service/mapper/BookingMapper.java
- **SQL：** src/main/resources/mapper/BookingMapper.xml
- **責任：**
  - 查詢指定日期與兩場地的 status=2 預約
  - JOIN venues 取得 venue_name

### 3.4 資料庫層
- **主要表：** bookings、venues
- **關鍵欄位：**
  - bookings.venue_id
  - bookings.booking_date
  - bookings.time_slots
  - bookings.status
  - venues.name

---

## 四、SQL 查詢設計（示意）

```sql
SELECT
  b.id AS booking_id,
  b.venue_id,
  v.name AS venue_name,
  b.time_slots,
  b.purpose
FROM bookings b
JOIN venues v ON b.venue_id = v.id
WHERE b.venue_id IN (#{venueIdA}, #{venueIdB})
  AND b.booking_date = #{date}
  AND b.status = 2
ORDER BY b.venue_id ASC, b.created_at ASC;
```

---

## 五、回傳資料組裝邏輯

1. Mapper 回傳兩場地的已通過預約清單（含 venueId、venueName、timeSlots、purpose）。
2. Service 依 venueId 分組：
   - 產生兩筆 ApprovedBookingsByVenueVO（即使某場地無資料，items 也回空陣列）。
3. time_slots 使用 BookingUtils.parseMaskToList 轉換成 slots。

---

## 六、影響範圍（檔案清單）

### 6.1 Controller
- src/main/java/tw/edu/ncu/osa/venue_reservation_service/controller/BookingController.java

### 6.2 Service
- src/main/java/tw/edu/ncu/osa/venue_reservation_service/service/BookingService.java
- src/main/java/tw/edu/ncu/osa/venue_reservation_service/service/impl/BookingServiceImpl.java

### 6.3 Mapper
- src/main/java/tw/edu/ncu/osa/venue_reservation_service/mapper/BookingMapper.java
- src/main/resources/mapper/BookingMapper.xml

### 6.4 VO
- src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/ApprovedBookingSimpleVO.java
- src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/ApprovedBookingsByVenueVO.java

### 6.5 API 文件
- docs/Design/API_Design/Booking_API_Design.md
- docs/Design/API_Design/Booking_API_Detailed_Documentation.md

### 6.6 設計文件
- docs/Design/Service_design/Booking_Service_Design.md
- docs/Design/Mapper_Design/Booking_Mapper_Design.md

---

## 七、測試清單（建議）

- 兩場地皆有已通過預約（回傳兩組 items）
- 其中一場地無預約（items 為空陣列）
- 兩場地皆無預約（兩組 items 都為空陣列）
- venueIdA 與 venueIdB 相同（回傳錯誤）
- date 為空或格式錯誤（回傳錯誤）

---

## 八、備註

- 本 API 為公開查詢，不需驗證。
- 僅回傳已通過（status=2）之預約活動。
- 回傳欄位為最小必要欄位，避免傳輸過多資料。

