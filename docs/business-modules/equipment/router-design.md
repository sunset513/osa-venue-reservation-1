# Equipment Router Design

來源：

- `venue_seed_v2.sql`
- 設備獨立借用功能修改指南

## Equipment Master Routes

Base path：`/api/equipments`

| Method | Path | Auth | 功能 |
| --- | --- | --- | --- |
| GET | `/api/equipments` | User | 查詢可借設備與場地限制規則。 |
| GET | `/api/equipments/status` | User | 查詢指定日期/小時所有設備目前借出狀態與 active booking。 |
| GET | `/api/equipments/{id}` | User | 查詢設備詳情。 |
| POST | `/api/equipments` | Admin | 新增設備。 |
| PUT | `/api/equipments/{id}` | Admin | 修改設備主檔。 |
| DELETE | `/api/equipments/{id}` | Admin | 軟刪除設備。 |
| PUT | `/api/equipments/{id}/restore` | Admin | 恢復已軟刪除設備。 |
| PUT | `/api/equipments/{id}/venue-rules` | Admin | 更新設備允許場地規則。 |

目前系統使用複數資源名稱 `/api/equipments`；舊 endpoint `/api/equipment` 不再作為主要對接面。

## Equipment Booking Routes

Base path：`/api/equipment-bookings`

| Method | Path | Auth | 功能 |
| --- | --- | --- | --- |
| POST | `/api/equipment-bookings` | User | 建立設備借用申請。 |
| POST | `/api/equipment-bookings/query` | User | 條件與分頁查詢自己的設備申請。 |
| GET | `/api/equipment-bookings/{id}` | User | 查詢自己的設備申請詳情。 |
| PUT | `/api/equipment-bookings/{id}` | User | 修改自己的設備申請。 |
| PUT | `/api/equipment-bookings/{id}/withdraw` | User | 撤回自己的設備申請。 |
| POST | `/api/equipment-bookings/availability` | User | 查詢設備可用性與場地規則有效性。 |

## Equipment Review Routes

Base path：`/api/equipment-reviews`

| Method | Path | Auth | 功能 |
| --- | --- | --- | --- |
| POST | `/api/equipment-reviews/query` | Admin | 管理端條件與分頁查詢設備申請。 |
| GET | `/api/equipment-reviews/by-venue-booking/{bookingId}` | Admin | 查詢指定場地預約關聯的設備申請。 |
| GET | `/api/equipment-reviews/standalone/pending-count` | Admin | 查詢待審核的單獨設備借用數量，供 review badge 使用。 |
| GET | `/api/equipment-reviews/{id}` | Admin | 管理端查看設備申請詳情。 |
| PUT | `/api/equipment-reviews/{id}/approve` | Admin | 核准設備申請。 |
| PUT | `/api/equipment-reviews/{id}/status` | Admin | 彈性更新設備審核狀態為審核中、已通過或已拒絕。 |
| PUT | `/api/equipment-reviews/{id}/reject` | Admin | 拒絕設備申請。 |

目前沒有提供設備申請刪除 endpoint；`status=4` 是資料模型保留狀態。

## Booking Integration Route

Base path：`/api/bookings`

| Method | Path | Auth | 功能 |
| --- | --- | --- | --- |
| POST | `/api/bookings/with-equipments` | User | 在同一交易中建立場地預約與關聯設備借用申請。 |

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

### Update Equipment Review Status

```json
{
  "status": 3
}
```

### Approve Equipment Booking

不需 request body；後端會以目前資料庫版本進行狀態更新。

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
  "data": {
    "available": true,
    "message": "設備可借用",
    "items": [
      {
        "equipmentId": 1,
        "equipmentName": "麥克風",
        "requestedQuantity": 2,
        "totalQuantity": 4,
        "minAvailableQuantity": 2,
        "available": true,
        "venueRulePassed": true,
        "message": null
      }
    ]
  }
}
```

## 權限

目前後端 interceptor 已明確限制：

- `/api/reviews/**` 需要 ADMIN。
- `/api/admin-roles/**` 需要 level 1 admin。
- `/api/equipment-reviews/**` 需要 ADMIN。
- `/api/equipments` 的 `POST`、`PUT`、`DELETE` 需要 ADMIN。

不能只依賴前端隱藏按鈕。
