# Venue Reservation 與 login-service 串接說明

## 目的

本文件說明 venue-reservation-service 如何透過 osa_infra 的 Nginx gateway 與 login-service 串接 NCU Portal 登入。venue 系統本身不處理 OAuth2 授權碼流程，也不保存登入 session；登入入口、Portal OAuth2、Session 建立與登出由 login-service 負責，venue backend 只驗證 login-service 寫入 Redis 的 session。

目前整合入口：

```text
http://localhost:8088/venue/
```

## 整體架構

```text
Browser
  |
  | GET /venue/
  v
osa_infra Nginx gateway (localhost:8088)
  |
  | auth_request /_auth_check
  v
login-service /auth/session
  |
  | read SESSIONID cookie -> Redis session:<id>
  v
Redis

Browser
  |
  | XHR /api/venue/...
  v
Nginx gateway
  |
  | rewrite /api/venue/... -> /api/...
  v
venue-backend
  |
  | read SESSIONID cookie -> Redis session:<id>
  v
Redis
```

## Nginx 路由契約

osa_infra 的 Nginx 是唯一對瀏覽器公開的入口，對外 port 由 compose 映射為 `8088:80`。

Venue UI:

```nginx
location = /venue {
    return 301 /venue/;
}

location ^~ /venue/ {
    auth_request /_auth_check;
    error_page 401 = @auth_login;

    set $target_venue_frontend venue-frontend;
    proxy_pass http://$target_venue_frontend:5173;
}
```

Venue API:

```nginx
location ^~ /api/venue/ {
    set $target_venue_backend venue-backend;
    rewrite ^/api/venue/(.*)$ /api/$1 break;
    proxy_pass http://$target_venue_backend:8080;
    proxy_set_header Cookie $http_cookie;
}
```

因此前端在 gateway 整合模式下必須使用：

```env
VITE_APP_BASE_PATH=/venue/
VITE_API_BASE_URL=/api/venue
```

## 登入流程

1. 使用者進入 `http://localhost:8088/venue/`。
2. Nginx 對 `/venue/` 發出 internal `auth_request /_auth_check`。
3. `/_auth_check` 會代理到 login-service 的 `/auth/session`。
4. 若沒有有效 `SESSIONID` cookie，login-service 回 `401`。
5. Nginx 使用 `@auth_login` 導向 `/auth/login?redirect=/venue/...`。
6. login-service 啟動 NCU Portal OAuth2 流程。
7. 使用者完成 Portal 登入後，login-service 建立 UUID session id。
8. login-service 將使用者 profile 寫入 Redis，key 形如 `session:<UUID>`。
9. login-service 回寫 `SESSIONID=<UUID>` Cookie，並導回原本的 `/venue/...`。
10. 後續 UI 與 API request 會自動帶上同一個 `SESSIONID` Cookie。

## Redis Session 格式

Redis key:

```text
session:<SESSIONID>
```

Value 是 JSON 字串，必要欄位如下：

```json
{
  "identifier": "114423011",
  "chineseName": "王小明",
  "email": "user@example.com",
  "unit": null
}
```

venue backend 目前會讀取：

- `identifier`: 必填，作為系統 user id。
- `chineseName`: 選填，作為顯示姓名。
- `chinese-name`: 選填，為相容舊格式的姓名欄位。
- `email`: 選填。

若 `identifier` 不存在、JSON 格式錯誤、或 key 不存在，backend 會回 `401`。

## Backend 驗證流程

核心實作在：

```text
src/main/java/tw/edu/ncu/osa/venue_reservation_service/interceptor/GatewaySessionAuthInterceptor.java
```

流程：

1. 攔截 `/api/**` request，`OPTIONS` 預檢直接放行。
2. 從 Cookie 讀取 `SESSIONID`。
3. 用 `auth.gateway-redis-prefix` 組出 Redis key，預設是 `session:<SESSIONID>`。
4. 從 Redis 取得 profile JSON。
5. 解析 profile，建立 `UserContext`。
6. 若路徑是 `/api/reviews` 或 `/api/reviews/**`，再檢查是否為 reviewer。
7. 展延 Redis TTL，並回寫同名 Cookie 更新 Max-Age。
8. request 結束後清除 `UserContext`。

