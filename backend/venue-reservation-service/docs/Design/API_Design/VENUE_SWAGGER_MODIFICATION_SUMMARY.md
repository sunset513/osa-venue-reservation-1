# Venue 模組 Swagger 註解修改摘要

**修改日期：** 2026-04-11  
**修改範圍：** Venue 相關所有組件（Controller、VO）  
**修改標準：** 
- `api-doc-standard` - API 文檔撰寫標準
- `swagger-api-documentation-standard` - Swagger 配置標準
- `enforcing-code-standard` - 程式碼撰寫規範

---

## 修改內容概覽

### 1. VenueController (控制層)

**修改位置：** `src/main/java/tw/edu/ncu/osa/venue_reservation_service/controller/VenueController.java`

**新增註解：**

- **類別級別註解**
  - `@Tag(name = "場地與組織", description = "...")` - 定義 API 分組與詳細描述

- **方法級別註解（每個端點）**
  - `@Operation(summary = "...", description = "...")` - 操作摘要與詳細說明
  - `@ApiResponses({...})` - 定義可能的回應狀態碼與內容
  - `@ApiResponse(responseCode = "200", description = "...", content = @Content(...))` - 具體回應描述

- **參數級別註解**
  - `@Parameter(description = "...", example = "...", required = true/false)` - 參數說明與範例

**修改的端點：**

| # | 端點 | 方法 | 說明 |
| --- | --- | --- | --- |
| 1 | `GET /api/public/units` | getAllUnits | 獲取所有管理單位清單 |
| 2 | `GET /api/public/venues` | getVenuesByUnit | 查詢單位下的場地清單 |
| 3 | `GET /api/public/venues/{id}` | getVenueById | 獲取場地詳細資訊 |

**特色：** Venue 模組的所有 API 都是公開接口，無需身份認證，因此沒有 `@SecurityRequirement` 註解

---

### 2. UnitVO (管理單位視圖物件)

**修改位置：** `src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/UnitVO.java`

**新增註解：**

- **類別級別**
  ```java
  @Schema(
      description = "管理單位資訊物件，用於標識系統中不同的管理單位",
      example = "{ \"id\": 1, \"name\": \"學生事務處\", \"code\": \"SAA\" }"
  )
  ```

- **每個欄位**
  ```java
  @Schema(
      description = "欄位說明",
      example = "範例值",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  ```

**主要欄位說明：**

| 欄位 | 類型 | 必填 | 描述 |
| --- | --- | --- | --- |
| id | Long | 是 | 單位唯一識別碼（1 或 2） |
| name | String | 是 | 單位名稱（學生事務處 或 圖書館） |
| code | String | 是 | 單位代碼（SAA 或 LIB） |

**系統內單位清單：**

| ID | 名稱 | 代碼 |
| --- | --- | --- |
| 1 | 學生事務處 | SAA |
| 2 | 圖書館 | LIB |

---

### 3. EquipmentVO (設備視圖物件)

**修改位置：** `src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/EquipmentVO.java`

**新增註解：**

- **類別級別** 含完整 JSON 範例
- **每個欄位** 含 description、example、requiredMode

**主要欄位說明：**

| 欄位 | 類型 | 說明 |
| --- | --- | --- |
| id | Long | 設備唯一識別碼 |
| name | String | 設備名稱（投影機、麥克風等） |

---

### 4. VenueVO (場地視圖物件)

**修改位置：** `src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/VenueVO.java`

**新增註解：**

- **類別級別** - 定義物件用途與完整 JSON 範例
- **每個欄位** - description、example、requiredMode

**主要欄位說明：**

| 欄位 | 類型 | 說明 |
| --- | --- | --- |
| id | Long | 場地唯一識別碼（1 或 2） |
| unitId | Long | 所屬管理單位 ID（1 或 2） |
| name | String | 場地名稱 |
| capacity | Integer | 容納人數上限 |
| description | String | 場地介紹與借用規則說明 |
| equipments | List<EquipmentVO> | 可借用設備清單 |

**系統內場地清單：**

| ID | 名稱 | 所屬單位 | 容納人數 |
| --- | --- | --- | --- |
| 1 | 會議室 A | 1（學生事務處） | 50 |
| 2 | 自習室 B | 2（圖書館） | 30 |

---

