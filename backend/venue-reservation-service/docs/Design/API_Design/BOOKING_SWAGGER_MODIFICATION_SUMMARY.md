# Booking 模組 Swagger 註解修改摘要

**修改日期：** 2026-04-11  
**修改範圍：** Booking 相關所有組件（Controller、DTO、VO、Result）  
**修改標準：** 
- `api-doc-standard` - API 文檔撰寫標準
- `swagger-api-documentation-standard` - Swagger 配置標準
- `enforcing-code-standard` - 程式碼撰寫規範

---

## 修改內容概覽

### 1. BookingController (控制層)

**修改位置：** `src/main/java/tw/edu/ncu/osa/venue_reservation_service/controller/BookingController.java`

**新增註解：**

- **類別級別註解**
  - `@Tag(name = "預約管理", description = "...")` - 定義 API 分組與詳細描述

- **方法級別註解（每個端點）**
  - `@Operation(summary = "...", description = "...")` - 操作摘要與詳細說明
  - `@ApiResponses({...})` - 定義可能的回應狀態碼與內容
  - `@ApiResponse(responseCode = "200", description = "...", content = @Content(...))` - 具體回應描述
  - `@SecurityRequirement(name = "Mock-Authorization")` - 標記需要認證的端點

- **參數級別註解**
  - `@Parameter(description = "...", example = "...", required = true/false)` - 參數說明與範例

**修改的端點：**

| # | 端點 | 方法 | 說明 |
| --- | --- | --- | --- |
| 1 | `POST /api/bookings` | createBooking | 提交預約申請 |
| 2 | `GET /api/bookings/my` | getMyBookings | 查詢個人預約清單 |
| 3 | `PUT /api/bookings/{id}` | updateBooking | 修改預約申請 |
| 4 | `PUT /api/bookings/{id}/withdraw` | withdrawBooking | 撤回預約申請 |
| 5 | `GET /api/bookings/calendar/month` | getCalendarMonth | 獲取月份日曆視圖 |
| 6 | `GET /api/bookings/calendar/week` | getCalendarWeek | 獲取周份日曆視圖 |
| 7 | `GET /api/bookings/calendar/day` | getCalendarDay | 獲取單日日曆視圖 |

---

### 2. BookingRequestDTO (請求資料傳輸物件)

**修改位置：** `src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/dto/BookingRequestDTO.java`

**新增註解：**

- **類別級別**
  ```java
  @Schema(
      description = "預約申請提交/修改請求物件",
      example = "{ 完整 JSON 範例 }"
  )
  ```

- **每個欄位**
  ```java
  @Schema(
      description = "欄位說明（包含格式、約束、用途等）",
      example = "範例值",
      requiredMode = Schema.RequiredMode.REQUIRED/NOT_REQUIRED
  )
  ```

**主要欄位說明：**

| 欄位 | 類型 | 必填 | 描述 |
| --- | --- | --- | --- |
| venueId | Long | 是 | 場地唯一識別碼 |
| bookingDate | LocalDate | 是 | 預約日期（YYYY-MM-DD，不可為過去） |
| slots | List<Integer> | 是 | 24 小時制時段索引列表（0-23） |
| purpose | String | 是 | 使用用途說明（最多 255 字） |
| participantCount | Integer | 是 | 預計使用人數（≥1） |
| contactInfo | ContactDTO | 是 | 聯絡人詳細資訊 |
| equipmentIds | List<Long> | 否 | 借用設備 ID 列表 |

**ContactDTO 欄位：**

| 欄位 | 類型 | 必填 | 描述 |
| --- | --- | --- | --- |
| name | String | 是 | 聯絡人姓名 |
| email | String | 是 | 電子郵件（需符合格式） |
| phone | String | 是 | 聯絡電話 |

---

### 3. BookingVO (預約視圖物件)

**修改位置：** `src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/BookingVO.java`

**新增註解：**

- **類別級別**
  ```java
  @Schema(
      description = "預約申請詳情物件，用於查詢個人預約清單時的回應",
      example = "{ 完整 JSON 範例 }"
  )
  ```

- **每個欄位** 含 description、example、requiredMode

**主要欄位說明：**

