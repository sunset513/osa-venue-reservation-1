# 預約管理模組 API 詳細文檔

**文檔版本：** V2.0 (詳細版)  
**最後更新日期：** 2026-04-11  
**基礎路徑：** `/api/bookings`  
**認證方式：** 於 Header 攜帶 `Authorization: mock-token-123` (MVP 階段使用)

---

## 一、模組概述

**預約管理模組** 提供場地預約的核心業務功能，包括：
- 📝 **預約申請**：用戶提交新的場地預約申請
- 📋 **預約查詢**：用戶查看個人預約清單
- ✏️ **預約修改**：用戶修改已提交的預約申請
- ❌ **預約撤回**：用戶撤回不需要的預約申請
- 📅 **日曆視圖**：用戶在不同時間維度（月、周、日）查看場地可用時段

用戶透過此模組可完整管理其場地預約申請的整個生命週期。

---

## 二、數據傳輸物件設計 (DTO/VO)

### 2.1 BookingRequestDTO - 預約申請請求

**用途：** 接收前端提交的預約申請資料（新建或修改預約時使用）

| 欄位名稱 | 類型 | 必填 | 說明 | 範例 |
| :--- | :--- | :--- | :--- | :--- |
| `venueId` | Long | 是 | 場地唯一識別碼 | `101` |
| `bookingDate` | LocalDate | 是 | 預約日期，格式：YYYY-MM-DD，必須為現在或未來日期 | `2026-04-10` |
| `slots` | List<Integer> | 是 | 預約時段列表，使用 24 小時制索引（0-23）。例如 [8, 9] 表示預約 08:00-09:00 和 09:00-10:00 兩個時段 | `[8, 9]` |
| `purpose` | String | 是 | 場地使用用途說明，最多 255 字符 | `舉辦專案討論會議` |
| `participantCount` | Integer | 是 | 預計使用人數，最少 1 人 | `5` |
| `contactInfo` | ContactDTO | 是 | 聯絡人詳細資訊物件 | 見下表 |
| `equipmentIds` | List<Long> | 否 | 借用設備 ID 列表（可選，若不借用設備可省略或傳入空陣列） | `[1, 2]` |

#### 2.1.1 ContactDTO - 聯絡人資訊

| 欄位名稱 | 類型 | 必填 | 說明 | 範例 |
| :--- | :--- | :--- | :--- | :--- |
| `name` | String | 是 | 聯絡人姓名 | `王小明` |
| `email` | String | 是 | 聯絡人電子郵件，需符合 Email 格式 | `xm@ncu.edu.tw` |
| `phone` | String | 是 | 聯絡電話，建議使用台灣手機號碼格式（如 09XXXXXXXX） | `0912345678` |

**完整請求示例：**

```json
{
  "venueId": 101,
  "bookingDate": "2026-04-10",
  "slots": [8, 9],
  "purpose": "舉辦專案討論會議",
  "participantCount": 5,
  "contactInfo": {
    "name": "王小明",
    "email": "xm@ncu.edu.tw",
    "phone": "0912345678"
  },
  "equipmentIds": [1, 2]
}
```

---

### 2.2 BookingVO - 預約申請視圖物件

**用途：** 回傳單筆預約申請的詳細資訊（查詢個人預約清單時使用）

| 欄位名稱 | 類型 | 說明 | 範例 |
| :--- | :--- | :--- | :--- |
| `id` | Long | 預約申請案的唯一編號 | `501` |
| `venueName` | String | 場地名稱 | `會議室 A` |
| `bookingDate` | LocalDate | 預約日期，格式：YYYY-MM-DD | `2026-04-10` |
| `slots` | List<Integer> | 預約時段清單，使用 24 小時制索引（0-23） | `[8, 9]` |
| `status` | Integer | 申請審核狀態：0=已撤回、1=審核中、2=已通過、3=已拒絕 | `2` |
| `createdAt` | LocalDateTime | 申請提交時間，格式：YYYY-MM-DDTHH:mm:ss | `2026-04-03T10:00:00` |
| `purpose` | String | 場地使用用途說明 | `舉辦專案討論會議` |
| `pCount` | Integer | 預計使用人數 | `5` |
| `contactInfo` | String | 聯絡人資訊（JSON 格式字串），包含 name、phone、email | `{"name":"王小明","phone":"0912345678","email":"xm@ncu.edu.tw"}` |
| `equipments` | List<String> | 所借用的設備名稱清單 | `["麥克風", "投影機", "音響"]` |

