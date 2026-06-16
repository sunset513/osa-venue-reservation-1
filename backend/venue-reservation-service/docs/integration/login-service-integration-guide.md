# login-service 通用串接開發指南

## 適用對象

本文件提供給任何需要接入 login-service 的子系統。子系統可以使用任意技術棧，例如 Spring Boot、Node.js、Python、Go、PHP、.NET、前端 SPA 或傳統 server-rendered app。核心原則是：子系統不直接串 NCU Portal OAuth2，而是信任 login-service 建立的 gateway session。

## 核心契約

login-service 負責：

- 將未登入使用者導向 NCU Portal。
- 處理 OAuth2 callback。
- 建立系統內部 session id。
- 將使用者 profile 寫入 Redis。
- 回寫 `SESSIONID` Cookie。
- 提供 `/auth/session` 讓 gateway 或子系統確認登入狀態。
- 提供 `/auth/logout` 清除 session。

子系統負責：

- 讓 UI 與 API 走 osa_infra Nginx gateway。
- 不自行處理 Portal OAuth2。
- 從 Cookie 取得 `SESSIONID`。
- 到 Redis 查 `session:<SESSIONID>`。
- 將 Redis profile 轉成自己的使用者上下文。
- 依自己的規則做授權。

## 對外入口與路由

建議每個子系統配置兩組 gateway path：

```text
/<system>/
/api/<system>/
```

例如 venue 系統：

```text
/venue/
/api/venue/
```

UI path 應由 Nginx 做登入檢查：

```nginx
location ^~ /<system>/ {
    auth_request /_auth_check;
    error_page 401 = @auth_login;
    proxy_pass http://<system-frontend>:<port>;
}
```

API path 應轉發 Cookie，並視需要 rewrite 給子系統內部 API：

```nginx
location ^~ /api/<system>/ {
    rewrite ^/api/<system>/(.*)$ /api/$1 break;
    proxy_pass http://<system-backend>:<port>;
    proxy_set_header Cookie $http_cookie;
}
```

## 登入流程

```text
Browser -> GET /<system>/
Nginx -> auth_request /_auth_check
Nginx -> login-service /auth/session
login-service -> Redis session:<id>

若未登入：
Nginx -> 302 /auth/login?redirect=/<system>/...
login-service -> NCU Portal
NCU Portal -> /login/oauth2/code/ncu-portal
login-service -> Redis SET session:<uuid> <profile json>
login-service -> Set-Cookie: SESSIONID=<uuid>
login-service -> 302 /<system>/...
```

## Cookie 契約

預設 Cookie 名稱：

```text
SESSIONID
```

建議屬性：

```text
Path=/
HttpOnly
SameSite=Lax
Max-Age=<session ttl>
Secure=<true in HTTPS production, false in local HTTP dev>
```

只要所有子系統都掛在同一個 gateway host，例如 `localhost:8088`，瀏覽器後續請求 `/api/<system>/...` 會自動帶上 `SESSIONID`。

如果正式環境改成 HTTPS，應將 Secure 設為 `true`。如果系統跨子網域部署，需額外規劃 Cookie Domain、SameSite 與 CORS。

## Redis 契約

Redis key 格式：

```text
session:<SESSIONID>
```

Value 是 JSON 字串。通用欄位：

```json
{
  "identifier": "114423011",
  "chineseName": "王小明",
  "email": "user@example.com",
  "unit": null
}
```

欄位說明：

| 欄位 | 必填 | 說明 |
| --- | --- | --- |
| `identifier` | 是 | Portal 使用者唯一識別碼，通常作為子系統 user id |
| `chineseName` | 否 | 中文姓名 |
| `email` | 否 | Email |
| `unit` | 否 | 單位資訊，格式可依 login-service 後續擴充 |

建議子系統只把 Redis session 當作「已登入身分來源」，不要直接把其中的 role 當作授權依據。授權規則應由各子系統依自己的資料表、白名單、角色設定或管理端配置決定。

## 子系統 Backend 驗證流程

每個子系統 backend 應在受保護 API 前加入 middleware / filter / interceptor：

1. 允許 `OPTIONS` request 通過。
2. 從 request Cookie 讀取 `SESSIONID`。
3. 若 Cookie 不存在，回 `401 Unauthorized`。
4. 查 Redis `session:<SESSIONID>`。
5. 若 key 不存在或 TTL 過期，回 `401 Unauthorized`。
6. 解析 JSON，確認 `identifier` 存在。
7. 將 profile 轉成 request-local user context。
8. 視需要展延 Redis TTL。
9. 視需要回寫 `Set-Cookie` 更新 Max-Age。
10. request 結束後清除 request-local context。

