# Bookings Router Design

來源：

- `backend/venue-reservation-service/src/main/java/tw/edu/ncu/osa/venue_reservation_service/controller/BookingController.java`
- `backend/venue-reservation-service/src/main/java/tw/edu/ncu/osa/venue_reservation_service/controller/ReviewController.java`

## BookingController

Base path：`/api/bookings`

| Method | Path | Auth | 功能 |
| --- | --- | --- | --- |
| POST | `/api/bookings` | Yes | 建立預約申請。 |
| GET | `/api/bookings/my` | Yes | 查詢目前登入使用者的所有預約。 |
| POST | `/api/bookings/query` | Yes | 多維度篩選並分頁查詢個人預約。 |
| PUT | `/api/bookings/{id}` | Yes | 修改個人預約。 |
| PUT | `/api/bookings/{id}/withdraw` | Yes | 撤回個人預約。 |
| GET | `/api/bookings/calendar/month` | Not marked | 查詢指定場地月曆視圖。 |
| GET | `/api/bookings/calendar/week` | Not marked | 查詢指定場地周曆視圖。 |
| GET | `/api/bookings/calendar/day` | Not marked | 查詢指定場地日曆視圖。 |
| GET | `/api/bookings/approved/three-venues` | Not marked | 查詢指定日期三個場地的已通過預約。 |

## ReviewController

Base path：`/api/reviews`

| Method | Path | Auth | 功能 |
| --- | --- | --- | --- |
| GET | `/api/reviews/pending` | Yes | 管理端查詢預約列表。 |
| GET | `/api/reviews/bookings/{id}` | Yes | 管理端查詢預約詳情。 |
| POST | `/api/reviews/bookings/{id}/approve` | Yes | 審核通過預約。 |
| PUT | `/api/reviews/bookings/{id}/status` | Yes | 更新預約審核狀態。 |
| DELETE | `/api/reviews/bookings/{id}` | Yes | 軟刪除預約案。 |

`Auth` 欄位依 controller 是否標註 `@SecurityRequirement(name = "Session-Cookie")` 記錄。實際登入攔截仍需以專案 security/filter 設定為準。

## Request

### POST /api/bookings

使用 `BookingRequestDTO`：

```json
{
  "venueId": 1,
  "bookingDate": "2026-07-10",
  "slots": [8, 9],
  "purpose": "專案討論",
  "participantCount": 5,
  "contactInfo": {
    "name": "王小明",
    "email": "xm@ncu.edu.tw",
    "phone": "0912345678"
  },
  "equipmentIds": [1, 2]
}
```

Response `data` 為新建預約 ID。

### POST /api/bookings/query

使用 `BookingQueryDTO`：

```json
{
  "venueId": 1,
  "statusList": [1, 2],
  "startDate": "2026-07-01",
  "endDate": "2026-07-31",
  "pageNo": 1,
  "pageSize": 20
}
```

Response `data` 為 `BookingPageVO`。

### PUT /api/bookings/{id}

使用 `BookingRequestDTO`。修改成功後 response `data` 為 `null`。

### PUT /api/bookings/{id}/withdraw

無 request body。撤回成功後 response `data` 為 `null`。

### Calendar Query

`GET /api/bookings/calendar/month`

- `venueId`
- `year`
- `month`

`GET /api/bookings/calendar/week`

- `venueId`
- `date`：必須為星期一。

`GET /api/bookings/calendar/day`

- `venueId`
- `date`

### GET /api/bookings/approved/three-venues

Query：

- `venueIdA`
- `venueIdB`
- `venueIdC`
- `date`

三個場地不可相同。

## Review Request

### GET /api/reviews/pending

Query：

- `venueId`：未提供時預設 `1`。
- `startDate`：未提供完整日期範圍時預設當月初。
- `endDate`：未提供完整日期範圍時預設當月末。
- `status`：可選，範圍 `0-3`。

### PUT /api/reviews/bookings/{id}/status

使用 `ReviewRequestDTO`：

```json
{
  "status": 3
}
```

Controller 會將 path `{id}` 寫入 `request.bookingId`。

## Response Wrapper

所有 endpoint 使用 `Result<T>` 包裝：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {}
}
```

失敗時由 service/controller 拋出例外，實際 response 格式依全域 exception handler 決定。