**完整回應示例：**

```json
{
  "id": 501,
  "venueName": "會議室 A",
  "bookingDate": "2026-04-10",
  "slots": [8, 9],
  "status": 2,
  "createdAt": "2026-04-03T10:00:00",
  "purpose": "舉辦專案討論會議",
  "pCount": 5,
  "contactInfo": "{\"name\":\"王小明\",\"phone\":\"0912345678\",\"email\":\"xm@ncu.edu.tw\"}",
  "equipments": ["麥克風", "投影機", "音響"]
}
```

---

### 2.3 VenueCalendarMonthVO - 場地月份日曆視圖

**用途：** 回傳場地在指定月份的日曆視圖資料

| 欄位名稱 | 類型 | 說明 |
| :--- | :--- | :--- |
| `year` | Integer | 年份（西元年） |
| `month` | Integer | 月份（1-12） |
| `days` | List<DaySimpleSummary> | 該月所有日期的簡化摘要資訊 |
| `bookings` | List<BookingVO> | 該月所有預約的詳細列表（包括所有狀態） |

#### 2.3.1 DaySimpleSummary - 日期簡化摘要

| 欄位名稱 | 類型 | 說明 | 範例 |
| :--- | :--- | :--- | :--- |
| `date` | String | 日期，格式：YYYY-MM-DD | `2026-04-01` |
| `hasApprovedBooking` | Boolean | 該日是否有已通過（狀態=2）的預約。true=有，false=無 | `true` |
| `hasUserBooking` | Boolean | 該日是否有用戶自己的預約（無論審核狀態）。true=有，false=無 | `true` |

**完整回應示例：**

```json
{
  "year": 2026,
  "month": 4,
  "days": [
    {
      "date": "2026-04-01",
      "hasApprovedBooking": false,
      "hasUserBooking": false
    },
    {
      "date": "2026-04-02",
      "hasApprovedBooking": true,
      "hasUserBooking": true
    }
  ],
  "bookings": [
    {
      "id": 501,
      "venueName": "會議室 A",
      "bookingDate": "2026-04-10",
      "slots": [8, 9],
      "status": 2,
      "createdAt": "2026-04-03T10:00:00",
      "purpose": "舉辦專案討論會議",
      "pCount": 5,
      "contactInfo": "{\"name\":\"王小明\",\"phone\":\"0912345678\",\"email\":\"xm@ncu.edu.tw\"}",
      "equipments": ["麥克風", "投影機"]
    }
  ]
}
```

---

### 2.4 VenueCalendarWeekVO - 場地周份日曆視圖

**用途：** 回傳場地在指定周份的詳細日曆視圖資料

| 欄位名稱 | 類型 | 說明 |
| :--- | :--- | :--- |
| `weekStart` | String | 周開始日期（周一），格式：YYYY-MM-DD |
| `weekEnd` | String | 周結束日期（周日），格式：YYYY-MM-DD |
| `days` | List<DayDetailSummary> | 周內 7 日（周一至周日）的詳細資訊 |

#### 2.4.1 DayDetailSummary - 日期詳細摘要

| 欄位名稱 | 類型 | 說明 | 範例 |
| :--- | :--- | :--- | :--- |
| `date` | String | 日期，格式：YYYY-MM-DD | `2026-04-06` |
| `dayOfWeek` | String | 星期幾，中文表示 | `星期一` |
| `approvedSlots` | List<Integer> | 已通過預約的時段列表（0-23 表示各小時時段） | `[8, 9]` |
| `userSlots` | List<Integer> | 當前登入用戶的預約時段列表（無論審核狀態）（0-23） | `[8]` |

**完整回應示例：**

```json
{
  "weekStart": "2026-04-06",
  "weekEnd": "2026-04-12",
  "days": [
    {
      "date": "2026-04-06",
      "dayOfWeek": "星期一",
      "approvedSlots": [8, 9],
      "userSlots": [8]
    },
    {
      "date": "2026-04-07",
      "dayOfWeek": "星期二",
      "approvedSlots": [10, 11, 12],
      "userSlots": []
    }
  ]
}
```

---

### 2.5 VenueCalendarDayVO - 場地單日日曆視圖

**用途：** 回傳場地在指定日期的最詳細日曆視圖資料

