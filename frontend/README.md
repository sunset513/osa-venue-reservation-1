# 學務處場地租借管理系統前端

## 功能概述

- 提供中央大學教職員、學生申請借用學務處所屬場地，
- 提供學務處承辦人便於審核前述申請。

## 專案結構

```
frontend/
├── index.html                # 主入口 HTML 文件
├── jsconfig.json             # JavaScript 配置文件
├── package.json              # 項目依賴和腳本配置
├── pnpm-lock.yaml            # PNPM 鎖定文件，確保依賴版本一致
├── README.md                 # 專案說明文件
├── vite.config.js            # Vite 配置文件
├── docs/                     # 文件資料夾
│   ├── api_reference.md      # API 參考文件
│   ├── mock_auth_logic.md    # 模擬認證邏輯文件
├── public/                   # 靜態資源文件夾
├── src/                      # 主應用程式碼文件夾
│   ├── App.vue               # Vue 主應用組件
│   ├── main.js               # 應用入口文件
│   ├── api/                  # API 模組
│   │   ├── booking.js        # 場地預訂相關 API
│   │   ├── index.js          # API 匯總文件
│   │   ├── venue.js          # 場地相關 API
│   ├── assets/               # 資源文件夾
│   │   ├── styles/           # 樣式文件夾
│   │   │   ├── global.scss   # 全域樣式
│   │   │   ├── selector-common.scss # 選擇器通用樣式
│   ├── components/           # Vue 組件文件夾
│   │   ├── NavBar.vue        # 導航欄組件
│   │   ├── Toast.vue         # 提示組件
│   │   ├── booking/          # 預訂相關組件
│   │       ├── BookingModal.vue # 預訂彈窗組件
│   ├── router/               # 路由配置文件夾
│   │   ├── index.js          # 路由主配置文件
│   ├── stores/               # 狀態管理文件夾
│   ├── utils/                # 工具函數文件夾
│   │   ├── dateHelper.js     # 日期處理工具
│   │   ├── useToast.js       # 提示工具
│   ├── views/                # 視圖文件夾
│       ├── Placeholder.vue   # 佔位符視圖
│       ├── UnitSelector.vue  # 單位選擇視圖
│       ├── VenueCalendar.vue # 場地日曆視圖
│       ├── VenueSelector.vue # 場地選擇視圖
```

## 使用技術

本專案使用以下技術與套件：

- **Vue 3**: 作為核心框架，用於構建用戶界面。
- **Vue Router**: 用於管理應用的路由。
- **Pinia**: 作為狀態管理工具，提供簡單且高效的狀態管理。
- **FullCalendar**: 用於顯示和管理日曆事件，包含核心、日網格、時間網格及互動功能。
- **Axios**: 用於處理 HTTP 請求。
- **Day.js**: 用於處理和格式化日期。
- **Lucide Vue Next**: 提供圖標庫，方便在應用中使用。
- **Sass**: 用於撰寫樣式，提供更強大的 CSS 功能。
- **Vite**: 作為開發和構建工具，提供快速的開發體驗。