## Swagger 註解標準應用

### 使用的主要註解

| 註解 | 位置 | 用途 |
| --- | --- | --- |
| `@Tag` | 類別 | 定義 API 分組名稱與詳細描述 |
| `@Operation` | 方法 | 定義端點摘要與詳細業務邏輯說明 |
| `@ApiResponse` | 方法 | 定義單個 HTTP 回應狀態 |
| `@ApiResponses` | 方法 | 定義多個 HTTP 回應狀態（集合） |
| `@Parameter` | 參數 | 定義路徑參數或查詢參數的說明與範例 |
| `@Schema` | 類別/欄位 | 定義資料模型的結構、欄位說明、範例值 |
| `@Content` | @ApiResponse | 定義回應內容的媒體類型與結構 |

### 命名規範

- **中文描述**：所有 `description`、`summary` 等文字都使用繁體中文
- **範例值真實性**：`example` 值符合實際資料格式與數據庫中的真實值（ID=1或2）
- **分組一致性**：所有場地相關 API 使用同一 `@Tag`

---

## 文檔交付物

### 1. 代碼層面修改

✅ **VenueController.java** - 添加完整 Swagger 註解（167 行）
- 3 個端點，每個都有詳細的 @Operation 和 @ApiResponses
- 查詢參數都有 @Parameter 註解，包含 unitId=1或2、id=1或2 的說明
- 所有端點都清楚標示為公開接口（無 @SecurityRequirement）

✅ **UnitVO.java** - 添加 @Schema 註解（45 行）
- 類別級別 @Schema 含完整請求範例
- 3 個欄位都有詳細 @Schema 說明
- 特別說明了目前系統內的單位 ID（1 和 2）

✅ **EquipmentVO.java** - 添加 @Schema 註解（37 行）
- 類別級別 @Schema 含完整範例
- 2 個欄位都有詳細 @Schema 說明

✅ **VenueVO.java** - 添加 @Schema 註解（82 行）
- 類別級別 @Schema 含完整回應範例
- 6 個欄位都有詳細 @Schema 說明
- 強調場地 ID 與單位 ID 的對應關係

### 2. 文檔層面交付

✅ **Venue_API_Detailed_Documentation.md** - 詳細 API 文檔（600+ 行）
- 模組概述與功能說明
- **系統現況說明**：清楚列出目前系統內有 2 個單位（ID 1, 2）和 2 個場地（ID 1, 2）
- 2.1-2.4 所有 VO 的詳細說明與完整 JSON 範例
- 3.1-3.3 所有 3 個端點的詳細 API 說明
  - 端點資訊（方法、路徑、參數）
  - 功能描述與應用場景
  - 完整的 curl 請求示例（包含 unitId=1、id=1 的具體示例）
  - 成功與失敗回應範例
  - 可能的錯誤訊息
- 第四章：前後端調用流程圖與實際調用示例
- 第五章：技術規範（場地與單位 ID 映射表、HTTP 狀態碼等）
- 第六章：前端實現建議（Vue 3 示例代碼）
- 第七章：常見問題解答 (FAQ)

✅ **本摘要文檔** - 修改內容整體概覽

---

## 與 Booking 模組的區別

| 特性 | Booking 模組 | Venue 模組 |
| --- | --- | --- |
| API 性質 | 私有（需認證） | 公開（無需認證） |
| @SecurityRequirement | 有 | 無 |
| 操作類型 | CRUD（新增、查詢、修改、撤回） | 只讀（查詢） |
| DTO | 有（BookingRequestDTO） | 無（只讀） |
| VO | 有（BookingVO、Calendar VO） | 有（UnitVO、VenueVO、EquipmentVO） |
| API 端點數 | 7 個 | 3 個 |

---

## 系統現況說明

為了提供準確的 API 文檔示例，本次修改特別強調了數據庫中的實際數據：

### 管理單位（Units）

```
ID = 1: 學生事務處 (Code: SAA)
ID = 2: 圖書館 (Code: LIB)
```

### 場地（Venues）

```
ID = 1: 會議室 A
  - 所屬單位：1（學生事務處）
  - 容納人數：50
  - 可借設備：投影機、音響系統、麥克風

ID = 2: 自習室 B
  - 所屬單位：2（圖書館）
  - 容納人數：30
  - 可借設備：檯燈
```

