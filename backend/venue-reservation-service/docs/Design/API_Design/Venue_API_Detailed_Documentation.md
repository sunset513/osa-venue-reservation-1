# 場地與組織模組 API 詳細文檔

**文檔版本：** V2.0 (詳細版)  
**最後更新日期：** 2026-04-11  
**基礎路徑：** `/api/public`  
**認證要求：** 無需認證（公開接口）

---

## 一、模組概述

**場地與組織模組** 提供場地、單位與設備資訊的公開查詢功能，包括：
- 🏢 **單位查詢**：查詢系統中所有可用的管理單位
- 🏛️ **場地查詢**：根據單位查詢該單位下的場地清單
- 📋 **場地詳情**：查看具體場地的詳細資訊（容納人數、借用規則、可借設備）

本模組是整個預約系統的基礎，提供了場地瀏覽與選擇的核心功能。用戶通過此模組了解系統中有哪些場地可供預約，進而決定要在哪個場地提交預約申請。

**系統現況：**
- 📍 管理單位：2 個
  - ID = 1：學生事務處
  - ID = 2：圖書館
- 🏛️ 場地：2 個
  - ID = 1：會議室 A（隸屬單位 1）
  - ID = 2：自習室 B（隸屬單位 2）

---

## 二、數據傳輸物件設計 (VO)

### 2.1 UnitVO - 管理單位資訊

**用途：** 回傳管理單位的基本資訊

| 欄位名稱 | 類型 | 說明 | 範例 |
| :--- | :--- | :--- | :--- |
| `id` | Long | 單位唯一識別碼 | `1` |
| `name` | String | 單位名稱 | `學生事務處` |
| `code` | String | 單位代碼（系統內部識別） | `SAA` |

**完整 JSON 範例：**

```json
{
  "id": 1,
  "name": "學生事務處",
  "code": "SAA"
}
```

**所有可用的單位列表：**

| ID | 名稱 | 代碼 | 說明 |
| :--- | :--- | :--- | :--- |
| 1 | 學生事務處 | SAA | Student Affairs Administration |
| 2 | 圖書館 | LIB | Library |

---

### 2.2 EquipmentVO - 設備資訊

**用途：** 回傳場地中可借用的設備資訊

| 欄位名稱 | 類型 | 說明 | 範例 |
| :--- | :--- | :--- | :--- |
| `id` | Long | 設備唯一識別碼 | `1` |
| `name` | String | 設備名稱 | `投影機` |

**完整 JSON 範例：**

```json
{
  "id": 1,
  "name": "投影機"
}
```

---

### 2.3 VenueVO - 場地詳細資訊

**用途：** 回傳場地的完整資訊，包含可借用設備清單

| 欄位名稱 | 類型 | 說明 | 範例 |
| :--- | :--- | :--- | :--- |
| `id` | Long | 場地唯一識別碼 | `1` |
| `unitId` | Long | 所屬管理單位 ID | `1` |
| `name` | String | 場地名稱 | `會議室 A` |
| `capacity` | Integer | 容納人數上限 | `50` |
| `description` | String | 場地介紹與借用規則 | `位於 3 樓，配備投影機和音響...` |
| `equipments` | List<EquipmentVO> | 可借用設備清單 | 見下表 |

**完整 JSON 範例：**

```json
{
  "id": 1,
  "unitId": 1,
  "name": "會議室 A",
  "capacity": 50,
  "description": "位於 3 樓，配備投影機和音響系統。可容納 50 人會議。借用需提前 3 天提出申請。",
  "equipments": [
    {
      "id": 1,
      "name": "投影機"
    },
    {
      "id": 2,
      "name": "音響系統"
    }
  ]
}
```

---

### 2.4 Result<T> - 通用 API 回應物件

**用途：** 封裝所有 API 的回應

| 欄位名稱 | 類型 | 說明 |
| :--- | :--- | :--- |
| `success` | Boolean | 操作是否成功。true=成功，false=失敗 |
| `message` | String | 提示訊息 |
| `data` | T | 實際回傳資料 |

**成功回應範例：**

```json
{
  "success": true,
  "message": "操作成功",
  "data": { /* 具體數據 */ }
}
```

**失敗回應範例：**

```json
{
  "success": false,
  "message": "找不到指定的場地資訊",
  "data": null
}
```

---

## 三、API 端點詳細說明

### 3.1 獲取所有管理單位清單