| 欄位 | 類型 | 說明 |
| --- | --- | --- |
| id | Long | 預約申請案的唯一編號 |
| venueName | String | 場地名稱 |
| bookingDate | LocalDate | 預約日期 |
| slots | List<Integer> | 預約時段清單 |
| status | Integer | 申請狀態（0=撤回、1=審核中、2=通過、3=拒絕） |
| createdAt | LocalDateTime | 申請提交時間 |
| purpose | String | 使用用途 |
| pCount | Integer | 預計使用人數 |
| contactInfo | String | 聯絡人資訊（JSON 字串） |
| equipments | List<String> | 所借設備名稱清單 |

---

### 4. VenueCalendarMonthVO (月份日曆視圖)

**修改位置：** `src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/VenueCalendarMonthVO.java`

**新增註解：**

- **類別級別** - 定義物件用途與完整 JSON 範例
- **每個欄位** - description、example、requiredMode
- **內部類 DaySimpleSummary** - 每個欄位詳細說明

**主要欄位說明：**

| 欄位 | 型別 | 說明 |
| --- | --- | --- |
| year | Integer | 年份 |
| month | Integer | 月份（1-12） |
| days | List<DaySimpleSummary> | 該月所有日期的簡化摘要 |
| bookings | List<BookingVO> | 該月所有預約詳細資訊 |

**DaySimpleSummary 欄位：**

| 欄位 | 型別 | 說明 |
| --- | --- | --- |
| date | String | 日期（YYYY-MM-DD） |
| hasApprovedBooking | Boolean | 該日是否有已通過預約 |
| hasUserBooking | Boolean | 該日是否有用戶自己的預約 |

---

### 5. VenueCalendarWeekVO (周份日曆視圖)

**修改位置：** `src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/VenueCalendarWeekVO.java`

**新增註解：**

- **類別級別** - 定義物件用途與完整 JSON 範例
- **每個欄位** - description、example、requiredMode
- **內部類 DayDetailSummary** - 每個欄位詳細說明

**主要欄位說明：**

| 欄位 | 型別 | 說明 |
| --- | --- | --- |
| weekStart | String | 周開始日期（周一，YYYY-MM-DD） |
| weekEnd | String | 周結束日期（周日，YYYY-MM-DD） |
| days | List<DayDetailSummary> | 周內 7 日詳細資訊 |

**DayDetailSummary 欄位：**

| 欄位 | 型別 | 說明 |
| --- | --- | --- |
| date | String | 日期 |
| dayOfWeek | String | 星期幾（中文） |
| approvedSlots | List<Integer> | 已通過預約的時段列表（0-23） |
| userSlots | List<Integer> | 用戶自己的預約時段列表 |

---

### 6. VenueCalendarDayVO (單日日曆視圖)

**修改位置：** `src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo/VenueCalendarDayVO.java`

**新增註解：**

- **類別級別** - 定義物件用途與完整 JSON 範例
- **每個欄位** - description、example、requiredMode
- **內部類 UserBookingDetail** - 每個欄位詳細說明

**主要欄位說明：**

| 欄位 | 型別 | 說明 |
| --- | --- | --- |
| venueId | Long | 場地 ID |
| venueName | String | 場地名稱 |
| date | String | 日期（YYYY-MM-DD） |
| dayOfWeek | String | 星期幾（中文） |
| approvedSlots | List<Integer> | 已通過預約時段列表 |
| userSlots | List<Integer> | 用戶自己的預約時段列表 |
| userBookingDetails | List<UserBookingDetail> | 該日用戶所有預約詳情 |

**UserBookingDetail 欄位：**

| 欄位 | 型別 | 說明 |
| --- | --- | --- |
| bookingId | Long | 預約案編號 |
| slots | List<Integer> | 該預約的時段列表 |
| status | Integer | 預約狀態（0/1/2/3） |
| purpose | String | 使用用途 |
| createdAt | LocalDateTime | 預約建立時間 |

---

### 7. Result<T> (統一回應物件)

**修改位置：** `src/main/java/tw/edu/ncu/osa/venue_reservation_service/common/result/Result.java`

**新增註解：**

