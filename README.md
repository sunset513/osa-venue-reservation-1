
# 🚀 OSA Venue Reservation - 開發環境部署指南
本指南旨在幫助前端開發者透過 Docker 快速啟動完整的前後端開發環境。

## 📋 系統需求
- 已安裝 Docker Desktop (Windows/Mac) 或 Docker Engine (Linux)
- 已安裝 Docker Compose
- (Windows 建議) 使用 WSL2 環境執行

## 🛠️ 1. 環境設定 (First-time Setup)
由於安全與環境差異，以下設定檔未包含在 Git 版本控制中。請在啟動前手動建立：

### A. 根目錄 .env
在專案根目錄建立 `.env`，用於設定 Docker Compose 變數：

```env
# Database
DB_PORT=3307               # 映射到本機的 Port (避開本機 3306)
DB_NAME=venue_reservation_system
DB_ROOT_PASSWORD=OsaVenue2026

# Backend
BACKEND_PORT=8080
SPRING_PROFILE=dev

# Frontend
FRONTEND_PORT=80           # 預設透過 http://localhost 存取
```

### B. 前端 .env.development

在 `frontend/` 目錄下建立 `.env.development`：

```env
VITE_API_BASE_URL=/api
VITE_PROXY_TARGET=http://localhost:8080
VITE_AUTH_TOKEN=mock-token-123
```

### C. 後端 application-dev.yaml

在 `backend/venue-reservation-service/src/main/resources/` 下確保有 `application-dev.yaml`，其資料庫連線應保持預設。
可以用下面這個檔案，如果用docker啟動來測試的話不用修改內容沒關係，如果要連到自己本地的db，就要修改password
```yaml
spring:
  docker:
    compose:
      enabled: false
  datasource:
    url: jdbc:mysql://localhost:3306/venue_reservation_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 用自己本地端mysql的密碼
    driver-class-name: com.mysql.cj.jdbc.Driver
```

## 📦 2. 啟動與停止

### 啟動完整環境

在根目錄執行，這會同時啟動 MySQL、Spring Boot 與 Vue 3：

```bash
docker compose up -d --build
```

* `--build`: 確保後端 Java 程式碼有重新打包進鏡像。
* `-d`: 在背景執行。

### 停止環境

```bash
docker compose stop
```

### 徹底關閉並移除容器

```bash
docker compose down
```

## 🗄️ 3. 資料庫管理與 Seed

### 初始資料注入

當你「第一次」啟動容器時，系統會自動執行根目錄的 `venue_seed_v1.sql`。

### 重置資料庫 (清除所有預約資料)

如果你修改了 Seed 檔案或想讓資料庫回到初始狀態，必須刪除掛載的資料卷 (Volume)：

```bash
docker compose down -v
docker compose up -d
```

⚠️ 注意：`-v` 會永久刪除資料庫內的資料，請謹慎使用。

## 👩‍💻 4. 開發者工作流 (Workflow)

### 前端代碼修改 (Hot Reload)

前端已透過 Docker Volume 掛載。修改 `frontend/src` 下的程式碼後，瀏覽器會自動即時更新，無需重啟 Docker。

### 後端代碼修改

Java 程式碼在容器內是編譯過的 `.jar` 檔，無法即時熱更新。修改後端程式碼後，必須執行以下指令重新編譯：

```bash
docker compose up -d --build backend
```

### 查看日誌 (Debug)

* 查看後端日誌：`docker compose logs -f backend`
* 查看前端日誌：`docker compose logs -f frontend`

## ❓ 常見問題排查

### 出現 403 Forbidden？

* 確保你在 `SecurityConfig.java` 中已關閉 CSRF。
* 確認 `WebConfig.java` 中的 CORS 已放行 `http://localhost`。
* 執行 `docker compose up -d --build backend` 確保後端版本是最新的。

### 無法連上 [http://localhost:5173？](http://localhost:5173？)

請改用 [http://localhost](http://localhost) (Port 80)，因為 Docker 已經幫你做好了 Port 映射。

---

### 💡 Tip for Frontend Collaborators:

本專案使用 Mock Token 進行開發，請確保你的 API 請求 Header 帶有
`Authorization: mock-token-123`
即可自動以測試學生身分登入。

```