| 欄位名稱 | 類型 | 說明 |
| :--- | :--- | :--- |
| `venueId` | Long | 場地唯一識別碼 |
| `venueName` | String | 場地名稱 |
| `date` | String | 日期，格式：YYYY-MM-DD |
| `dayOfWeek` | String | 星期幾，中文表示 |
| `approvedSlots` | List<Integer> | 已通過預約的時段列表（0-23） |
| `userSlots` | List<Integer> | 當前登入用戶的預約時段列表（0-23） |
| `userBookingDetails` | List<UserBookingDetail> | 該日期用戶所有預約的詳細資訊 |

#### 2.5.1 UserBookingDetail - 用戶預約詳情

| 欄位名稱 | 類型 | 說明 | 範例 |
| :--- | :--- | :--- | :--- |
| `bookingId` | Long | 預約案的唯一編號 | `501` |
| `slots` | List<Integer> | 該筆預約的時段列表（0-23） | `[8]` |
| `status` | Integer | 預約狀態：0=已撤回、1=審核中、2=已通過、3=已拒絕 | `2` |
| `purpose` | String | 場地使用用途說明 | `舉辦專案討論會議` |
| `createdAt` | LocalDateTime | 預約建立時間，格式：YYYY-MM-DDTHH:mm:ss | `2026-04-03T10:00:00` |

**完整回應示例：**

```json
{
  "venueId": 101,
  "venueName": "會議室 A",
  "date": "2026-04-06",
  "dayOfWeek": "星期一",
  "approvedSlots": [8, 9],
  "userSlots": [8],
  "userBookingDetails": [
    {
      "bookingId": 501,
      "slots": [8],
      "status": 2,
      "purpose": "舉辦專案討論會議",
      "createdAt": "2026-04-03T10:00:00"
    }
  ]
}
```

---

### 2.6 Result<T> - 通用 API 回應物件

**用途：** 封裝所有 API 的回應

| 欄位名稱 | 類型 | 說明 |
| :--- | :--- | :--- |
| `success` | Boolean | 操作是否成功。true=成功，false=失敗 |
| `message` | String | 提示訊息。成功時通常為 "操作成功"；失敗時包含具體錯誤原因 |
| `data` | T | 實際承載的回應資料。對於查詢類操作返回結果；對於修改/刪除操作通常為 null |

**成功回應示例：**

```json
{
  "success": true,
  "message": "操作成功",
  "data": { /* 具體數據 */ }
}
```

**失敗回應示例：**

```json
{
  "success": false,
  "message": "該時段已被其他已通過之申請佔用",
  "data": null
}
```

---

## 三、API 端點詳細說明

### 3.1 提交預約申請

**端點資訊：**
- **方法：** POST
- **路徑：** `/api/bookings`
- **認證：** 必須 (需提供 Mock-Authorization Header)

**功能描述：**

建立一筆新的場地預約申請。用戶提交預約申請後，系統將進行如下處理：

1. **參數驗證**：驗證所有必填欄位已填寫、參數值合理（如人數 ≥ 1、日期不是過去）
2. **時段衝突檢查**：檢查所選時段是否與其他已通過（狀態=2）的預約佔用
3. **申請建立**：若驗證通過，建立預約申請，初始狀態為「審核中（1）」
4. **返回結果**：返回新建預約的 ID

**時段衝突檢查說明：**
- 系統只檢查狀態為「已通過（2）」的預約時段
- 若所選時段與「審核中（1）」或「已拒絕（3）」的預約重疊，系統允許申請通過，由審核者決定衝突處理

**請求示例：**

```bash
curl -X POST "http://localhost:8080/api/bookings" \
  -H "Authorization: mock-token-123" \
  -H "Content-Type: application/json" \
  -d '{
    "venueId": 101,
    "bookingDate": "2026-04-10",
    "slots": [8, 9],
    "purpose": "舉辦專案討論會議",
    "participantCount": 5,
    "contactInfo": {
      "name": "王小明",
      "email": "xm@ncu.edu.tw",
      "phone": "0912345678"
    },
    "equipmentIds": [1, 2]
  }'
```

**成功回應 (200 OK)：**

```json
{
  "success": true,
  "message": "操作成功",
  "data": 501
}
```

**失敗回應示例：**

| 情況 | HTTP 狀態碼 | 回應 |
| :--- | :--- | :--- |
| 參數驗證失敗（如人數為負數） | 200* | `{"success": false, "message": "預估人數至少需為 1 人", "data": null}` |
| 時段衝突 | 200* | `{"success": false, "message": "該時段已被其他已通過之申請佔用", "data": null}` |
| 未預期的伺服器錯誤 | 200* | `{"success": false, "message": "伺服器內部錯誤，請稍後再試", "data": null}` |

