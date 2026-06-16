# 前端登入狀態、同意書與身分分流流程

## 目的

本文件記錄前端從 mock token 認證轉為 login-service session 認證後，如何處理登入者狀態、同意書狀態與身分分流。

需求如下：

- 使用者透過 osa_infra Nginx gateway 與 login-service 完成 NCU Portal 登入。
- 所有登入使用者第一次進入 venue 系統時都必須簽同意書。
- 同一個有效瀏覽器 session 期間，已簽過同意書的使用者不需要重簽。
- 簽完同意書後依身分分流：
  - reviewer 導向 `/review`
  - 一般使用者導向一般借用入口
- session 過期或後端回 `401` 時，清除前端 session state 並導向 login-service。

## 整體資料流

```text
Browser -> http://localhost:8088/venue/
  |
  | Nginx auth_request 檢查 login-service session
  v
Vue Router beforeEach
  |
  | authSession.ensureCurrentUser()
  v
GET /api/venue/me
  |
  | Nginx rewrite /api/venue/me -> /api/me
  v
venue-backend /api/me
  |
  | 讀 SESSIONID Cookie -> Redis session:<id>
  v
CurrentUserVO
  |
  | { identifier, chineseName, email, role, isReviewer, defaultRoute }
  v
Pinia authSession store
  |
  | sessionStorage["venue:consent:<identifier>"]
  v
Router decision
```

## Router 決策流程

每次路由切換都會經過 `validateRouteAccess()`：

1. 先呼叫 `authSession.ensureCurrentUser()`。
2. 若 `/api/me` 失敗並回 `401`，由 axios interceptor 導向 `/auth/login`。
3. 若目標頁是 `/consent-agreement`，直接允許進入。
4. 若目前登入者尚未簽署同意書，導向：

   ```text
   /consent-agreement?redirect=<原本要去的路徑>
   ```

5. 若已簽署同意書，且目標是 `/`，reviewer 會導向 `/review`。
6. 其他路由繼續執行原本的單位/場地參數檢查。

## 同意書保存方式

同意書狀態由 Pinia store 管理，並存入 `sessionStorage`：

```text
venue:consent:<identifier> = accepted
```

範例：

```text
venue:consent:114423011 = accepted
```

選擇 `sessionStorage` 的原因：

- 符合「有效登入期間不用再簽」的需求。
- 不會像 `localStorage` 一樣長期保存。
- 重新開啟新的瀏覽器 session 時會重新要求同意。
- key 綁定 `identifier`，避免不同登入者共用同意狀態。

## 401 處理流程

所有 API request 都使用共用 axios instance。若後端回 `401`：

1. 動態載入 `authSession` store。
2. 呼叫 `clearSessionState()` 清除前端目前登入者與同意書狀態。
3. 導向：

   ```text
   /auth/login?redirect=<目前頁面路徑>
   ```

使用動態 import 是為了避免 API client 與 Pinia store 之間形成靜態循環 import。

## 新增檔案

### `frontend/src/api/auth.js`

新增目前登入者 API client：

```js
export const fetchCurrentUser = () => {
  return request.get("/me");
};
```

在 gateway 整合模式下，實際 request 會是：

```text
GET /api/venue/me
```

Nginx 會 rewrite 成 backend：

```text
GET /api/me
```

### `frontend/src/stores/authSession.js`

新增 Pinia store，集中管理：

- `currentUser`
- `hasLoadedUser`
- `isLoadingUser`
- `hasAcceptedConsent`
- `loadUserPromise`

主要方法：

- `ensureCurrentUser()`: 載入並快取 `/api/me` 回傳的目前使用者。
- `refreshConsentState()`: 依目前 `identifier` 從 `sessionStorage` 讀取同意書狀態。
- `acceptConsentForCurrentUser()`: 將目前使用者標記為已簽同意書。
- `getPostConsentRoute()`: 依身分取得簽署後預設導向。
- `clearSessionState()`: 清除前端 session state。

## 更動檔案

### `frontend/src/router/guards.js`

原本 guard 使用 `consentGate.js` 的記憶體變數判斷是否簽署同意書。現在改為：