**端點資訊：**
- **方法：** GET
- **路徑：** `/api/public/units`
- **認證：** 無需認證（公開接口）

**功能描述：**

取得系統中所有可用的管理單位資訊。本 API 通常在前端應用初始化時調用，獲取所有可用的管理單位列表，用於提供用戶進行單位篩選，進一步查詢該單位下的場地。

**應用場景：**

1. **場地瀏覽流程**：用戶首先調用此 API 獲取所有可用單位，選擇感興趣的單位
2. **下拉菜單**：前端可將單位列表展示為下拉選項，供用戶選擇
3. **前端初始化**：應用啟動時調用此 API，緩存單位清單到前端狀態管理中

**返回數據包含：**

- 單位 ID：唯一識別碼，用於後續查詢該單位下的場地
- 單位名稱：用於 UI 展示
- 單位代碼：內部編碼，用於系統識別

**請求示例：**

```bash
curl -X GET "http://localhost:8080/api/public/units"
```

**成功回應 (200 OK)：**

```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "學生事務處",
      "code": "SAA"
    },
    {
      "id": 2,
      "name": "圖書館",
      "code": "LIB"
    }
  ]
}
```

**失敗回應：** 此端點通常不會失敗（即使無單位也返回空陣列）

```json
{
  "success": true,
  "message": "操作成功",
  "data": []
}
```

---

### 3.2 查詢單位下的場地清單

**端點資訊：**
- **方法：** GET
- **路徑：** `/api/public/venues`
- **認證：** 無需認證（公開接口）
- **查詢參數：**
  - `unitId` (Long, 必填) - 管理單位的唯一識別碼（1 或 2）

**功能描述：**

根據指定的單位 ID，查詢該單位管理的所有場地清單。用戶在單位篩選後，調用此 API 獲取該單位下的所有可預約場地，進一步點擊具體場地查看詳細資訊。

**應用場景：**

1. **場地清單展示**：用戶選擇單位後，展示該單位下的所有場地
2. **場地篩選**：前端根據返回的場地列表，進行容納人數、設備等進一步篩選
3. **場地卡片列表**：以卡片形式展示場地名稱、容納人數等關鍵資訊

**返回數據包含：**

1. 場地基本資訊：ID、名稱、所屬單位 ID
2. 容納人數：該場地的最大容納人數
3. 場地介紹：借用規則、設施說明等
4. 可借設備清單：該場地提供的所有可借用設備（簡化版，僅包含 ID 和名稱）

**請求示例：**

```bash
# 查詢學生事務處（unitId=1）下的場地
curl -X GET "http://localhost:8080/api/public/venues?unitId=1"

# 查詢圖書館（unitId=2）下的場地
curl -X GET "http://localhost:8080/api/public/venues?unitId=2"
```

**成功回應 (200 OK)：**

```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "unitId": 1,
      "name": "會議室 A",
      "capacity": 50,
      "description": "位於 3 樓，配備投影機和音響系統。可容納 50 人會議。借用需提前 3 天提出申請。",
      "equipments": [
        {
          "id": 1,
          "name": "投影機"
        },
        {
          "id": 2,
          "name": "音響系統"
        }
      ]
    }
  ]
}
```

**場地為空回應：**

```json
{
  "success": true,
  "message": "操作成功",
  "data": []
}
```

**失敗回應示例：**

| 情況 | 回應 |
| :--- | :--- |
| 單位不存在 | `{"success": false, "message": "找不到指定的單位", "data": null}` |

---

### 3.3 獲取場地詳細資訊

**端點資訊：**
- **方法：** GET
- **路徑：** `/api/public/venues/{id}`
- **認證：** 無需認證（公開接口）
- **路徑參數：**
  - `id` (Long, 必填) - 場地的唯一識別碼（1 或 2）

**功能描述：**

根據場地 ID 獲取該場地的完整資訊。用戶在場地列表中選擇具體場地後，調用此 API 獲取場地的詳細資訊，包括容納人數上限、借用規則、可借用的設備等，作為進一步預約的參考。

**應用場景：**

1. **場地詳情頁面**：用戶點擊場地後，進入詳情頁面，展示完整的場地介紹與借用規則
2. **預約前準備**：用戶在預約前查看場地詳情，確認場地是否符合需求（容納人數、設備等）
3. **設備選擇**：用戶在提交預約申請時，根據此 API 返回的設備清單選擇要借用的設備

**返回數據包含：**

