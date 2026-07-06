# Equipment Schema Design

來源：

- `venue_seed_v2.sql`
- 設備獨立借用功能修改指南

## Schema Groups

建議將 schema 分為四組：

- 設備主檔：查詢、新增、修改、恢復、場地規則。
- 設備借用：建立、修改、查詢、詳情、撤回。
- 設備可用性：查詢指定日期、時段、設備數量是否可借。
- 設備審核：核准、拒絕、刪除、管理端查詢。

## Equipment Master DTO

### EquipmentCreateDTO

| 欄位 | 必填 | 說明 |
| --- | --- | --- |
| `name` | Yes | 設備名稱。 |
| `totalQuantity` | Yes | 設備總數量，需大於等於 1。 |
| `description` | No | 設備介紹。 |
| `borrowNote` | No | 借用方式與注意事項。 |
| `venueRules` | No | 允許場地規則，空陣列代表不限場地。 |

### EquipmentUpdateDTO

| 欄位 | 必填 | 說明 |
| --- | --- | --- |
| `name` | No | 設備名稱。 |
| `totalQuantity` | No | 設備總數量；降低數量時需檢查未來已通過申請最大用量。 |
| `description` | No | 設備介紹。 |
| `borrowNote` | No | 借用方式與注意事項。 |
| `venueRules` | No | 若提供則整批取代既有規則。 |

### EquipmentVenueRuleDTO

| 欄位 | 必填 | 說明 |
| --- | --- | --- |
| `venueId` | Yes | 允許使用的場地 ID。 |
| `ruleNote` | No | 規則說明。 |

## Equipment Booking DTO

### EquipmentBookingCreateDTO

| 欄位 | 必填 | 說明 |
| --- | --- | --- |
| `borrowDate` | Yes | 借用日期。 |
| `slots` | Yes | 時段列表，元素範圍 `0-23`。 |
| `purpose` | Yes | 借用用途，最多 255 字。 |
| `contactInfo` | Yes | 聯絡人資訊。 |
| `relatedVenueBookingId` | No | 關聯場地預約 ID；若包含受限制設備則必填。 |
| `items` | Yes | 申請設備明細，至少一筆。 |

### EquipmentBookingUpdateDTO

| 欄位 | 必填 | 說明 |
| --- | --- | --- |
| `borrowDate` | Yes | 借用日期。 |
| `slots` | Yes | 時段列表。 |
| `purpose` | Yes | 借用用途。 |
| `contactInfo` | Yes | 聯絡人資訊。 |
| `relatedVenueBookingId` | No | 關聯場地預約 ID。 |
| `items` | Yes | 申請設備明細。 |

### EquipmentBookingItemDTO

| 欄位 | 必填 | 說明 |
| --- | --- | --- |
| `equipmentId` | Yes | 設備 ID。 |
| `quantity` | Yes | 申請數量，需大於 0。 |

## Review DTO

### EquipmentReviewStatusUpdateDTO

| 欄位 | 必填 | 說明 |
| --- | --- | --- |
| `status` | Yes | 目標審核狀態：`1=審核中`、`2=已通過`、`3=已拒絕`。 |

## Availability DTO

### EquipmentAvailabilityQueryDTO

| 欄位 | 必填 | 說明 |
| --- | --- | --- |
| `borrowDate` | Yes | 借用日期。 |
| `slots` | Yes | 借用時段。 |
| `relatedVenueBookingId` | No | 關聯場地預約 ID，用於檢查設備場地限制。 |
| `excludeEquipmentBookingId` | No | 修改既有申請時計算可用量需排除自身。 |
| `items` | Yes | 欲查詢的設備與數量。 |

## Response VO

### EquipmentVO

| 欄位 | 說明 |
| --- | --- |
| `id` | 設備 ID。 |
| `name` | 設備名稱。 |
| `totalQuantity` | 總數量。 |
| `description` | 設備介紹。 |
| `borrowNote` | 借用注意事項。 |
| `venueRestricted` | 是否限制場地。 |
| `allowedVenues` | 允許場地清單。 |
| `deletedAt` | 軟刪除時間。 |
| `createdAt` | 建立時間。 |
| `updatedAt` | 更新時間。 |

### EquipmentBookingVO

| 欄位 | 說明 |
| --- | --- |
| `id` | 設備申請 ID。 |
| `userId` | 申請人 Portal ID。 |
| `borrowDate` | 借用日期。 |
| `slots` | 借用時段列表。 |
| `status` | 申請狀態。 |
| `purpose` | 借用用途。 |
| `contactInfo` | 聯絡資訊。 |
| `relatedVenueBookingId` | 關聯場地預約 ID。 |
| `relatedVenueName` | 關聯場地名稱。 |
| `relatedVenueStatus` | 關聯場地預約狀態。 |
| `items` | 設備明細。 |
| `reviewedBy` | 審核人員。 |
| `reviewedAt` | 審核時間。 |
| `version` | 版本號。 |
| `createdAt` | 建立時間。 |
| `updatedAt` | 更新時間。 |

### EquipmentAvailabilityVO

| 欄位 | 說明 |
| --- | --- |
| `available` | 整體是否可借用。 |
| `message` | 整體結果訊息。 |
| `items` | 各設備的可用性明細。 |

### EquipmentAvailabilityVO.ItemAvailability

| 欄位 | 說明 |
| --- | --- |
| `equipmentId` | 設備 ID。 |
| `equipmentName` | 設備名稱。 |
| `totalQuantity` | 總數量。 |
| `minAvailableQuantity` | 查詢時段內最低可用數量。 |
| `requestedQuantity` | 本次要求數量。 |
| `available` | 數量是否可用。 |
| `venueRulePassed` | 場地規則是否符合。 |
| `message` | 不可借或限制說明。 |

## ContactInfoDTO

與場地預約一致：

- `name`
- `email`
- `phone`

後端將其存入 `equipment_bookings.contact_info` JSON 欄位。
