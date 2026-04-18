# 學務處場地租借管理系統前端

這份文件整理 `frontend/src` 的主要模組，方便新加入的開發者快速理解前端目前的職責分工。

## 功能概述

- 提供學生或教職員查詢學務處場地、查看月曆時段、送出借用申請。
- 提供承辦人進入審核頁面，依場地與狀態篩選申請並進行審核。

## 開發指令

```bash
pnpm dev
```

常用指令：

- `pnpm build`：建置 production bundle
- `pnpm test`：執行前端單元測試
- `pnpm test:watch`：以 watch 模式執行 Vitest


## 專案結構

```text
frontend/
├── docs/                         # 補充文件
├── public/                       # 靜態資源
├── src/
│   ├── api/                      # API 封裝
│   ├── assets/
│   │   └── styles/               # 共用 SCSS 樣式
│   ├── components/               # 可重用 UI 元件
│   │   ├── booking/              # 使用者預約流程元件
│   │   └── review/               # 審核流程元件
│   ├── router/                   # 路由與路由守衛
│   ├── utils/                    # 共用工具函式
│   ├── views/                    # 頁面層元件
│   ├── App.vue                   # App 外框，掛載 NavBar 與 Toast
│   ├── main.js                   # Vue 入口
│   └── style/style.css           # 實際的全域樣式入口
├── index.html
├── jsconfig.json
├── package.json
└── vite.config.js
```

## 模組說明

### `assets`

`src/assets/styles` 放的是可重用的樣式片段，不直接承載頁面邏輯。

- `selector-common.scss`
  - 抽出選擇頁共用卡片樣式，包含 `.selector-page`、`.card-grid`、`.select-card`、`.venue-card`。
  - 目前由 `UnitSelector.vue` 與 `VenueSelector.vue` 透過 `@use` 載入。
- `global.scss`
  - 保留較早期的 reset 與 `.fade-*` 轉場樣式。
  - 目前沒有在 `main.js` 載入，所以不是實際生效中的全域樣式入口。

補充：

- 專案目前真正的全域樣式入口是 `src/style/style.css`。
- 如果是「頁面共用、可抽離重複樣式」，優先放在 `assets/styles`。

### `utils`

`src/utils` 放不直接綁定特定畫面的共用邏輯。

- `dateHelper.js`
  - 負責預約時段整理、顯示字串轉換、FullCalendar event 時間格式轉換與 event 顏色。
  - 常用函式包含：
    - `groupContiguousSlots(slots)`：把連續時段分組。
    - `formatSlotsAsTimeRange(slots)`：把單一時段陣列轉成 `08:00 - 11:00`。
    - `formatSlotGroupsAsTimeRange(slots)`：把不連續時段整理成顯示字串。
    - `convertSlotsToTimeRange(dateStr, slots)`：把後端 `slots` 轉成 FullCalendar 需要的 `start` / `end`。
    - `getEventColorConfig(...)`、`getReviewEventColorConfig(...)`：提供行事曆事件顏色。
- `bookingMeta.js`
  - 放預約顯示層會共用的 metadata 純函式。
  - 目前包含 `parseContactInfo(contactInfo)` 與 `getBookingStatusMeta(status)`。
- `calendarDisplay.js`
  - 放日曆畫面共用的顯示輔助函式。
  - 目前包含 `formatDateKey(date)`、`getDailyEventCount(events, date)`、`renderMoreLinkContent(arg, unitLabel)`。
- `useToast.js`
  - 提供全域 Toast 狀態與操作方法。
  - Toast 是畫面角落短暫出現、通常會自動消失的提示訊息，用來顯示成功、失敗、警告或資訊通知，不會像 `alert()` 一樣中斷操作。
  - `useToast()` 會回傳 `toasts`、`showToast`、`removeToast`、`clearToasts` 與 `success` / `error` / `warning` / `info` 快捷方法。
  - `App.vue` 會掛載 `Toast.vue`，其他頁面或 router guard 只要呼叫 `useToast()` 就能顯示提示訊息。