- **類別級別**
  ```java
  @Schema(
      description = "通用 API 回應物件，所有端點都使用此格式進行回應",
      example = "{ \"success\": true, \"message\": \"操作成功\", \"data\": {} }"
  )
  ```

- **每個欄位** 含詳細 description 與 example

**主要欄位說明：**

| 欄位 | 類型 | 說明 |
| --- | --- | --- |
| success | Boolean | 操作是否成功 |
| message | String | 提示訊息（成功時為"操作成功"，失敗時包含錯誤原因） |
| data | T | 實際回傳資料（查詢時有值，修改/刪除時通常為 null） |

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
| `@SecurityRequirement` | 方法 | 標記該端點需要認證 |
| `@Content` | @ApiResponse | 定義回應內容的媒體類型與結構 |

### 命名規範

- **中文描述**：所有 `description`、`summary` 等文字都使用繁體中文
- **範例值真實性**：`example` 值符合實際資料格式（如日期格式、ID 類型等）
- **分組一致性**：所有預約管理相關 API 使用同一 `@Tag`

---

## 文檔交付物

### 1. 代碼層面修改

✅ **BookingController.java** - 添加完整 Swagger 註解（595 行）
- 7 個端點，每個都有詳細的 @Operation 和 @ApiResponses
- 參數都有 @Parameter 註解
- 所有非公開 API 都標記 @SecurityRequirement

✅ **BookingRequestDTO.java** - 添加 @Schema 註解（125 行）
- 類別級別 @Schema 含完整請求範例
- 7 個欄位都有詳細 @Schema 說明
- ContactDTO 內部類也有完整註解

✅ **BookingVO.java** - 添加 @Schema 註解（138 行）
- 類別級別 @Schema 含完整回應範例
- 11 個欄位都有詳細 @Schema 說明

✅ **VenueCalendarMonthVO.java** - 添加 @Schema 註解（108 行）
- 包含月視圖和 DaySimpleSummary 完整註解

✅ **VenueCalendarWeekVO.java** - 添加 @Schema 註解（103 行）
- 包含周視圖和 DayDetailSummary 完整註解

✅ **VenueCalendarDayVO.java** - 添加 @Schema 註解（159 行）
- 包含日視圖和 UserBookingDetail 完整註解

✅ **Result.java** - 添加 @Schema 註解（80 行）
- 通用回應物件完整註解
- 3 個欄位詳細說明

### 2. 文檔層面交付

✅ **Booking_API_Detailed_Documentation.md** - 詳細 API 文檔（850+ 行）
- 模組概述與功能說明
- 2.1-2.6 所有 DTO/VO 的詳細說明與完整 JSON 範例
- 3.1-3.7 所有 7 個端點的詳細 API 說明
  - 端點資訊（方法、路徑、認證、參數）
  - 功能描述與業務流程
  - 完整的 curl 請求示例
  - 成功與失敗回應範例
  - 可能的錯誤訊息
- 第四章：錯誤處理機制與常見錯誤訊息表
- 第五章：前後端調用流程圖
- 第六章：技術規範（時間格式、認證方式、HTTP 狀態碼等）
- 第七章：前後端最佳實踐建議
- 第八章：常見問題解答 (FAQ)

✅ **本摘要文檔** - 修改內容整體概覽

---

## Swagger UI 展示效果

修改後，當訪問 Swagger UI (通常在 `/swagger-ui.html`) 時，可以看到：

1. **「預約管理」API 分組** - 在 Swagger UI 的側邊欄中獨立顯示
2. **7 個端點列表** - 每個都有清晰的摘要與詳細描述
3. **參數說明面板** - 每個參數都有中文說明與範例值
4. **回應範例** - 成功與失敗的 JSON 回應示例
5. **Try it out 功能** - 支持直接在 Swagger UI 中測試 API（含參數自動填充）
6. **Schemas 區塊** - 所有 DTO/VO 的結構定義與欄位說明

---

## 與現有標準的對應

### api-doc-standard 要求

