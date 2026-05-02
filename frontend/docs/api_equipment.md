這份報告根據您提供的設備管理模組（Equipment Management Module）程式碼，為您整理了各個 API 的用途、請求參數與返回結果。

這份設計充分體現了您在系統架構上的細心，例如使用軟刪除復原機制來避免資料庫產生髒亂資料，以及透過位元遮罩轉換來優化前端顯示。

以下是完整的 API 規格報告：

---

### 1. 查詢所有設備及使用狀態

- **端點 (Endpoint)：** `GET /api/equipment`
- **功能用途：**
  查詢系統內所有未被軟刪除的設備清單[cite: 7]。系統會在查詢當下，比對當前日期與小時，動態計算該設備是否有進行中的預約（status=1 或 2），藉此判定並返回即時的「使用狀態 (isInUse)」[cite: 7]。
- **請求參數：** 無（僅需 Header 中的 `Mock-Authorization` 權限驗證）[cite: 7]。
- **返回結果：** 返回以「場地」為分組依據的設備清單[cite: 7]。

**回應結構 (`EquipmentListByVenueVO` 陣列[cite: 10])：**
| 欄位名稱 | 型別 | 說明 |
| :--- | :--- | :--- |
| `venueName` | String | 場地名稱[cite: 10] |
| `equipmentList` | Array | 該場地擁有的設備詳細資訊列表[cite: 10] |

**`equipmentList` 內部結構 (`EquipmentWithStatusVO`[cite: 12])：**
| 欄位名稱 | 型別 | 說明 |
| :--- | :--- | :--- |
| `venueId` | Long | 場地唯一識別碼[cite: 12] |
| `venueName` | String | 場地名稱[cite: 12] |
| `equipmentId` | Long | 設備唯一識別碼[cite: 12] |
| `equipmentName` | String | 設備名稱[cite: 12] |
| `quantity` | Integer | 該場地擁有的該設備數量[cite: 12] |
| `isInUse` | Boolean | **即時狀態：** true 表示有預約佔用中，false 表示閒置[cite: 12] |

---

### 2. 新增設備

- **端點 (Endpoint)：** `POST /api/equipment`
- **功能用途：**
  新增一筆設備並將其關聯至指定場地[cite: 7]。具備**防呆與資料復原機制**：若嘗試新增的設備名稱已存在且正常使用中，將拒絕新增；若該名稱的設備已被「軟刪除」，系統會自動將其復原，並重新建立新的場地關聯，避免資料庫累積過多重複名稱的廢棄記錄[cite: 7]。
- **請求參數：** JSON Body (`EquipmentCreateDTO`[cite: 14])

| 欄位名稱        | 型別    | 必填 | 說明                                     |
| :-------------- | :------ | :--- | :--------------------------------------- |
| `equipmentName` | String  | 是   | 設備名稱[cite: 14]                       |
| `venueId`       | Long    | 是   | 要將設備配發至哪個場地[cite: 14]         |
| `quantity`      | Integer | 是   | 該設備在該場地的數量（預設 1）[cite: 14] |

- **返回結果：** 返回新建立（或復原）的設備 ID[cite: 7]。
  - 成功 (`200`)：`{ "success": true, "data": 5, ... }`
  - 失敗 (`400`)：設備名稱已存在[cite: 7]。

---

### 3. 修改設備

- **端點 (Endpoint)：** `PUT /api/equipment/{id}`
- **功能用途：**
  修改現有設備的屬性[cite: 7]。支援更新名稱（需驗證名稱不與其他設備衝突）、變更所屬場地或調整設備數量[cite: 7]。若變更場地資訊，系統會同步刪除舊有關聯並重建 `venue_equipment_map` 中的紀錄[cite: 7]。
- **請求參數：**
  1.  **Path Variable:** `id` (Long, 必填) - 設備唯一識別碼[cite: 7]。
  2.  **JSON Body:** (`EquipmentUpdateDTO`[cite: 15])

| 欄位名稱        | 型別    | 必填 | 說明                      |
| :-------------- | :------ | :--- | :------------------------ |
| `equipmentName` | String  | 否   | 新的設備名稱[cite: 15]    |
| `venueId`       | Long    | 否   | 新的所屬場地 ID[cite: 15] |
| `quantity`      | Integer | 否   | 新的設備數量[cite: 15]    |

- **返回結果：** 返回操作成功提示字串[cite: 7]。

---

### 4. 刪除設備

- **端點 (Endpoint)：** `DELETE /api/equipment/{id}`
- **功能用途：**
  執行設備的**軟刪除 (Soft Delete)**[cite: 7]。在刪除前，系統會嚴格檢查該設備是否被包含在「尚未結束的預約（狀態為審核中或已通過）」內[cite: 7]。若有進行中的預約則強制擋下；若無，則執行軟刪除並清除場地關聯表 (`venue_equipment_map`) 的紀錄[cite: 7]。
- **請求參數：**
  - **Path Variable:** `id` (Long, 必填) - 設備唯一識別碼[cite: 7]。
- **返回結果：** 返回操作成功提示字串[cite: 7]。
  - 失敗 (`400`)：該設備有進行中的預約，無法刪除[cite: 7]。

---

### 5. 查詢設備借用歷史紀錄

- **端點 (Endpoint)：** `GET /api/equipment/history`
- **功能用途：**
  以分頁形式列出設備的借用紀錄[cite: 7]。僅針對狀態為「已通過 (status=2)」的預約進行查詢[cite: 7]。後端會將原始儲存的 24-bit 位元遮罩 (timeSlots)，自動解析合併為對人類友善的連續時段字串（例如："09:00-12:00, 14:00-16:00"）交給前端渲染[cite: 7]。
- **請求參數：** URL Query String (`EquipmentBorrowQueryDTO`[cite: 13])

| 欄位名稱   | 型別    | 預設值 | 說明                                            |
| :--------- | :------ | :----- | :---------------------------------------------- |
| `pageNum`  | Integer | 1      | 請求的頁碼，若傳入 <=0 則自動校正為 1[cite: 13] |
| `pageSize` | Integer | 10     | 每頁顯示筆數，最大上限為 100[cite: 13]          |

- **返回結果：** 包含分頁資訊與資料陣列的複合結構[cite: 7]。

**回應結構 (`EquipmentBorrowRecordPageVO`[cite: 8])：**
| 欄位名稱 | 型別 | 說明 |
| :--- | :--- | :--- |
| `totalCount` | Integer | 總資料筆數[cite: 8] |
| `totalPages` | Integer | 總頁數[cite: 8] |
| `currentPage` | Integer | 當前頁碼[cite: 8] |
| `pageSize` | Integer | 當前設定的每頁筆數[cite: 8] |
| `data` | Array | 該頁的借用紀錄清單 (`EquipmentBorrowRecordVO`)[cite: 8] |

**`data` 內部結構 (`EquipmentBorrowRecordVO`[cite: 9])：**
| 欄位名稱 | 型別 | 說明 |
| :--- | :--- | :--- |
| `venueId` | Long | 場地 ID[cite: 9] |
| `venueName` | String | 場地名稱[cite: 9] |
| `equipmentId` | Long | 設備 ID[cite: 9] |
| `equipmentName` | String | 設備名稱[cite: 9] |
| `borrowDate` | LocalDate | 借用的具體日期 (YYYY-MM-DD)[cite: 9] |
| `timeSlots` | String | 解析後的易讀時段（例：09:00-12:00）[cite: 9] |
| `purpose` | String | 借用用途[cite: 9] |
