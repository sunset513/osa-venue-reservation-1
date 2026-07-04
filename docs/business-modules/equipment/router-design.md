# Equipment Router Design

來源：

- `venue_seed_v2.sql`
- 設備獨立借用功能修改指南

## Equipment Master Routes

建議 base path：`/api/equipments`

| Method | Path | Auth | 功能 |
| --- | --- | --- | --- |
| GET | `/api/equipments` | User | 查詢可借設備與場地限制規則。 |
| GET | `/api/equipments/{id}` | User | 查詢設備詳情。 |
| POST | `/api/equipments` | Admin / Level 1 | 新增設備。 |
| PUT | `/api/equipments/{id}` | Admin / Level 1 | 修改設備主檔。 |
| DELETE | `/api/equipments/{id}` | Admin / Level 1 | 軟刪除設備。 |
| PUT | `/api/equipments/{id}/restore` | Admin / Level 1 | 恢復已軟刪除設備。 |
| PUT | `/api/equipments/{id}/venue-rules` | Admin / Level 1 | 更新設備允許場地規則。 |

目前舊 endpoint `/api/equipment` 可在過渡期保留相容，但新設計建議使用複數資源名稱 `/api/equipments`。

## Equipment Booking Routes

建議 base path：`/api/equipment-bookings`

| Method | Path | Auth | 功能 |
| --- | --- | --- | --- |
| POST | `/api/equipment-bookings` | User | 建立設備借用申請。 |
| GET | `/api/equipment-bookings/my` | User | 查詢自己的設備申請。 |
| POST | `/api/equipment-bookings/query` | User | 條件與分頁查詢自己的設備申請。 |
| GET | `/api/equipment-bookings/{id}` | User | 查詢自己的設備申請詳情。 |
| PUT | `/api/equipment-bookings/{id}` | User | 修改自己的設備申請。 |
| PUT | `/api/equipment-bookings/{id}/withdraw` | User | 撤回自己的設備申請。 |
| POST | `/api/equipment-bookings/availability` | User | 查詢設備可用性與場地規則有效性。 |

## Equipment Review Routes

建議 base path：`/api/equipment-reviews`

| Method | Path | Auth | 功能 |
| --- | --- | --- | --- |
| GET | `/api/equipment-reviews` | Admin | 管理端查詢設備申請。 |
| GET | `/api/equipment-reviews/{id}` | Admin | 管理端查看設備申請詳情。 |
| PUT | `/api/equipment-reviews/{id}/approve` | Admin | 核准設備申請。 |
| PUT | `/api/equipment-reviews/{id}/reject` | Admin | 拒絕設備申請。 |
| DELETE | `/api/equipment-reviews/{id}` | Admin | 將設備申請改為 `status=4`。 |

## Booking Helper Route

設備借用頁從場地預約跳轉時，需要取得場地預約資料作為預填內容。

建議新增：

| Method | Path | Auth | 功能 |
| --- | --- | --- | --- |
| GET | `/api/bookings/{id}` | User | 查詢自己的單筆場地預約詳情。 |

一般使用者只能查自己的場地預約。管理端若需查任意場地預約，仍使用 review API。

## Request Examples

### Create Equipment Booking

```json
{
  "borrowDate": "2026-07-20",
  "slots": [9, 10, 11],
  "purpose": "社團成果發表",
  "contactInfo": {
    "name": "王小明",
    "email": "student@ncu.edu.tw",
    "phone": "0912345678"
  },
  "relatedVenueBookingId": 123,
  "items": [
    {
      "equipmentId": 1,
      "quantity": 2
    },
    {
      "equipmentId": 2,
      "quantity": 1
    }
  ]
}
```

若 items 包含受場地限制設備，例如投影機白/黑，`relatedVenueBookingId` 必填。

### Availability Query

```json
{
  "borrowDate": "2026-07-20",
  "slots": [9, 10, 11],
  "relatedVenueBookingId": 123,
  "excludeEquipmentBookingId": null,
  "items": [
    {
      "equipmentId": 1,
      "quantity": 2
    },
    {
      "equipmentId": 2,
      "quantity": 1
    }
  ]
}
```

### Reject Equipment Booking

```json
{
  "version": 3,
  "reason": "該時段設備已無可借數量"
}
```

### Approve Equipment Booking

```json
{
  "version": 3
}
```

### Update Venue Rules

有限制場地：

```json
{
  "venueRules": [
    {
      "venueId": 1,
      "ruleNote": "僅限學務處本部會議室使用。"
    }
  ]
}
```

不限場地：

```json
{
  "venueRules": []
}
```

## Response Examples

### Equipment List

```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "id": 2,
      "name": "投影機(白)",
      "totalQuantity": 1,
      "description": "白色無線投影設備",
      "borrowNote": "僅限學務處本部會議室使用",
      "venueRestricted": true,
      "allowedVenues": [
        {
          "venueId": 1,
          "venueName": "會議室",
          "ruleNote": "投影機(白)僅限學務處本部會議室使用。"
        }
      ]
    }
  ]
}
```

### Availability

```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "equipmentId": 1,
      "equipmentName": "麥克風",
      "totalQuantity": 4,
      "minimumAvailableQuantity": 2,
      "requestedQuantity": 2,
      "available": true,
      "venueRuleValid": true,
      "message": null
    }
  ]
}
```

## 權限

目前後端 interceptor 已明確限制：

- `/api/reviews/**` 需要 ADMIN。
- `/api/admin-roles/**` 需要 level 1 admin。

重構後需補上：

- `/api/equipment-reviews/**` 需要 ADMIN。
- 設備主檔寫入 API 需要管理員權限，實際採一般 admin 或 level 1 admin 需依政策定案。

不能只依賴前端隱藏按鈕。