| 要求 | 完成情況 |
| --- | --- |
| ✅ 依模組分點 | BookingController 使用 @Tag 統一分組 |
| ✅ 包含所有 API 端點 URL | 3.1-3.7 涵蓋全部 7 個端點 |
| ✅ 功能說明 | 每個端點都有詳細的 @Operation.description |
| ✅ 請求參數說明 | BookingRequestDTO 與 ContactDTO 完整註解 |
| ✅ 成功回應格式說明 | 每個端點都有 @ApiResponse(responseCode="200") |
| ✅ 失敗回應格式說明 | 第四章詳細列舉所有錯誤情況 |
| ✅ 使用範例 | 3.1-3.7 每個端點都有 curl 示例與 JSON 範例 |
| ✅ 清晰簡潔 | 中文描述避免冗長，使用表格分類 |

### swagger-api-documentation-standard 要求

| 要求 | 完成情況 |
| --- | --- |
| ✅ @Tag 定義分組 | BookingController 有詳細 @Tag |
| ✅ @Operation 定義操作 | 7 個端點都有 summary 和 description |
| ✅ @Parameter 標註參數 | 路徑參數和查詢參數都有說明與範例 |
| ✅ @Schema 標註資料模型 | DTO/VO 的類別與欄位都有 @Schema |
| ✅ example 提供範例值 | 所有欄位都有符合格式的範例 |
| ✅ requiredMode 標記必填 | 所有欄位都明確標記 REQUIRED/NOT_REQUIRED |
| ✅ @SecurityRequirement 標記認證 | 需認證的 API 都有標記 |
| ✅ 繁體中文描述 | 所有 UI 文字都使用繁體中文 |

### enforcing-code-standard 要求

| 要求 | 完成情況 |
| --- | --- |
| ✅ 繁體中文註解 | 所有 Javadoc 和行內註解都是繁體中文 |
| ✅ PascalCase 類別名 | BookingRequestDTO、BookingVO 等符合規範 |
| ✅ camelCase 方法名 | createBooking、updateBooking 等符合規範 |
| ✅ Javadoc 文件註解 | 每個類別和公開方法都有 Javadoc |
| ✅ 避免 Magic Numbers | 狀態碼用描述性文字說明（已撤回、審核中等） |

---

## 驗證檢查清單

修改完成後的自我審查：

- ✅ 是否所有暴露於 API 的欄位都有 @Schema 描述與 example？
  - BookingRequestDTO：7 個欄位 + ContactDTO 3 個欄位
  - BookingVO：11 個欄位
  - VenueCalendarMonthVO：2 個欄位 + DaySimpleSummary 3 個欄位
  - VenueCalendarWeekVO：3 個欄位 + DayDetailSummary 4 個欄位
  - VenueCalendarDayVO：7 個欄位 + UserBookingDetail 5 個欄位
  - Result：3 個欄位

- ✅ 需要授權的 API 是否都有 @SecurityRequirement？
  - POST /api/bookings ✅
  - GET /api/bookings/my ✅
  - PUT /api/bookings/{id} ✅
  - PUT /api/bookings/{id}/withdraw ✅
  - 日曆視圖 API 無需認證 ✅

- ✅ @Tag 的名稱是否與功能領域一致？
  - "預約管理" - 正確反映預約模組功能

- ✅ example 值是否符合資料庫格式？
  - ID：Long (501)
  - 日期：LocalDate (2026-04-10)
  - 日期時間：LocalDateTime (2026-04-03T10:00:00)
  - 時段：Integer (0-23)
  - 狀態：Integer (0-3)

- ✅ Result 封裝後的結構在 Schemas 區塊是否正確？
  - success：boolean
  - message：String
  - data：泛型 T（根據不同端點變化）

---

## 後續建議

1. **Swagger UI 測試**
   - 訪問 `/swagger-ui.html` 驗證所有 API 都正確顯示
   - 使用 "Try it out" 功能測試各個端點

2. **前端整合**
   - 使用 Swagger/OpenAPI 代碼生成工具（如 Swagger Codegen）為前端自動生成 TypeScript/JavaScript client
   - 確保前端所有 DTO 定義與後端 @Schema 一致

3. **文檔維護**
   - 當 API 有變更時，同時更新 Swagger 註解和詳細文檔
   - 定期檢查 Swagger UI 與實際代碼的一致性

4. **額外標準應用**
   - 考慮為其他模組（如 Venue Module）應用同樣的 Swagger 標準
   - 建立全專案的 API 文檔規範