*注：本系統統一使用 HTTP 200 回應，通過 success 欄位區分成功與失敗

**可能的錯誤訊息：**

- `"場地 ID 不可為空"`
- `"預約日期不可為空"`
- `"預約日期不能是過去的時間"`
- `"請至少選擇一個預約時段"`
- `"請填寫使用用途"`
- `"用途描述過長"`
- `"預估人數至少需為 1 人"`
- `"聯絡資訊不可為空"`
- `"該時段已被其他已通過之申請佔用"`

---

### 3.2 查詢個人預約清單

**端點資訊：**
- **方法：** GET
- **路徑：** `/api/bookings/my`
- **認證：** 必須 (需提供 Mock-Authorization Header)

**功能描述：**

取得當前登入用戶的所有預約申請記錄。系統將返回該用戶所有狀態（已撤回、審核中、已通過、已拒絕）的申請列表，按申請時間倒序排列。

**預約狀態說明：**
- **0 - 已撤回**：申請人主動撤回的申請，不可再進行任何操作
- **1 - 審核中**：尚未被核准或拒絕的新申請，等待審核
- **2 - 已通過**：申請已獲批准，用戶可使用場地
- **3 - 已拒絕**：申請被駁回，用戶需重新申請

**請求示例：**

```bash
curl -X GET "http://localhost:8080/api/bookings/my" \
  -H "Authorization: mock-token-123"
```

**成功回應 (200 OK)：**

```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "id": 501,
      "venueName": "會議室 A",
      "bookingDate": "2026-04-10",
      "slots": [8, 9],
      "status": 2,
      "createdAt": "2026-04-03T10:00:00",
      "purpose": "舉辦專案討論會議",
      "pCount": 5,
      "contactInfo": "{\"name\":\"王小明\",\"phone\":\"0912345678\",\"email\":\"xm@ncu.edu.tw\"}",
      "equipments": ["麥克風", "投影機"]
    },
    {
      "id": 502,
      "venueName": "會議室 B",
      "bookingDate": "2026-04-15",
      "slots": [14, 15, 16],
      "status": 1,
      "createdAt": "2026-04-05T14:30:00",
      "purpose": "團隊會議",
      "pCount": 10,
      "contactInfo": "{\"name\":\"李小花\",\"phone\":\"0987654321\",\"email\":\"flower@ncu.edu.tw\"}",
      "equipments": []
    }
  ]
}
```

**失敗回應：** 此端點通常不會失敗（即使無預約也返回空陣列）

---

### 3.3 修改預約申請

**端點資訊：**
- **方法：** PUT
- **路徑：** `/api/bookings/{id}`
- **認證：** 必須 (需提供 Mock-Authorization Header)
- **路徑參數：** `id` - 要修改的預約申請 ID（例如 501）

**功能描述：**

修改指定的預約申請內容。用戶可修改尚未被核准或已核准的申請。修改後狀態將自動重置為「審核中」，以便審核者重新評估修改後的申請。

**修改規則：**

1. 僅允許修改狀態為「審核中（1）」或「已通過（2）」的申請
2. 修改後狀態自動重置為「審核中（1）」，需要重新審核
3. 驗證修改後的時段是否與其他已通過預約衝突
4. 更新預約資訊並記錄修改時間

**限制條件：**

- 已拒絕（3）或已撤回（0）的申請無法修改，需重新申請
- 新選時段不可與其他已通過預約重疊

**請求示例：**

```bash
curl -X PUT "http://localhost:8080/api/bookings/501" \
  -H "Authorization: mock-token-123" \
  -H "Content-Type: application/json" \
  -d '{
    "venueId": 101,
    "bookingDate": "2026-04-10",
    "slots": [9, 10],
    "purpose": "修改後的專案討論會議",
    "participantCount": 6,
    "contactInfo": {
      "name": "王小明",
      "email": "xm@ncu.edu.tw",
      "phone": "0912345678"
    },
    "equipmentIds": [1]
  }'
```

**成功回應 (200 OK)：**

```json
{
  "success": true,
  "message": "操作成功",
  "data": null
}
```

**失敗回應示例：**

| 情況 | 回應 |
| :--- | :--- |
| 預約不存在 | `{"success": false, "message": "找不到指定的預約申請", "data": null}` |
| 狀態不允許修改 | `{"success": false, "message": "已拒絕之申請無法修改", "data": null}` |
| 時段衝突 | `{"success": false, "message": "該時段已被其他已通過之申請佔用", "data": null}` |