- 使用 `useAuthSessionStore()`。
- 先載入目前登入者。
- 依登入者 `identifier` 檢查 `sessionStorage` 同意書狀態。
- 所有未簽署使用者都導向同意書。
- reviewer 簽署後進 `/` 時導向 `/review`。
- 保留原本單位與場地路由驗證。

### `frontend/src/router/index.js`

移除 `/consent-agreement` route 上舊的 `skipConsentGate` meta。

現在是否放行同意書頁由 `guards.js` 的 `isConsentRoute()` 判斷，不再依賴 route meta。

### `frontend/src/views/ConsentAgreement.vue`

原本直接呼叫 `acceptConsent()` 寫入記憶體狀態。現在改為：

- 使用 `useAuthSessionStore()`。
- 簽署後呼叫 `acceptConsentForCurrentUser()`。
- redirect 若是 `/`、無效路徑或 `/consent-agreement`，改用 `getPostConsentRoute()`。
- reviewer 簽完後會導向 `/review`。

### `frontend/src/api/index.js`

401 response 處理新增前端 session 清理：

- 動態 import `@/stores/authSession`。
- 呼叫 `clearSessionState()`。
- 再導向 login-service。

### 刪除 `frontend/src/utils/consentGate.js`

舊版本只使用模組層級記憶體：

```js
let acceptedConsent = false;
```

問題：

- refresh 後狀態消失。
- 無法綁定登入者。
- 無法配合 login-service session。
- 不適合處理 reviewer 與一般使用者分流。

因此正式流程改由 Pinia store + `sessionStorage` 管理。

### 刪除 `frontend/src/utils/__tests__/consentGate.test.js`

該測試只驗證舊的記憶體 utility，已不符合新流程，因此移除。

## 後端契約

前端依賴 backend `/api/me` 回傳：

```json
{
  "identifier": "114423011",
  "chineseName": "王小明",
  "email": "user@example.com",
  "role": "ADMIN",
  "isReviewer": true,
  "defaultRoute": "/review"
}
```

重要欄位：

- `identifier`: 用於同意書 sessionStorage key。
- `isReviewer`: 判斷是否 reviewer。
- `defaultRoute`: 簽署後的建議預設導向。

目前前端會特別處理一般使用者的 `defaultRoute` 若仍是 `/consent-agreement`，會 fallback 到 `/`，避免簽署後又回到同意書頁。

## 靜態檢查方式

本次修改驗證只做靜態檢查，不執行 build、test、Vite dev server 或 Docker。

確認舊 utility 不再被 active code 引用：

```powershell
rg -n "consentGate|skipConsentGate|resetConsentForTests|hasAcceptedConsent\\(|acceptConsent\\(" frontend/src
```

確認 `/api/me` 與 session store flow：

```powershell
rg -n "fetchCurrentUser|ensureCurrentUser|sessionStorage|venue:consent|acceptConsentForCurrentUser|getPostConsentRoute|clearSessionState|isReviewer" frontend/src
```

確認 `/me` 只由 auth API client 呼叫：

```powershell
rg -n "/me" frontend/src
```

## 預期行為

### Reviewer 第一次登入

1. 使用者登入 Portal。
2. login-service 導回 `/venue/`。
3. 前端呼叫 `/api/me`，取得 `isReviewer=true`。
4. 尚未簽同意書，導向 `/consent-agreement?redirect=/`。
5. 使用者簽署同意書。
6. 前端寫入 `sessionStorage["venue:consent:<identifier>"]`。
7. 因 redirect 是 `/`，前端依身分導向 `/review`。

### Reviewer 已簽署後再進 `/venue/`

1. 前端呼叫 `/api/me`。
2. 從 `sessionStorage` 找到已簽署狀態。
3. 目標路徑是 `/` 且 `isReviewer=true`。
4. 自動導向 `/review`。

### 一般使用者第一次登入

1. 登入後導回 `/venue/`。
2. 前端取得目前使用者。
3. 尚未簽同意書，導向同意書頁。
4. 簽署後導向一般入口。

### Session 過期

1. 任一 API 回 `401`。
2. 前端清除 Pinia session state。
3. 導向 login-service。
