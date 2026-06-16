# OSA Venue Reservation - 開發環境部署指南

本專案包含 Vue 3 frontend 與 Spring Boot backend。認證已改為搭配 login-service 與 osa_infra gateway 的 `SESSIONID` Cookie / Redis session 模型；前端不再送 mock token。

## 系統需求

- 已安裝 Docker Desktop 或 Docker Engine
- 已安裝 Docker Compose
- Windows 建議使用 WSL2 環境執行 Docker

## 開發模式

### Standalone 模式

用根目錄 `docker-compose.yml` 啟動本專案自己的 MySQL、Redis、backend、frontend，適合單獨開發 venue reservation。

```bash
docker compose up -d --build
```

預設入口：

- Frontend: `http://localhost`
- Backend: `http://localhost:8080`
- API base path: `/api`

根目錄 `.env` 可用以下設定覆蓋預設值：

```env
DB_PORT=3307
DB_NAME=venue_reservation_system
DB_ROOT_PASSWORD=OsaVenue2026
REDIS_PORT=6379
BACKEND_PORT=8080
SPRING_PROFILE=dev
FRONTEND_PORT=80
```

Standalone 模式不包含 login-service。需要登入狀態的 API 仍會依 `SESSIONID` Cookie 到 Redis 查 `session:<id>`；本模式主要用於基礎服務與前端畫面開發，完整 Portal 登入流程請使用 osa gateway 整合模式。

### osa gateway 整合模式

用 `docker-compose.osa-dev.yml` 讓本專案接到既有 `osa_infra` 的 MySQL、Redis、Nginx gateway，以及 login-service。

啟動順序：

```bash
cd C:\Users\wl110\Desktop\osa_infra
docker compose -f docker-compose.infra.yml up -d

cd C:\Users\wl110\Desktop\login-service
docker compose -f docker-compose.login.dev.yml up -d --build

cd C:\Users\wl110\Desktop\osa-venue-reservation
docker compose -f docker-compose.osa-dev.yml up -d --build
```

整合入口：

- Venue frontend: `http://localhost:8088/venue/`
- Venue API through gateway: `http://localhost:8088/api/venue/...`
- Gateway 會將 `/api/venue/*` rewrite 成 backend 的 `/api/*`

整合模式下 frontend 使用：

```env
VITE_API_BASE_URL=/api/venue
VITE_APP_BASE_PATH=/venue/
```

backend 使用 login-service 建立的 `SESSIONID` Cookie，到 infra Redis 查 `session:<SESSIONID>`，再建立 `UserContext`。審核權限由 `AUTH_REVIEWER_IDENTIFIER` 對應 Portal `identifier` 判斷。

## 本機非 Docker 前端開發

在 `frontend/.env.development` 設定：

```env
VITE_API_BASE_URL=/api
VITE_PROXY_TARGET=http://localhost:8080
VITE_APP_BASE_PATH=/
```

啟動：

```bash
cd frontend
pnpm install
pnpm dev
```

若要透過 osa gateway 測試 `/venue/` 子路徑，請改用 `docker-compose.osa-dev.yml`，讓 Nginx gateway 代理 frontend。

## 後端設定

backend 預設由環境變數指定 profile：

```env
SPRING_PROFILES_ACTIVE=dev
```

常用認證相關環境變數：

```env
AUTH_SESSION_COOKIE_NAME=SESSIONID
AUTH_GATEWAY_REDIS_PREFIX=session:
AUTH_SESSION_TTL_SECONDS=1800
AUTH_SESSION_SECURE_COOKIE=false
AUTH_SESSION_SAME_SITE=Lax
AUTH_REVIEWER_IDENTIFIER=114423011
```

`application.yaml` 只保留通用設定；不同環境應由 compose、shell 或部署平台指定 profile 與連線資訊。

## 常用指令

Standalone:

```bash
docker compose up -d --build
docker compose logs -f backend
docker compose logs -f frontend
docker compose stop
docker compose down
```

osa gateway 整合:

```bash
docker compose -f docker-compose.osa-dev.yml up -d --build
docker compose -f docker-compose.osa-dev.yml logs -f backend
docker compose -f docker-compose.osa-dev.yml logs -f frontend
docker compose -f docker-compose.osa-dev.yml down
```

重置 standalone 資料庫：

```bash
docker compose down -v
docker compose up -d
```

`-v` 會刪除資料庫 volume，請確認資料可重建後再使用。

## 認證資料流

1. 使用者進入 `http://localhost:8088/venue/`。
2. osa_infra Nginx 對 `/venue/` 執行 `auth_request /_auth_check`。
3. 未登入時導向 `/auth/login?redirect=/venue/...`。
4. login-service 完成 Portal OAuth 後建立 Redis `session:<UUID>`，並回寫 `SESSIONID=<UUID>` Cookie。
5. 前端呼叫 `/api/venue/...`，Nginx rewrite 成 backend `/api/...`。
6. backend interceptor 讀 `SESSIONID`，查 Redis profile，建立 `UserContext`。

## 注意事項

- 前端 active API code 應依賴 `SESSIONID` Cookie，不要加入舊式認證 header。
- `SESSIONID` Cookie 由 login-service 負責建立與登出清除。
- `osa_infra/nginx.conf` 的 upstream 名稱依賴 `venue-backend` 與 `venue-frontend` container name；整合 compose 需維持這兩個名稱。