目前 `/api/bookings/approved/two-venues` 為免登入查詢端點，在 `WebConfig` 中排除驗證。

## 角色與審核權限

venue backend 不從 Redis session 直接相信 role 欄位，而是用設定值判斷 reviewer：

```env
AUTH_REVIEWER_IDENTIFIER=114423011
```

當 session profile 的 `identifier` 等於 `AUTH_REVIEWER_IDENTIFIER`，backend 會將目前使用者視為 `ADMIN`；否則為 `USER`。

審核 API 權限判斷集中在 `GatewaySessionAuthInterceptor.requiresReviewer()`，目前保護：

```text
/api/reviews
/api/reviews/**
```

## Backend 設定

通用設定在 `application.yaml`：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      database: ${REDIS_DB:0}
      password: ${REDIS_PASSWORD:}

auth:
  session-cookie-name: ${AUTH_SESSION_COOKIE_NAME:SESSIONID}
  gateway-redis-prefix: ${AUTH_GATEWAY_REDIS_PREFIX:session:}
  session-ttl-seconds: ${AUTH_SESSION_TTL_SECONDS:1800}
  session-secure-cookie: ${AUTH_SESSION_SECURE_COOKIE:false}
  session-same-site: ${AUTH_SESSION_SAME_SITE:Lax}
  reviewer-identifier: ${AUTH_REVIEWER_IDENTIFIER:114423011}
```

osa gateway 整合模式使用 `docker-compose.osa-dev.yml`，backend 會加入 external `osa_network`，並連到 infra 的 Redis 與 MySQL：

```env
REDIS_HOST=redis
REDIS_PORT=6379
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/venue_reservation_system?...
```

## Frontend 設定

前端不再加入 mock authorization header。Axios 設定重點：

```js
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  withCredentials: true,
});
```

401 時前端導向 login-service：

```text
/auth/login?redirect=<current path>
```

在 gateway 整合模式中，`current path` 會是 `/venue/...`，login-service 登入完成後會導回 venue UI。

## 資料庫 Seed 注意事項

osa_infra 的 MySQL init SQL 只會在 MySQL volume 第一次初始化時執行。若 `osa_mysql_data` 已存在，修改 `mysql/init/*.sql` 後單純重啟 infra 不會重新匯入。

可用兩種方式更新 venue schema/data：

1. 手動匯入 SQL 到 `osa_mysql_dev`。
2. 刪除 infra MySQL volume 後重建 infra。

`02_seed_venue.sql` 應包含 venue 系統所需的 schema 與 seed data，不能只有 `CREATE DATABASE`。

## 排錯清單

### 1. 能進 Portal，但回 venue 後 API 沒資料

檢查 backend 是否成功啟動：

```powershell
docker logs --tail 200 venue-backend
```

檢查 MySQL 是否有 venue tables：

```powershell
docker exec osa_mysql_dev mysql -uroot -proot -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='venue_reservation_system';"
```

### 2. 登入後仍然 401

檢查 Redis session：

```powershell
docker exec osa_redis_dev redis-cli --scan --pattern "session:*"
docker exec osa_redis_dev redis-cli GET "session:<SESSIONID>"
docker exec osa_redis_dev redis-cli TTL "session:<SESSIONID>"
```

若 Redis 沒有 session，問題在 login-service 登入成功處理或 Cookie 寫入。若 Redis 有 session，但 backend 回 401，檢查 Cookie 是否有帶到 `/api/venue/...`，以及 `AUTH_SESSION_COOKIE_NAME` / `AUTH_GATEWAY_REDIS_PREFIX` 是否一致。

### 3. 審核 API 回 403

檢查 Redis profile 的 `identifier` 是否等於 backend 的 `AUTH_REVIEWER_IDENTIFIER`。

### 4. 前端資源 404

確認前端 gateway 模式有設定：

```env
VITE_APP_BASE_PATH=/venue/
```

並確認 Vue Router 使用 `createWebHistory(import.meta.env.BASE_URL)`。
