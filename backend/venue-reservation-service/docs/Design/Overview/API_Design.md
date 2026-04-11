# API 設計文檔 - 場地預約系統
**更新日期**：2026-04-11  
**狀態**：已更新
---
## 📋 API 簽名規範
### 統一回應格式 (Result<T>)
所有 API 回應都遵循統一格式：
`json
{
  "code": 200,
  "msg": "操作成功",
  "data": { ... }
}
`
（註：實際的 Result 結構可能是 success, message, data；具體依據當前程式碼為準，以 	w.edu.ncu.osa.venue_reservation_service.common.result.Result 實際輸出為主。）
---
## 🛠️ Module 1：場地與基礎數據查詢 API (VenueController)
**基路徑：** /api/public  
**功能：** 提供前端渲染場地選擇器和初始化數據
### 1.1 取得所有管理單位清單
- **端點：** GET /api/public/units
- **功能：** 取得系統中所有的管理單位
- **請求參數：** 無
- **成功回應 (200)：**
  `json
  {
    "success": true,
    "message": "操作成功",
    "data": [
      {
        "id": 1,
        "name": "學務處本部",
        "code": "STUA"
      }
    ]
  }
  `
### 1.2 根據單位取得場地清單
- **端點：** GET /api/public/venues
- **功能：** 根據選定的單位，查詢該單位下的所有場地
- **請求參數：**
  - unitId (Long，必填，Query)
- **成功回應 (200)：**
  `json
  {
    "success": true,
    "message": "操作成功",
    "data": [
      {
        "id": 101,
        "unitId": 1,
        "name": "會議室 A",
        "capacity": 50,
        "description": "配備投影機與無線麥克風",
        "equipments": null
      }
    ]
  }
  `
### 1.3 取得單一場地的詳細資訊
- **端點：** GET /api/public/venues/{id}
- **功能：** 取得單一場地的詳細資訊，包括可借用設備清單
- **路徑參數：**
  - id (Long，必填，Path)
- **成功回應 (200)：**
  `json
  {
    "success": true,
    "message": "操作成功",
    "data": {
      "id": 101,
      "unitId": 1,
      "name": "會議室 A",
      "capacity": 50,
      "description": "配備投影機與無線麥克風",
      "equipments": [
        {
          "id": 1,
          "name": "投影機"
        }
      ]
    }
  }
  `
---
## 🛠️ Module 2：預約操作 API (BookingController)
**基路徑：** /api/bookings  
### 2.1 提交預約申請
- **端點：** POST /api/bookings
- **功能：** 提交新的預約申請，系統會自動檢查時段衝突等
- **請求參數（Body）：** BookingRequestDTO
  `json
  {
    "venueId": 101,
    "bookingDate": "2026-04-15",
    "slots": [8, 9, 10],
    "purpose": "課程討論會議",
    "participantCount": 15,
    "contactInfo": {
      "name": "王小明",
      "email": "student@ncu.edu.tw",
      "phone": "0912345678"
    },
    "equipmentIds": [1, 2]
  }
  `
- **成功回應 (200)：** 回傳建立的預約 id
  `json
  {
    "success": true,
    "message": "操作成功",
    "data": 501
  }
  `
### 2.2 查看個人預約清單
- **端點：** GET /api/bookings/my
- **功能：** 查看當前登入用戶的所有預約申請清單
- **成功回應 (200)：**
  `json
  {
    "success": true,
    "message": "操作成功",
    "data": [
      {
        "id": 501,
        "venueName": "會議室 A",
        "bookingDate": "2026-04-15",
        "slots": [8, 9, 10],
        "status": 1,
        "createdAt": "2026-04-11T10:00:00",
        "purpose": "課程討論會議",
        "pCount": 15,
        "contactInfo": "{\"name\":\"王小明\",\"phone\":\"0912345678\",\"email\":\"student@ncu.edu.tw\"}",
        "equipments": ["投影機", "麥克風"]
      }
    ]
  }
  `
### 2.3 修改預約申請
- **端點：** PUT /api/bookings/{id}
- **功能：** 修改預約申請資料
- **路徑參數：** id (Long，必填，Path)
- **請求參數（Body）：** BookingRequestDTO
- **成功回應 (200)：** data 為 null
### 2.4 撤回預約申請
- **端點：** PUT /api/bookings/{id}/withdraw
- **功能：** 撤回預約申請
- **路徑參數：** id (Long，必填，Path)
- **成功回應 (200)：** data 為 null
---
## 🛠️ Module 3：日曆視圖查詢 API (BookingController)
**基路徑：** /api/bookings/calendar  
### 3.1 月曆視圖
- **端點：** GET /api/bookings/calendar/month
- **請求參數：**
  - enueId (Long，必填，Query)
  - year (Integer，必填，Query)
  - month (Integer，必填，Query)
- **成功回應 (200)：** 包含 year, month, days（date, hasApprovedBooking, hasUserBooking）, 等等屬性，具體視 VenueCalendarMonthVO 結構而定。
### 3.2 周曆視圖
- **端點：** GET /api/bookings/calendar/week
- **請求參數：**
  - enueId (Long，必填，Query)
  - date (LocalDate，必填，Query，標準格式：YYYY-MM-DD)
- **成功回應 (200)：** 包含該週之每日可用/佔用/用戶的詳細時段列表資料，具體屬性參考程式碼的 VenueCalendarWeekVO。
### 3.3 日曆視圖
- **端點：** GET /api/bookings/calendar/day
- **請求參數：**
  - enueId (Long，必填，Query)
  - date (LocalDate，必填，Query，標準格式：YYYY-MM-DD)
- **成功回應 (200)：** 包含該日之中各種時段資料及用戶自己預約的資訊列表，具體屬性參考程式碼的 VenueCalendarDayVO。