---

### 3.4 撤回預約申請

**端點資訊：**
- **方法：** PUT
- **路徑：** `/api/bookings/{id}/withdraw`
- **認證：** 必須 (需提供 Mock-Authorization Header)
- **路徑參數：** `id` - 要撤回的預約申請 ID（例如 501）

**功能描述：**

申請人主動撤回已提交的預約申請。撤回後狀態變更為「已撤回」，不可再修改或重新激活。

**撤回規則：**

1. 僅允許撤回狀態為「審核中（1）」或「已通過（2）」的申請
2. 撤回後狀態變更為「已撤回（0）」，不可再修改或重新激活
3. 已拒絕（3）的申請無法撤回（已經被駁回）
4. 已撤回（0）的申請無法重複撤回

**應用場景：**

用戶決定不需要該場地預約，或已取消相應活動計劃

**請求示例：**

```bash
curl -X PUT "http://localhost:8080/api/bookings/501/withdraw" \
  -H "Authorization: mock-token-123"
```

**成功回應 (200 OK)：**

```json
{
  "success": true,
  "message": "操作成功",
  "data": null
}
```

**失敗回應示例：**

| 情況 | 回應 |
| :--- | :--- |
| 預約不存在 | `{"success": false, "message": "找不到指定的預約申請", "data": null}` |
| 狀態不允許撤回 | `{"success": false, "message": "已拒絕之申請無法撤回", "data": null}` |
| 已經被撤回 | `{"success": false, "message": "該申請已經被撤回", "data": null}` |

---

### 3.5 獲取場地月份日曆視圖

**端點資訊：**
- **方法：** GET
- **路徑：** `/api/bookings/calendar/month`
- **認證：** 非必須 (允許公開訪問)
- **查詢參數：**
  - `venueId` (Long, 必填) - 場地的唯一識別碼，例如 101
  - `year` (Integer, 必填) - 年份（西元年），例如 2026
  - `month` (Integer, 必填) - 月份（1-12），例如 4 表示四月

**功能描述：**

取得指定場地在指定月份的日曆視圖。月視圖展示該月每日是否有已通過的預約和用戶自己的預約，便於用戶直觀瀏覽月份中哪些日期可用。

月視圖主要用於快速瀏覽，不展示詳細時段資訊。詳細時段資訊可查詢周視圖或日視圖。

**返回數據包含：**

1. 該月每日的簡化摘要：
   - `date`：日期（ISO 8601 格式）
   - `hasApprovedBooking`：該日是否有已通過的預約
   - `hasUserBooking`：該日是否有用戶自己的預約

2. 該月所有預約的詳細列表：前端可根據各預約的 `status` 與 `slots` 判斷時段占用情況

**應用場景：**

前端用於展示月份日曆，用戶可快速瀏覽整個月份，點擊具體日期後進一步查看日視圖以了解詳細時段資訊。

**時段占用判斷邏輯：**

- 若 `hasApprovedBooking = true`，表示該日有已通過的預約，部分時段被佔用
- 若 `hasUserBooking = true`，表示該日有用戶自己的預約（無論審核狀態）

**請求示例：**

```bash
curl -X GET "http://localhost:8080/api/bookings/calendar/month?venueId=101&year=2026&month=4"
```

**成功回應 (200 OK)：**

```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "year": 2026,
    "month": 4,
    "days": [
      {
        "date": "2026-04-01",
        "hasApprovedBooking": false,
        "hasUserBooking": false
      },
      {
        "date": "2026-04-02",
        "hasApprovedBooking": true,
        "hasUserBooking": true
      },
      {
        "date": "2026-04-10",
        "hasApprovedBooking": true,
        "hasUserBooking": true
      }
    ],
    "bookings": [
      {
        "id": 501,
        "venueName": "會議室 A",
        "bookingDate": "2026-04-10",
        "slots": [8, 9],
        "status": 2,
        "createdAt": "2026-04-03T10:00:00",
        "purpose": "舉辦專案討論會議",
        "pCount": 5,
        "contactInfo": "{\"name\":\"王小明\",\"phone\":\"0912345678\",\"email\":\"xm@ncu.edu.tw\"}",
        "equipments": ["麥克風", "投影機"]
      },
      {
        "id": 502,
        "venueName": "會議室 A",
        "bookingDate": "2026-04-02",
        "slots": [10, 11, 12],
        "status": 2,
        "createdAt": "2026-04-01T09:00:00",
        "purpose": "主管會議",
        "pCount": 8,
        "contactInfo": "{\"name\":\"李經理\",\"phone\":\"0912111111\",\"email\":\"manager@ncu.edu.tw\"}",
        "equipments": []
      }
    ]
  }
}
```