偽程式：

```text
sessionId = request.cookies["SESSIONID"]
if sessionId is blank:
    return 401

profileJson = redis.get("session:" + sessionId)
if profileJson is blank:
    return 401

profile = parseJson(profileJson)
if profile.identifier is blank:
    redis.delete("session:" + sessionId)
    return 401

request.user = {
    id: profile.identifier,
    name: profile.chineseName,
    email: profile.email
}

redis.expire("session:" + sessionId, ttlSeconds)
response.setCookie("SESSIONID", sessionId, maxAge=ttlSeconds, path="/", httpOnly=true)
```

## Frontend 串接方式

前端不要保存 Portal token，也不要自行呼叫 Portal OAuth2 endpoint。

SPA 建議：

- API base URL 使用 gateway path，例如 `/api/<system>`。
- Request 攜帶 credentials。
- 收到 API `401` 時導向 `/auth/login?redirect=<current path>`。
- Router base path 對齊 gateway UI path，例如 `/venue/`。

Axios 範例：

```js
const api = axios.create({
  baseURL: "/api/<system>",
  withCredentials: true,
});

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      const currentPath = window.location.pathname + window.location.search + window.location.hash;
      window.location.assign(`/auth/login?redirect=${encodeURIComponent(currentPath)}`);
    }
    return Promise.reject(error);
  },
);
```

Fetch 範例：

```js
fetch("/api/<system>/me", {
  credentials: "include",
});
```

## Logout

通用登出 endpoint：

```text
POST /auth/logout
```

login-service 會刪除 Redis session 並清除 `SESSIONID` Cookie。子系統前端登出後可導向首頁或登入入口。

## 設定清單

每個子系統至少需要以下設定：

| 類別 | 設定 | 範例 |
| --- | --- | --- |
| UI base path | Gateway UI path | `/venue/` |
| API base path | Gateway API path | `/api/venue` |
| Backend upstream | Nginx target | `venue-backend:8080` |
| Frontend upstream | Nginx target | `venue-frontend:5173` |
| Redis host | Shared Redis service | `redis` |
| Redis key prefix | Session key prefix | `session:` |
| Cookie name | Login cookie | `SESSIONID` |
| Session TTL | Redis and Cookie TTL | `1800` |

## 安全注意事項

- 子系統不要接收前端傳來的 user id 作為登入身分。
- 子系統不要信任自訂 header 取代 session，除非 gateway 有完整簽章或內網保護設計。
- Redis session value 必須由 login-service 建立，子系統只讀取與展延。
- 正式 HTTPS 環境 Cookie 應使用 `Secure=true`。
- Session JSON 解析失敗時，建議刪除該 Redis key。
- 權限判斷應在子系統 backend 執行，不應只依賴前端路由隱藏。

## 排錯指令

查看 Redis session keys：

```powershell
docker exec osa_redis_dev redis-cli --scan --pattern "session:*"
```

查看 session 內容：

```powershell
docker exec osa_redis_dev redis-cli GET "session:<SESSIONID>"
```

查看 session TTL：

```powershell
docker exec osa_redis_dev redis-cli TTL "session:<SESSIONID>"
```

查看 gateway log：

```powershell
docker logs --tail 200 osa_nginx_gateway
```

查看子系統 backend log：

```powershell
docker logs --tail 200 <system-backend-container>
```

## 常見問題

### 登入成功後仍然回到登入頁

檢查 `redirect` 是否是允許的相對路徑，並確認 login-service 有成功寫入 `SESSIONID` Cookie。

### UI 可以打開，但 API 回 401

檢查 API request 是否走 gateway path `/api/<system>/...`，以及 Nginx API location 是否有轉發 Cookie。

### Redis 有 session，但 backend 仍回 401

確認 backend 使用的 Cookie 名稱與 Redis key prefix 是否與 login-service 一致。也檢查 profile JSON 是否有 `identifier`。

### 子系統 assets 404

SPA 需要設定 base path，使靜態資源與 router 都掛在 `/<system>/` 下。

### 改了 init SQL 但資料庫沒有變

MySQL Docker init SQL 只在 data volume 第一次初始化時執行。已存在 volume 時，需手動匯入 SQL 或刪除 volume 重建。
