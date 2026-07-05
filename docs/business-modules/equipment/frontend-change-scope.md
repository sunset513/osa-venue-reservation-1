# Equipment Frontend Change Scope

本文記錄本次 equipment 前端整合的修改步驟、涉及檔案與主要目的，供後續前端開發與 code review 參考。

| Step | Purpose | Scope |
| --- | --- | --- |
| 1. 更新 API client | 將前端設備 API 從舊 `/equipment`、`/equipment/history` 接到新後端 `/equipments`、`/equipment-bookings`、`/equipment-reviews`，並新增場地設備合併建立 API。 | `frontend/src/api/equipment.js`、`frontend/src/api/booking.js` |
| 2. 更新 equipment normalizer | 統一轉換 `EquipmentVO`、`EquipmentStatusVO`、`EquipmentBookingVO`、分頁格式與設備狀態文案，降低 view 直接依賴後端欄位細節。 | `frontend/src/utils/equipment.js` |
| 3. 新增設備借用入口 | 在場地選擇頁加入「單獨設備借用」入口，讓使用者可在不選場地的情況下進入設備借用申請。 | `frontend/src/views/VenueSelector.vue`、`frontend/src/router/index.js` |
| 4. 場地預約表單支援設備與數量 | 將原本 `equipmentIds` checkbox 改為 `equipmentItems` 加數量；建立場地預約時若有設備，改呼叫 `POST /bookings/with-equipments`，並移除編輯流程中舊設備 ID 回填。 | `frontend/src/components/booking/BookingModal.vue`、`frontend/src/views/VenueCalendar.vue` |
| 5. 新增獨立設備借用表單 | 新增一般表單頁，支援日期、時段、用途、聯絡資訊、設備與數量，送出 `POST /equipment-bookings` 且不帶 `relatedVenueBookingId`。 | `frontend/src/views/EquipmentBorrowForm.vue`、`frontend/src/router/index.js` |
| 6. 改造設備狀態頁 | 改接 `GET /equipments/status`，顯示所有設備目前借出數、可用數與出借中的 active booking 詳情。 | `frontend/src/views/EquipmentStatus.vue` |
| 7. 改造我的設備借用紀錄 | 改接 `POST /equipment-bookings/query`，支援設備 ID 篩選，並顯示設備申請狀態、設備項目、關聯場地或單獨借用。 | `frontend/src/views/EquipmentBorrowHistory.vue` |
| 8. 場地 review 帶出設備申請 | 審核場地預約詳情時，同步查詢 `GET /equipment-reviews/by-venue-booking/{bookingId}`，在 modal 中顯示關聯設備申請並可核准/拒絕。 | `frontend/src/views/ReviewCalendar.vue`、`frontend/src/components/review/ReviewBookingModal.vue` |
| 9. Review 加入設備借用切換 | 在 review 頁新增「場地預約 / 設備借用」切換，badge 接 `GET /equipment-reviews/standalone/pending-count` 顯示單獨設備待審數，列表接 `POST /equipment-reviews/query` 查詢設備申請。 | `frontend/src/views/ReviewCalendar.vue` |

## Current Update Scope

| Step | Purpose | Scope |
| --- | --- | --- |
| 1. Flexible equipment review status | 新增審核者直接更新設備申請狀態的前端資料流，改用 `PUT /equipment-reviews/{id}/status`，讓已核准可改拒絕、已拒絕可改核准或退回審核中。 | `frontend/src/api/equipment.js`、`frontend/src/views/ReviewCalendar.vue`、`frontend/src/components/review/ReviewBookingModal.vue` |
| 2. Reviewer equipment list | 將 review 頁切換按鈕文案改為「設備借用」，列表改查所有設備申請；badge 維持顯示單獨設備待審數。 | `frontend/src/views/ReviewCalendar.vue` |
| 3. Borrower contact display | 設備審核清單與場地審核詳情中的設備區塊顯示借用者姓名、電話、Email、用途與關聯場地。 | `frontend/src/views/ReviewCalendar.vue`、`frontend/src/components/review/ReviewBookingModal.vue` |
| 4. Personal user menu | 頭像下拉選單只保留個人功能，改為「我的場地借用紀錄」與「我的設備借用紀錄」。 | `frontend/src/components/NavBar.vue` |
| 5. Equipment status page cleanup | 移除設備狀態管理頁面的總設備數、使用中、可用總量卡片，保留查詢條件、狀態表與展開借用資訊。 | `frontend/src/views/EquipmentStatus.vue` |

## Notes

- 本次未啟動前後端服務，僅做靜態資料流與資料格式檢查。
- 場地預約「建立」已支援設備合併建立；場地預約「編輯」仍只修改場地預約本身，因為後端目前沒有提供場地與設備合併更新 API。
- 設備借用 request 的 `contactInfo` 以前端物件送出，後端會序列化為 JSON 存入資料庫；設備借用 response 仍由 normalizer 負責安全解析。