**參數驗證失敗示例：**

```json
{
  "success": false,
  "message": "月份（1-12）",
  "data": null
}
```

---

### 3.6 獲取場地周份日曆視圖

**端點資訊：**
- **方法：** GET
- **路徑：** `/api/bookings/calendar/week`
- **認證：** 非必須 (允許公開訪問)
- **查詢參數：**
  - `venueId` (Long, 必填) - 場地的唯一識別碼，例如 101
  - `date` (LocalDate, 必填) - 周開始日期（必須為周一，格式：YYYY-MM-DD），例如 2026-04-06

**功能描述：**

取得指定場地在指定周份的日曆視圖。周視圖展示該周每日的詳細時段占用情況，相比月視圖展示每個小時的占用狀態，便於用戶精確查看週內哪些時段可用。

**參數要求：**

- `date` 參數必須為周一日期
- 系統會自動計算周一至周日的七日數據

**返回數據包含：**

1. 周份基本資訊：`weekStart`（周一）、`weekEnd`（周日）
2. 周內每日詳細資訊：
   - `date`：日期
   - `dayOfWeek`：星期幾（中文表示）
   - `approvedSlots`：已通過預約的時段列表（0-23）
   - `userSlots`：用戶自己的預約時段列表（0-23）

**應用場景：**

用戶在月視圖選定日期後，進一步查看周視圖以了解該周每日的詳細時段占用情況，用於判斷各時段是否可用。

**時段索引說明：**

使用 24 小時制索引，0-23 分別表示：
- 0：00:00-01:00
- 1：01:00-02:00
- ...
- 23：23:00-24:00

**請求示例：**

```bash
curl -X GET "http://localhost:8080/api/bookings/calendar/week?venueId=101&date=2026-04-06"
```

**成功回應 (200 OK)：**

```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "weekStart": "2026-04-06",
    "weekEnd": "2026-04-12",
    "days": [
      {
        "date": "2026-04-06",
        "dayOfWeek": "星期一",
        "approvedSlots": [8, 9],
        "userSlots": [8]
      },
      {
        "date": "2026-04-07",
        "dayOfWeek": "星期二",
        "approvedSlots": [10, 11, 12],
        "userSlots": []
      },
      {
        "date": "2026-04-08",
        "dayOfWeek": "星期三",
        "approvedSlots": [],
        "userSlots": []
      },
      {
        "date": "2026-04-09",
        "dayOfWeek": "星期四",
        "approvedSlots": [14, 15],
        "userSlots": [14]
      },
      {
        "date": "2026-04-10",
        "dayOfWeek": "星期五",
        "approvedSlots": [8, 9],
        "userSlots": []
      },
      {
        "date": "2026-04-11",
        "dayOfWeek": "星期六",
        "approvedSlots": [],
        "userSlots": []
      },
      {
        "date": "2026-04-12",
        "dayOfWeek": "星期日",
        "approvedSlots": [],
        "userSlots": []
      }
    ]
  }
}
```

---

### 3.7 獲取場地單日日曆視圖

**端點資訊：**
- **方法：** GET
- **路徑：** `/api/bookings/calendar/day`
- **認證：** 非必須 (允許公開訪問)
- **查詢參數：**
  - `venueId` (Long, 必填) - 場地的唯一識別碼，例如 101
  - `date` (LocalDate, 必填) - 查詢日期（格式：YYYY-MM-DD），例如 2026-04-06

**功能描述：**

取得指定場地在指定日期的日曆視圖。日視圖是最詳細的視圖，包含該日所有預約的詳細資訊（預約狀態、時段、用途等），用戶可進一步決策是否進行新預約。

**返回數據包含：**

1. 場地與日期基本資訊：`venueId`、`venueName`、`date`、`dayOfWeek`
2. 時段占用情況：
   - `approvedSlots`：已通過預約的時段列表
   - `userSlots`：用戶自己的預約時段列表
3. 用戶預約詳情列表：
   - `bookingId`：預約編號
   - `slots`：該預約的時段列表
   - `status`：預約狀態（0/1/2/3）
   - `purpose`：使用用途
   - `createdAt`：申請時間

