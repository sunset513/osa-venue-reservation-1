# Equipment API Reference

本文記錄設備獨立借用模組後端 API。所有 API 回應皆使用系統通用格式：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {}
}
```

## Status

設備借用申請 `status`：

| Value | Name | 說明 |
| --- | --- | --- |
| `0` | withdrawn | 使用者撤回 |
| `1` | pending | 審核中 |
| `2` | approved | 已通過 |
| `3` | rejected | 已拒絕 |
| `4` | deleted | 系統刪除 |

## Permission Summary

- 一般登入使用者可查詢設備清單、設備狀態、建立/查詢/修改/撤回自己的設備借用申請。
- `/api/equipments` 的寫入端點（`POST`、`PUT`、`DELETE`）需要 admin role。
- `/api/equipment-reviews/**` 需要 admin role。
- 目前設備審核端沒有提供刪除設備申請的 API；`status=4` 僅作為資料模型保留狀態。

## Equipment Master

Base path：`/api/equipments`

### GET `/api/equipments`

查詢設備主檔清單。

Query parameters：

| Name | Required | Type | 說明 |
| --- | --- | --- | --- |
| `includeDeleted` | No | boolean | 是否包含已軟刪除設備，預設 `false`。 |

Returns：`data` 為 `EquipmentVO[]`。

```json
[
  {
    "id": 1,
    "name": "無線麥克風",
    "totalQuantity": 4,
    "description": "可攜式無線麥克風",
    "borrowNote": "請於活動結束後歸還",
    "venueRestricted": false,
    "allowedVenues": [],
    "deletedAt": null,
    "createdAt": "2026-07-04T10:00:00",
    "updatedAt": "2026-07-04T10:00:00"
  }
]
```

### GET `/api/equipments/{id}`

查詢單一設備詳情。

Path parameters：

| Name | Required | Type | 說明 |
| --- | --- | --- | --- |
| `id` | Yes | long | 設備 ID。 |

Returns：`data` 為 `EquipmentVO`。

### GET `/api/equipments/status`

查詢所有設備在目前時間或指定日期/小時的借出狀態，供 `EquipmentStatus.vue` 類型頁面查看每項設備是否出借，以及出借中的申請資訊。

Query parameters：

| Name | Required | Type | 說明 |
| --- | --- | --- | --- |
| `date` | No | date | 查詢日期，格式 `YYYY-MM-DD`；未提供時使用系統當日。 |
| `hour` | No | integer | 查詢小時，`0` 到 `23`；未提供時使用系統目前小時。 |

Returns：`data` 為 `EquipmentStatusVO[]`。

```json
[
  {
    "equipmentId": 1,
    "equipmentName": "無線麥克風",
    "totalQuantity": 4,
    "borrowedQuantity": 2,
    "availableQuantity": 2,
    "inUse": true,
    "activeBookings": [
      {
        "equipmentBookingId": 8001,
        "userId": "110123456",
        "borrowDate": "2026-07-10",
        "slots": [9, 10],
        "quantity": 2,
        "purpose": "社團活動",
        "contactInfo": "{\"name\":\"王小明\",\"phone\":\"0912345678\"}",
        "relatedVenueBookingId": 501,
        "relatedVenueId": 1,
        "relatedVenueName": "會議室"
      }
    ]
  }
]
```

### POST `/api/equipments`

新增設備主檔。需管理員權限。

Request body：

```json
{
  "name": "無線麥克風",
  "totalQuantity": 4,
  "description": "可攜式無線麥克風",
  "borrowNote": "請於活動結束後歸還",
  "venueRules": [
    {
      "venueId": 1,
      "ruleNote": "僅限會議室使用"
    }
  ]
}
```

Rules：

| Field | Required | 說明 |
| --- | --- | --- |
| `name` | Yes | 設備名稱，需唯一。 |
| `totalQuantity` | Yes | 同一時段可核准借出的總數量，至少 `1`。 |
| `description` | No | 設備說明。 |
| `borrowNote` | No | 借用注意事項。 |
| `venueRules` | No | 允許場地規則；`null` 或空陣列表示不限場地。 |

Returns：`data` 為新設備 ID。

### PUT `/api/equipments/{id}`

修改設備主檔。需管理員權限。

Path parameters：`id` 為設備 ID。

Request body 欄位同新增；所有欄位皆可選，`venueRules` 只有在 body 中提供時才會重設規則。

Returns：`data` 為 `null`。

### DELETE `/api/equipments/{id}`

軟刪除設備。需管理員權限。

Rules：若該設備仍有今日以後的審核中或已通過借用申請，拒絕刪除。

Returns：`data` 為 `null`。

### PUT `/api/equipments/{id}/restore`

恢復已軟刪除設備。需管理員權限。

Returns：`data` 為 `null`。

### PUT `/api/equipments/{id}/venue-rules`

更新設備允許場地規則。需管理員權限。

Request body：

```json
[
  {
    "venueId": 1,
    "ruleNote": "投影機僅限會議室"
  }
]
```

Rules：空陣列或 `null` 代表清空限制，設備可不限場地借用。

Returns：`data` 為 `null`。

## Venue Booking With Equipments

Base path：`/api/bookings`

### POST `/api/bookings/with-equipments`

合併建立場地預約與關聯設備借用申請。此端點用於「使用者在場地預約表單中同時選擇設備與數量」的流程，後端會在同一個 transaction 中先建立場地預約，再建立 `relatedVenueBookingId` 指向該場地預約的設備借用申請；任一段失敗皆回滾。

Request body：

```json
{
  "booking": {
    "venueId": 1,
    "bookingDate": "2026-07-10",
    "slots": [9, 10],
    "purpose": "社團活動",
    "participantCount": 30,
    "contactInfo": {
      "name": "王小明",
      "phone": "0912345678"
    }
  },
  "equipmentItems": [
    {
      "equipmentId": 1,
      "quantity": 2
    }
  ]
}
```

Rules：

| Field | Required | 說明 |
| --- | --- | --- |
| `booking` | Yes | 場地預約資料，格式同既有建立場地預約 API。 |
| `equipmentItems` | No | 欲一併借用的設備與數量；空陣列或未提供時只建立場地預約。 |

設備借用會沿用 `booking.bookingDate`、`booking.slots`、`booking.purpose`、`booking.contactInfo`，並自動填入 `relatedVenueBookingId`。若設備數量不足或場地限制不符，整筆合併建立會失敗。

Returns：`data` 為 `BookingWithEquipmentCreateVO`。未選設備時只建立場地預約，`equipmentBookingId` 會是 `null`。

```json
{
  "bookingId": 501,
  "equipmentBookingId": 8001
}
```

## Equipment Bookings

Base path：`/api/equipment-bookings`

### POST `/api/equipment-bookings`

建立設備借用申請。

Request body：

```json
{
  "borrowDate": "2026-07-10",
  "slots": [9, 10],
  "purpose": "社團活動",
  "contactInfo": {
    "name": "王小明",
    "phone": "0912345678"
  },
  "relatedVenueBookingId": 501,
  "items": [
    {
      "equipmentId": 1,
      "quantity": 2
    }
  ]
}
```

Rules：

| Field | Required | 說明 |
| --- | --- | --- |
| `borrowDate` | Yes | 借用日期。 |
| `slots` | Yes | 借用時段，0-23 小時索引。 |
| `purpose` | Yes | 借用用途。 |
| `contactInfo` | Yes | 聯絡資訊物件；後端會序列化為 JSON 存入資料庫。 |
| `relatedVenueBookingId` | Conditional | 借用限場地設備時必填。 |
| `items` | Yes | 借用設備與數量；同設備重複出現會加總。 |

若設備有場地限制，相關場地預約需屬於目前使用者、日期相同、時段涵蓋設備借用時段，且場地符合該設備規則。

Returns：`data` 為新設備借用申請 ID。

### GET `/api/equipment-bookings/{id}`

查詢自己的設備借用詳情。

Returns：`data` 為 `EquipmentBookingVO`。

```json
{
  "id": 8001,
  "userId": "110123456",
  "borrowDate": "2026-07-10",
  "slots": [9, 10],
  "status": 1,
  "purpose": "社團活動",
  "contactInfo": "{\"name\":\"王小明\",\"phone\":\"0912345678\"}",
  "relatedVenueBookingId": 501,
  "relatedVenueId": 1,
  "relatedVenueName": "會議室",
  "reviewedBy": null,
  "reviewedAt": null,
  "version": 1,
  "items": [
    {
      "id": 9001,
      "equipmentId": 1,
      "equipmentName": "無線麥克風",
      "quantity": 2
    }
  ],
  "createdAt": "2026-07-04T10:00:00",
  "updatedAt": "2026-07-04T10:00:00"
}
```

### POST `/api/equipment-bookings/query`

查詢自己的設備借用列表。

Request body：

```json
{
  "statusList": [1, 2],
  "startDate": "2026-07-01",
  "endDate": "2026-07-31",
  "equipmentId": 1,
  "relatedVenueBookingId": 501,
  "standaloneOnly": false,
  "pageNo": 1,
  "pageSize": 20
}
```

All fields optional. `pageNo` 預設 `1`，`pageSize` 預設 `20`，最大 `100`。

Additional filters：

| Field | Type | 說明 |
| --- | --- | --- |
| `relatedVenueBookingId` | long | 只查詢關聯指定場地預約的設備借用。 |
| `standaloneOnly` | boolean | `true` 時只查詢沒有 `relatedVenueBookingId` 的單獨設備借用。 |

Returns：`data` 為 `EquipmentBookingPageVO`。

### PUT `/api/equipment-bookings/{id}`

修改自己的設備借用申請。

Rules：僅 `pending(1)` 或 `approved(2)` 可修改；修改後狀態重設為 `pending(1)`，審核欄位清空。

Request body 欄位同建立；目前前端不需要帶 `version`。

Returns：`data` 為 `null`。

### PUT `/api/equipment-bookings/{id}/withdraw`

撤回自己的設備借用申請。

Rules：僅 `pending(1)` 或 `approved(2)` 可撤回。

Returns：`data` 為 `null`。

### POST `/api/equipment-bookings/availability`

檢查設備可用量與場地規則。

Request body：

```json
{
  "borrowDate": "2026-07-10",
  "slots": [9, 10],
  "relatedVenueBookingId": 501,
  "excludeEquipmentBookingId": null,
  "items": [
    {
      "equipmentId": 1,
      "quantity": 2
    }
  ]
}
```

Returns：`data` 為 `EquipmentAvailabilityVO`。

```json
{
  "available": true,
  "message": "設備可借用",
  "items": [
    {
      "equipmentId": 1,
      "equipmentName": "無線麥克風",
      "requestedQuantity": 2,
      "totalQuantity": 4,
      "minAvailableQuantity": 4,
      "available": true,
      "venueRulePassed": true,
      "message": null
    }
  ]
}
```

## Equipment Reviews

Base path：`/api/equipment-reviews`

所有端點需管理員權限。

### POST `/api/equipment-reviews/query`

管理端查詢設備借用申請列表。

Request body 同 `/api/equipment-bookings/query`，但查詢範圍為所有使用者。

Returns：`data` 為 `EquipmentBookingPageVO`。

### GET `/api/equipment-reviews/by-venue-booking/{bookingId}`

管理端查詢指定場地預約關聯的設備借用申請。此端點用於 `/review` 場地審核詳情中，一併帶出該場地預約底下的設備申請。

Path parameters：

| Name | Required | Type | 說明 |
| --- | --- | --- | --- |
| `bookingId` | Yes | long | 場地預約 ID。 |

Returns：`data` 為 `EquipmentBookingVO[]`。

### GET `/api/equipment-reviews/standalone/pending-count`

管理端查詢待審核的單獨設備借用申請數量。此端點用於 review 介面的切換按鈕徽章數。

Returns：`data` 為 number。

```json
3
```

### GET `/api/equipment-reviews/{id}`

管理端查詢設備借用詳情。

Returns：`data` 為 `EquipmentBookingVO`。

### PUT `/api/equipment-reviews/{id}/approve`

核准設備借用申請。

Rules：

- 可將設備申請改為 `approved(2)`。
- 核准時會重新檢查設備總量。
- 若設備有限場地規則，相關場地預約必須已通過，日期與時段也需符合規則。
- 成功後狀態改為 `approved(2)`，寫入 `reviewedBy` 與 `reviewedAt`。
- 已撤回的設備借用申請 `withdrawn(0)` 不可由審核端重新啟用。
- 成功核准後，後端會檢查其他同日期、時段重疊、設備有交集的 `pending(1)` 申請；若該 pending 申請因目前已核准數量而不可核准，會自動改為 `rejected(3)`。

Returns：`data` 為 `null`。

### PUT `/api/equipment-reviews/{id}/status`

更新設備借用審核狀態。此端點供審核者在核准後改為拒絕、拒絕後改為核准或退回審核中使用，行為比照場地預約審核的彈性狀態切換。

Request body：

```json
{
  "status": 2
}
```

Rules：

- `status` 可為 `1(pending)`、`2(approved)`、`3(rejected)`。
- 已撤回的設備借用申請 `withdrawn(0)` 不可由審核端重新啟用。
- 目標狀態為 `approved(2)` 時，會重新檢查設備總量與場地規則。
- 目標狀態為 `approved(2)` 且更新成功後，會自動拒絕因設備數量不足而不可核准的衝突 pending 設備申請。
- 成功後寫入 `reviewedBy`、`reviewedAt`，並透過 `version` 做樂觀鎖檢查。
- 不寫入拒絕原因；承辦人若需說明，會於系統外通知使用者。

Returns：`data` 為 `null`。

### PUT `/api/equipment-reviews/{id}/reject`

拒絕設備借用申請。

Rules：可將設備申請改為 `rejected(3)`，不需要 request body，也不寫入拒絕原因。

Returns：`data` 為 `null`。
