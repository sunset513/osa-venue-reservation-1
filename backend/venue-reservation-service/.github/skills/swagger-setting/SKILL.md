---
name: swagger-api-documentation-standard
description: 定義 Spring Boot 專案中 OpenAPI 3 (Swagger) 的配置標準，確保 API 文檔具備高可讀性與完整的安全性標記。
---

# 共同原則
- **文檔即合約**：API 說明必須準確反映業務邏輯，方便前端開發者（如 Vue 3 團隊）直接調用。
- **欄位必填化**：所有 DTO/VO 的欄位必須包含描述（Description）與範例（Example）。
- **安全性透明**：涉及權限控管的 API 必須顯式標註授權需求。
- **語言規範**：所有 UI 介面上的描述文字（Summary, Description）統一使用繁體中文。

# 配置標準

## Step 1: 全域配置 (SwaggerConfig)
- **職責**：定義 API 基礎資訊與安全方案。
- **配置要點**：
    - `Info` 物件需包含系統名稱與版本號。
    - 定義 `SecurityScheme`（如 `Mock-Authorization`），類型為 `APIKEY` 或 `HTTP Bearer`。
    - **位置**：`src/main/java/tw/edu/ncu/osa/venue_reservation_service/config/SwaggerConfig.java`

## Step 2: 數據模型 (VO / DTO / Entity)
- **職責**：定義數據結構與欄位含義。
- **標註規則**：
    - **類別上方**：`@Schema(description = "物件用途")`。
    - **欄位上方**：`@Schema(description = "欄位功能", example = "範例值", requiredMode = REQUIRED/NOT_REQUIRED)`。
- **位置**：`src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/` (含 `dto`, `vo`, `entity`)

## Step 3: 控制層 (Controller)
- **職責**：定義 API 端點與導航分組。
- **標註規則**：
    - **類別上方**：使用 `@Tag(name = "模組名稱", description = "模組詳細描述")`。
    - **方法上方**：
        - `@Operation(summary = "短標題", description = "詳細邏輯說明")`。
        - 若非公開 API，必須加上 `@SecurityRequirement(name = "Mock-Authorization")`。
    - **參數上方**：使用 `@Parameter(description = "參數說明", example = "1")` 標註 `@PathVariable` 或 `@RequestParam`。
- **位置**：`src/main/java/tw/edu/ncu/osa/venue_reservation_service/controller`

## Step 4: 通用響應與異常 (Result / Exception)
- **職責**：確保錯誤碼與封裝格式透明化。
- **配置要點**：
    - `Result<T>` 類別需標註 `@Schema`，確保泛型 `T` 能正確展開。
    - 在 Controller 方法上使用 `@ApiResponse(responseCode = "400/500", description = "異常描述")` 標註可能的失敗情況。
- **位置**：
  - `src/main/java/tw/edu/ncu/osa/venue_reservation_service/common/result/Result.java` 
  - `src/main/java/tw/edu/ncu/osa/venue_reservation_service/exception/GlobalExceptionHandler.java`

# Code Review 檢查清單
產出程式碼後應進行自我審查：
1. **描述完整性**：是否所有暴露於 API 的欄位都有 `@Schema` 描述與 `example`？
2. **鎖頭標記**：需要授權的 API（如刪除、新增）是否漏掉 `@SecurityRequirement`？
3. **分組正確性**：`@Tag` 的名稱是否與該 Controller 的功能領域嚴格一致？
4. **範例真實性**：`example` 值是否符合資料庫格式（如 ID 為 Long, 日期格式等）？
5. **一致性**：`Result` 封裝後的 JSON 結構在 Swagger UI 的 "Schemas" 區塊是否顯示正確？

# 其他注意事項
- 執行結束後不需要產生任何說明文件。
- 執行是不需要編譯程式碼進行測試的，僅需確保 Swagger 注解的正確性與完整性。