**應用場景：**

前端在月視圖或周視圖點擊特定日期時，顯示該日最詳細的預約資訊。用戶可查看該日所有預約（含狀態）、利用情況，以及決定是否在空閒時段提交新的預約申請。

**時段索引說明：**

同周視圖，使用 24 小時制索引（0-23）

**請求示例：**

```bash
curl -X GET "http://localhost:8080/api/bookings/calendar/day?venueId=101&date=2026-04-06"
```

**成功回應 (200 OK)：**

```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "venueId": 101,
    "venueName": "會議室 A",
    "date": "2026-04-06",
    "dayOfWeek": "星期一",
    "approvedSlots": [8, 9],
    "userSlots": [8],
    "userBookingDetails": [
      {
        "bookingId": 501,
        "slots": [8],
        "status": 2,
        "purpose": "舉辦專案討論會議",
        "createdAt": "2026-04-03T10:00:00"
      }
    ]
  }
}
```

**空閒日期回應示例：**

```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "venueId": 101,
    "venueName": "會議室 A",
    "date": "2026-04-08",
    "dayOfWeek": "星期三",
    "approvedSlots": [],
    "userSlots": [],
    "userBookingDetails": []
  }
}
```

---

## 四、錯誤處理機制

### 4.1 全局異常處理

當系統發生異常時，會由 `GlobalExceptionHandler` 捕獲並轉化為 `Result.error()` 格式，統一回傳：

| 異常類型 | HTTP 狀態碼 | 回應示例 | 說明 |
| :--- | :--- | :--- | :--- |
| 參數校驗失敗 | 200* | `{"success": false, "message": "場地 ID 不可為空", "data": null}` | 若 DTO 欄位不符規範（如人數為負數、日期是過去），回傳 DTO 定義的驗證訊息 |
| 業務邏輯異常 | 200* | `{"success": false, "message": "該時段已被其他已通過之申請佔用", "data": null}` | 業務層拋出 RuntimeException，顯示具體業務錯誤訊息 |
| 未預期異常 | 200* | `{"success": false, "message": "伺服器內部錯誤，請稍後再試", "data": null}` | 系統捕獲所有未預期的異常，隱藏內部細節，返回通用錯誤訊息 |

*注：本系統統一使用 HTTP 200 狀態碼，通過 Result.success 欄位區分成功與失敗

### 4.2 常見錯誤訊息

#### 參數驗證相關

| 欄位 | 錯誤訊息 | 觸發條件 |
| :--- | :--- | :--- |
| venueId | `"場地 ID 不可為空"` | 未提供或為 null |
| bookingDate | `"預約日期不可為空"` | 未提供或為 null |
| bookingDate | `"預約日期不能是過去的時間"` | 提供的日期早於今天 |
| slots | `"請至少選擇一個預約時段"` | 提供空陣列或未提供 |
| slots | `"請填寫使用用途"` | purpose 為空或未提供 |
| purpose | `"用途描述過長"` | purpose 長度超過 255 字 |
| participantCount | `"預估人數至少需為 1 人"` | 人數 < 1 或為 null |
| contactInfo | `"聯絡資訊不可為空"` | 未提供或為 null |
| contactInfo.name | `"聯絡人姓名不可為空"` | 未提供或為空字串 |
| contactInfo.email | `"電子郵件不正確"` | 郵件格式不符 |
| contactInfo.email | `"電子郵件不可為空"` | 未提供或為空字串 |
| contactInfo.phone | `"聯絡電話不可為空"` | 未提供或為空字串 |

#### 業務邏輯相關

| 錯誤訊息 | 觸發條件 |
| :--- | :--- |
| `"該時段已被其他已通過之申請佔用"` | 所選時段與已通過的預約衝突 |
| `"找不到指定的預約申請"` | 預約不存在 |
| `"已拒絕之申請無法修改"` | 試圖修改已拒絕的申請 |
| `"已撤回之申請無法修改"` | 試圖修改已撤回的申請 |
| `"已拒絕之申請無法撤回"` | 試圖撤回已拒絕的申請 |
| `"該申請已經被撤回"` | 試圖撤回已撤回的申請 |

---

## 五、前後端調用流程示例

### 5.1 用戶提交預約申請流程