所有 API 示例和文檔都基於這些真實數據，確保文檔的準確性與可用性。

---

## Swagger UI 展示效果

修改後，當訪問 Swagger UI (通常在 `/swagger-ui.html`) 時，可以看到：

1. **「場地與組織」API 分組** - 在 Swagger UI 的側邊欄中獨立顯示
2. **3 個端點列表** - 每個都有清晰的摘要與詳細描述
3. **參數說明面板** - unitId 和 id 參數都有中文說明與真實範例（1 或 2）
4. **回應範例** - 包含單位、場地、設備的完整 JSON 範例
5. **Try it out 功能** - 支持直接在 Swagger UI 中測試 API
6. **Schemas 區塊** - 所有 VO 的結構定義與欄位說明

---

## 與現有標準的對應

### api-doc-standard 要求

| 要求 | 完成情況 |
| --- | --- |
| ✅ 依模組分點 | VenueController 使用 @Tag 統一分組 |
| ✅ 包含所有 API 端點 URL | 3.1-3.3 涵蓋全部 3 個端點 |
| ✅ 功能說明 | 每個端點都有詳細的 @Operation.description |
| ✅ 請求參數說明 | unitId、id 參數都有完整說明與範例 |
| ✅ 成功回應格式說明 | 每個端點都有 @ApiResponse(responseCode="200") 與 JSON 範例 |
| ✅ 失敗回應格式說明 | 第四章詳細列舉所有錯誤情況 |
| ✅ 使用範例 | 3.1-3.3 每個端點都有 curl 示例與 JSON 範例 |
| ✅ 清晰簡潔 | 中文描述避免冗長，使用表格和 FAQ |

### swagger-api-documentation-standard 要求

| 要求 | 完成情況 |
| --- | --- |
| ✅ @Tag 定義分組 | VenueController 有詳細 @Tag |
| ✅ @Operation 定義操作 | 3 個端點都有 summary 和 description |
| ✅ @Parameter 標註參數 | 查詢參數和路徑參數都有說明與範例 |
| ✅ @Schema 標註資料模型 | VO 的類別與欄位都有 @Schema |
| ✅ example 提供範例值 | 所有欄位都有符合實際的範例（ID=1或2） |
| ✅ requiredMode 標記必填 | 所有欄位都明確標記 REQUIRED |
| ✅ 繁體中文描述 | 所有 UI 文字都使用繁體中文 |

---

## 驗證檢查清單

修改完成後的自我審查：

- ✅ 是否所有暴露於 API 的欄位都有 @Schema 描述與 example？
  - UnitVO：3 個欄位 ✅
  - EquipmentVO：2 個欄位 ✅
  - VenueVO：6 個欄位 ✅

- ✅ 場地 API 是否都正確標識為公開接口？
  - 沒有 @SecurityRequirement ✅
  - 文檔中明確說明「無需認證」✅

- ✅ @Tag 的名稱是否與功能領域一致？
  - "場地與組織" - 正確反映場地及單位查詢功能 ✅

- ✅ example 值是否符合數據庫中的真實數據？
  - unitId：1 或 2 ✅
  - id（venueId）：1 或 2 ✅
  - 單位名稱：學生事務處、圖書館 ✅
  - 場地名稱：會議室 A、自習室 B ✅

- ✅ 文檔中是否清楚說明系統現況？
  - 「系統現況」部分明確列出 2 個單位和 2 個場地 ✅
  - 所有 API 示例都基於真實數據 ✅

---

## 後續建議

1. **Swagger UI 測試**
   - 訪問 `/swagger-ui.html` 驗證 3 個 API 都正確顯示
   - 使用 "Try it out" 功能測試各個端點（分別測試 unitId=1、unitId=2、id=1、id=2）

2. **前端整合**
   - 使用本文檔中提供的 Vue 3 示例代碼快速集成 API 調用
   - 根據實際的 UI 設計調整數據展示邏輯

3. **文檔維護**
   - 當新增場地或單位時，同時更新文檔和 Swagger 註解
   - 定期驗證 Swagger UI 與實際代碼的一致性

4. **與 Booking 模組整合**
   - Venue API 提供場地選擇功能
   - 用戶選定場地後調用 Booking API 進行預約申請
   - 兩個模組形成完整的場地預約流程