### `api`

`src/api` 負責集中前端對後端的請求邏輯，避免把 API 細節散落在頁面元件中。

- `index.js`
  - 建立共用 Axios instance。
  - 統一設定 `baseURL`、timeout、`Content-Type`。
  - 在 request interceptor 自動補上 mock token。
  - 在 response interceptor 統一解析後端 `{ success, message, data }` 格式。
- `venue.js`
  - 場地與管理單位相關 API。
  - 包含取得單位列表、查單位底下場地、查單一場地資訊。
- `booking.js`
  - 一般使用者預約流程 API。
  - 包含新增預約、修改預約、撤回預約、查我的預約，以及月曆 / 週曆 / 日曆資料。
- `review.js`
  - 承辦人審核流程 API。
  - 以管理者 mock token 呼叫審核相關端點。
  - 包含查詢待審清單、查申請詳情、通過申請、更新申請狀態。

建議原則：

- API module 只處理「怎麼打 API」。
- 頁面元件只處理「何時呼叫、如何顯示結果」。

### `router`

`src/router` 管理頁面路徑與進入頁面前的驗證。

- `index.js`
  - 定義主要路由：
    - `/`：管理單位選擇頁
    - `/unit/:unitId`：場地選擇頁
    - `/venue/:venueId`：使用者月曆預約頁
    - `/review`：承辦人審核頁
  - 在 `beforeEach` 中統一呼叫 `validateRouteAccess(to)`。
- `guards.js`
  - 驗證 `unitId`、`venueId` 是否為合法數字格式。
  - 透過 API 確認單位或場地是否存在。
  - 驗證失敗時顯示 warning toast，並導回安全頁面。

### `views`

`src/views` 是頁面層元件，負責組合 API、畫面區塊與互動流程。

- `UnitSelector.vue`
  - 首頁。
  - 載入管理單位列表，並混合顯示暫時未開放的 placeholder 單位。
- `VenueSelector.vue`
  - 根據 `unitId` 顯示該單位底下可借用場地。
- `VenueCalendar.vue`
  - 使用者主流程頁。
  - 顯示單一場地月曆、切換同單位場地、查看每日預約、開啟新增 / 編輯預約 modal。
- `ReviewCalendar.vue`
  - 承辦人主流程頁。
  - 依場地與狀態篩選申請，從月曆查看每日申請，並進入審核詳情 modal。
- `Placeholder.vue`
  - 早期開發用的佔位頁，目前主要保留作為示意與備用。

### `components`

`src/components` 放可重用 UI 區塊，目前分為共用元件、預約流程元件、審核流程元件。

- 共用元件
  - `NavBar.vue`：網站上方導覽列與品牌區塊。
  - `Toast.vue`：全域提示訊息顯示元件，由 `App.vue` 統一掛載，負責把 `useToast()` 管理的訊息顯示在畫面角落。
- `components/booking`
  - `BookingModal.vue`：新增 / 編輯預約表單。
  - `DayScheduleModal.vue`：查看某一天的預約清單，並決定是否建立新預約或編輯既有預約。
- `components/review`
  - `ReviewDayScheduleModal.vue`：查看某一天的申請清單，並選擇要打開哪一筆詳情。
  - `ReviewBookingModal.vue`：顯示單筆申請詳情，並提供通過、拒絕、改狀態等操作。

## 使用技術

- Vue 3
- Vue Router
- Pinia
- Axios
- FullCalendar
- Sass
- Lucide Vue Next
- Vite

## 維護建議

- 畫面共用樣式放 `assets/styles`，避免散落在多個 view 中重複維護。
- 資料轉換、日期與提示訊息邏輯放 `utils`。
- 跟後端溝通的程式集中在 `api`，不要直接把 Axios 呼叫寫進 component。
- 頁面層流程留在 `views`，細部互動視窗與可重用區塊拆到 `components`。