```
前端              後端
  │                │
  ├─ 調用 3.1 POST /api/bookings ─→
  │     (提交預約申請)               │
  │                │  驗證參數、檢查衝突
  │                │
  │     ← Result<Long> 返回預約ID ────┤
  │     (201 或錯誤訊息)             │
  │                │
  └─ 調用 3.2 GET /api/bookings/my ─→
        (查看個人預約清單)            │
                  │   返回新建的預約
        ← Result<List<BookingVO>> ────┤
```

### 5.2 用戶查看日曆並修改預約流程

```
前端              後端
  │                │
  ├─ 調用 3.5 GET /api/bookings/calendar/month ─→
  │     (查看月份日曆)                │
  │                │  返回該月所有預約
  │     ← Result<VenueCalendarMonthVO> ────┤
  │                │
  ├─ 調用 3.7 GET /api/bookings/calendar/day ─→
  │     (查看具體日期詳情)             │
  │                │  返回該日詳細預約
  │     ← Result<VenueCalendarDayVO> ────┤
  │                │
  ├─ 調用 3.3 PUT /api/bookings/{id} ─→
  │     (修改預約)                    │
  │                │  驗證並更新預約
  │     ← Result<Void> 返回成功/失敗 ────┤
  │                │
```

---

## 六、技術規範

### 6.1 時間格式

- **日期格式**：ISO 8601 (YYYY-MM-DD)，例如 `2026-04-10`
- **日期時間格式**：ISO 8601 (YYYY-MM-DDTHH:mm:ss)，例如 `2026-04-03T10:00:00`
- **時段表示**：24 小時制索引 (0-23)，例如 8 表示 08:00-09:00 時段

### 6.2 認證方式

所有涉及用戶個人資料的 API（建立、修改、撤回、查詢個人預約）均需在 HTTP Header 中提供 Mock Authorization Token：

```
Authorization: mock-token-123
```

日曆視圖查詢 API 無需認證（允許公開訪問）。

### 6.3 HTTP 狀態碼規範

本系統統一使用 **HTTP 200** 狀態碼進行所有回應，通過 `Result.success` 欄位區分成功與失敗：

```
成功：HTTP 200 + success=true
失敗：HTTP 200 + success=false
```

此設計的優點是簡化前端錯誤處理邏輯，所有業務異常都由 `success` 和 `message` 欄位反映。

### 6.4 Content-Type

所有 POST/PUT 請求應指定：

```
Content-Type: application/json
```

---

## 七、最佳實踐建議

### 7.1 前端實現建議

1. **月視圖→周視圖→日視圖漸進式展開**：
   - 首先展示月份日曆（3.5），讓用戶快速瀏覽
   - 點擊月份上的日期進入周視圖（3.6）查看該周詳情
   - 再點擊週內特定日期進入日視圖（3.7）查看最詳細資訊

2. **時段衝突實時檢查**：
   - 在日視圖中展示 `approvedSlots`，視覺化標記已被佔用的時段
   - 用戶選擇時段時實時檢查是否衝突

3. **預約狀態視覺化**：
   - 使用不同顏色/圖標區分預約狀態：0=灰色、1=黃色、2=綠色、3=紅色

4. **聯絡資訊存儲**：
   - `contactInfo` 以 JSON 字串形式存儲，需在前端序列化/反序列化

### 7.2 後端實現建議

1. **樂觀鎖保護**：在更新預約時添加版本控制，防止併發修改衝突

2. **時段衝突檢查優化**：在數據庫層使用位元運算或 SQL 判斷，而非在應用層過濾

3. **非同步通知**：預約狀態變更時異步發送郵件/簡訊通知用戶

---

## 八、常見問題 (FAQ)

**Q: 如何判斷某個時段是否可預約？**

A: 比對 `approvedSlots`。若時段不在此列表中，則該時段可預約。

**Q: 修改預約後為什麼狀態變為「審核中」？**

A: 設計考慮是防止修改後的時段安排與其他預約衝突，需由審核者重新評估。

**Q: 時段索引 0-23 如何對應實際時間？**

A: 索引 n 表示 n:00 到 (n+1):00 的時段。例如 8 表示 08:00-09:00。

**Q: 「已撤回」的預約能否重新激活？**

A: 不能。已撤回的預約是終態，用戶如需相同預約需重新提交申請。

---

## 九、版本歷史

| 版本 | 日期 | 變更內容 |
| :--- | :--- | :--- |
| V1.0 | 2026-04-03 | 初版文檔 |
| V2.0 | 2026-04-11 | 添加 Swagger 註解、詳細 API 說明、最佳實踐建議 |


