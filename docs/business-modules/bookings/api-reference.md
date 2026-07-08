# Bookings API Reference

本文補充 booking 模組目前需要前端直接對接的 API。所有 API 回應皆使用系統通用格式：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {}
}
```

## Approved Bookings For Three Venues

### GET `/api/bookings/approved/three-venues`

公開查詢指定日期三個場地的已通過預約，供活動資訊頁顯示目前與即將開始的活動。

Query parameters：

| Name | Required | Type | 說明 |
| --- | --- | --- | --- |
| `venueIdA` | Yes | long | 第一個場地 ID。 |
| `venueIdB` | Yes | long | 第二個場地 ID。 |
| `venueIdC` | Yes | long | 第三個場地 ID。 |
| `date` | Yes | date | 查詢日期，格式 `YYYY-MM-DD`。 |

Rules：

- 三個場地 ID 不可為空。
- 三個場地不可重複。
- 三個場地都必須存在。
- 只回傳 `status=2` 的已通過場地預約。

Returns：`data` 為 `ApprovedBookingsByVenueVO[]`。回傳格式維持與原兩場地版本相同，只是分組數量固定為三組。

```json
[
  {
    "venueId": 1,
    "venueName": "會議室",
    "items": [
      {
        "bookingId": 101,
        "slots": [9, 10],
        "purpose": "社團活動"
      }
    ]
  },
  {
    "venueId": 2,
    "venueName": "交誼廳",
    "items": []
  },
  {
    "venueId": 3,
    "venueName": "學務長會議室",
    "items": []
  }
]
```

Notes：

- 原 `/api/bookings/approved/two-venues` 已改為 `/api/bookings/approved/three-venues`。
- 前端活動資訊頁只需要改打新 endpoint，並多傳 `venueIdC`；攤平 venue groups 的資料格式不用改。