1. 場地基本資訊：ID、名稱、所屬單位 ID
2. 容納人數：該場地的最大容納人數上限
3. 場地介紹：詳細的借用規則、設施說明等文字說明
4. 可借設備詳細清單：所有可供借用的設備名稱與 ID

**請求示例：**

```bash
# 獲取 ID 為 1 的場地詳細資訊（會議室 A）
curl -X GET "http://localhost:8080/api/public/venues/1"

# 獲取 ID 為 2 的場地詳細資訊（自習室 B）
curl -X GET "http://localhost:8080/api/public/venues/2"
```

**成功回應 (200 OK)：**

```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "id": 1,
    "unitId": 1,
    "name": "會議室 A",
    "capacity": 50,
    "description": "位於 3 樓，配備投影機和音響系統。可容納 50 人會議。借用需提前 3 天提出申請，如需取消請至少提前 24 小時告知。場地內有空調、白板和會議用長桌。",
    "equipments": [
      {
        "id": 1,
        "name": "投影機"
      },
      {
        "id": 2,
        "name": "音響系統"
      },
      {
        "id": 3,
        "name": "麥克風"
      }
    ]
  }
}
```

**失敗回應示例：**

| 情況 | 回應 |
| :--- | :--- |
| 場地不存在 | `{"success": false, "message": "找不到指定的場地資訊", "data": null}` |

---

## 四、前後端調用流程示例

### 4.1 用戶瀏覽場地流程

```
前端                        後端
  │                          │
  ├─ 1. 應用初始化 ──────→
  │    調用 3.1 GET /api/public/units
  │                         │
  │                     (獲取所有單位)
  │                         │
  │    ← Result<List<UnitVO>> ────────
  │                         │
  ├─ 2. 用戶選擇單位 (e.g., unitId=1)
  │
  ├─ 3. 查詢單位下場地 ──→
  │    調用 3.2 GET /api/public/venues?unitId=1
  │                         │
  │                   (查詢該單位場地)
  │                         │
  │    ← Result<List<VenueVO>> ────────
  │                         │
  ├─ 4. 用戶選擇場地 (e.g., id=1)
  │
  ├─ 5. 查看場地詳情 ────→
  │    調用 3.3 GET /api/public/venues/1
  │                         │
  │              (獲取場地詳細資訊)
  │                         │
  │    ← Result<VenueVO> ───────────
  │                         │
  ├─ 6. 用戶點擊「預約」按鈕
  │
  └─ 7. 跳轉到預約申請頁面
       調用 Booking API (3.1 POST /api/bookings)
```

### 4.2 實際調用示例

**步驟 1：初始化時獲取所有單位**

```bash
curl -X GET "http://localhost:8080/api/public/units"
```

回應：

```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    { "id": 1, "name": "學生事務處", "code": "SAA" },
    { "id": 2, "name": "圖書館", "code": "LIB" }
  ]
}
```

**步驟 2：用戶選擇「學生事務處」(ID=1)，查詢其下的場地**

```bash
curl -X GET "http://localhost:8080/api/public/venues?unitId=1"
```

回應：

```json
{
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "unitId": 1,
      "name": "會議室 A",
      "capacity": 50,
      "description": "位於 3 樓，配備投影機和音響系統。",
      "equipments": [
        { "id": 1, "name": "投影機" },
        { "id": 2, "name": "音響系統" }
      ]
    }
  ]
}
```

**步驟 3：用戶點擊「會議室 A」查看詳情**

```bash
curl -X GET "http://localhost:8080/api/public/venues/1"
```

回應：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "id": 1,
    "unitId": 1,
    "name": "會議室 A",
    "capacity": 50,
    "description": "位於 3 樓，配備投影機和音響系統。可容納 50 人會議。借用需提前 3 天提出申請，如需取消請至少提前 24 小時告知。",
    "equipments": [
      { "id": 1, "name": "投影機" },
      { "id": 2, "name": "音響系統" },
      { "id": 3, "name": "麥克風" }
    ]
  }
}
```

**步驟 4：用戶點擊「預約」，跳轉到預約申請頁面**

- 前端記住場地 ID (venueId=1)
- 調用 Booking API 提交預約申請

```bash
curl -X POST "http://localhost:8080/api/bookings" \
  -H "Authorization: mock-token-123" \
  -H "Content-Type: application/json" \
  -d '{
    "venueId": 1,
    "bookingDate": "2026-04-10",
    "slots": [8, 9],
    "purpose": "團隊會議",
    "participantCount": 20,
    "contactInfo": {
      "name": "王小明",
      "email": "xm@ncu.edu.tw",
      "phone": "0912345678"
    },
    "equipmentIds": [1, 3]
  }'
```

---

## 五、技術規範

### 5.1 場地與單位 ID 映射表

**單位列表：**

| ID | 名稱 | 代碼 | 備註 |
| :--- | :--- | :--- | :--- |
| 1 | 學生事務處 | SAA | Student Affairs Administration |
| 2 | 圖書館 | LIB | Library |

**場地列表：**

| ID | 名稱 | 所屬單位 ID | 容納人數 | 可借設備 |
| :--- | :--- | :--- | :--- | :--- |
| 1 | 會議室 A | 1 | 50 | 投影機、音響系統、麥克風 |
| 2 | 自習室 B | 2 | 30 | 檯燈 |

### 5.2 HTTP 狀態碼規範

本系統統一使用 **HTTP 200** 狀態碼進行所有回應，通過 `Result.success` 欄位區分成功與失敗：

```
成功：HTTP 200 + success=true
失敗：HTTP 200 + success=false
```

### 5.3 Content-Type

所有 GET 請求回應 Content-Type：

```
Content-Type: application/json
```

---

## 六、前端實現建議

### 6.1 單位選擇實現

```javascript
// Vue 3 示例
const units = ref([]);

async function initializeUnits() {
  try {
    const response = await fetch('http://localhost:8080/api/public/units');
    const result = await response.json();
    if (result.success) {
      units.value = result.data;
    }
  } catch (error) {
    console.error('Failed to fetch units:', error);
  }
}

onMounted(() => {
  initializeUnits();
});
```

### 6.2 場地清單實現

```javascript
// Vue 3 示例
const venues = ref([]);
const selectedUnit = ref(null);

async function fetchVenuesByUnit(unitId) {
  try {
    const response = await fetch(
      `http://localhost:8080/api/public/venues?unitId=${unitId}`
    );
    const result = await response.json();
    if (result.success) {
      venues.value = result.data;
    }
  } catch (error) {
    console.error('Failed to fetch venues:', error);
  }
}

watch(selectedUnit, (newUnitId) => {
  if (newUnitId) {
    fetchVenuesByUnit(newUnitId);
  }
});
```

### 6.3 場地詳情實現

```javascript
// Vue 3 示例
const venueDetail = ref(null);

async function fetchVenueDetail(venueId) {
  try {
    const response = await fetch(
      `http://localhost:8080/api/public/venues/${venueId}`
    );
    const result = await response.json();
    if (result.success) {
      venueDetail.value = result.data;
    }
  } catch (error) {
    console.error('Failed to fetch venue detail:', error);
  }
}
```

### 6.4 錯誤處理建議

```javascript
// 統一的錯誤處理邏輯
function handleApiResponse(result) {
  if (result.success) {
    return result.data;
  } else {
    // 顯示錯誤訊息給用戶
    showError(result.message);
    throw new Error(result.message);
  }
}
```

---

## 七、常見問題 (FAQ)

**Q: 如何知道有多少個場地？**

A: 首先調用 3.1 API 獲取所有單位，然後針對每個單位調用 3.2 API 查詢其下的場地。或者直接查看本文檔的「系統現況」部分。

**Q: 場地容納人數有什麼用？**

A: 用戶在提交預約時，系統會驗證預估參與人數不超過場地的容納人數上限。此欄位提前告知用戶該場地的限制。

**Q: 可借設備清單在哪裡？**

A: 調用 3.2 API（查詢單位下的場地清單）會返回簡化版的設備清單；調用 3.3 API（獲取場地詳細資訊）會返回完整的設備清單。兩個 API 返回的設備資訊相同，只是場景不同。

**Q: 為什麼場地 ID 和單位 ID 有限制（只有 1 和 2）？**

A: 當前系統處於 MVP（最小可行產品）階段，數據庫中只有 2 個單位和 2 個場地。未來可根據需求擴展。

**Q: 如果在 API 調用中使用不存在的 ID 會怎樣？**

A: 系統會返回 `success=false` 和相應的錯誤訊息，HTTP 狀態碼仍為 200。

---

## 八、版本歷史

| 版本 | 日期 | 變更內容 |
| :--- | :--- | :--- |
| V1.0 | 2026-04-03 | 初版設計文檔 |
| V2.0 | 2026-04-11 | 添加 Swagger 註解、詳細 API 說明、前後端實現建議 |


