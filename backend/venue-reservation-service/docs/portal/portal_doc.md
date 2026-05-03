# NCU Portal OAuth2 串接完整技術文檔

**版本**: 0.1  
**日期**: 2025-05-01  
**狀態**: 生產環境連接正常 ✅

---

## 目錄

1. [概述](#概述)
2. [系統架構](#系統架構)
3. [依賴套件分析](#依賴套件分析)
4. [YAML 配置詳解](#yaml-配置詳解)
5. [Spring Security 設定](#spring-security-設定)
6. [CORS 跨域與攔截器配置](#cors-跨域與攔截器配置)
7. [測試端點](#測試端點)
8. [完整認證流程](#完整認證流程)
9. [核心修正說明](#核心修正說明)
10. [常見問題與排查](#常見問題與排查)

---

## 概述

本服務與 **NCU Portal（中央大學統一認證入口）** 進行 OAuth2 標準協議的整合，用戶可通過 Portal 單一帳號登入本服務，無需重複註冊。

### 整合特點

- **認證方式**: OAuth2 Authorization Code Grant (授權碼授予)
- **識別方式**: 使用者在 Portal 中的唯一識別碼 (`identifier`)
- **用戶信息**: 獲取 identifier、chinese-name、email 等基本資訊
- **安全機制**: 
  - 關閉 PKCE（Proof Key for Code Exchange），由 Portal 決定
  - 採用 client_secret_basic 認證
  - CSRF 防護禁用（因使用 OAuth2）

---

## 系統架構

```
┌─────────────────┐
│   前端應用      │
│  (Vite/React)   │
└────────┬────────┘
         │ 1. 使用者點擊登入
         │
┌────────▼────────────────────────────────────┐
│    本服務 (Spring Boot)                     │
│  localhost:8080                             │
└─────┬──────────────────────────────────────┘
      │ 2. 重導向到 Portal 授權端點
      │
┌─────▼──────────────────────────────────────┐
│  Portal OAuth2 授權伺服器                   │
│  https://portal.ncu.edu.tw/oauth2/auth     │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │  使用者登入、核准授權範圍            │   │
│  └─────────────────────────────────────┘   │
└─────┬──────────────────────────────────────┘
      │ 3. Portal 回傳授權碼 (Authorization Code)
      │
┌─────▼──────────────────────────────────────┐
│  本服務 /login/oauth2/code/ncu-portal      │
│  (後端接收授權碼)                          │
└─────┬──────────────────────────────────────┘
      │ 4. 本服務用授權碼 + client_id + client_secret 
      │    向 Portal 交換 Access Token
      │
┌─────▼──────────────────────────────────────┐
│  Portal Token 端點                         │
│  https://portal.ncu.edu.tw/oauth2/token    │
└─────┬──────────────────────────────────────┘
      │ 5. Portal 回傳 Access Token
      │
┌─────▼──────────────────────────────────────┐
│  本服務建立 Spring Security Session         │
│  使用者成功登入                             │
└──────────────────────────────────────────┘
```

---

## 依賴套件分析

### 核心 OAuth2 依賴

在 `pom.xml` 中定義的關鍵依賴：

```xml
<!-- Spring Boot 安全框架 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- OAuth2 客戶端庫（用於連接 Portal） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

### 版本資訊

| 套件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 4.0.3 | 主框架版本 |
| spring-boot-starter-security | 4.0.3 | 安全管理（自動匹配） |
| spring-boot-starter-oauth2-client | 4.0.3 | OAuth2 客戶端（自動匹配） |
| springdoc-openapi-starter-webmvc-ui | 3.0.2 | Swagger 文檔（可選） |
| lombok | 最新 | 程式碼簡化工具 |

### 依賴用途詳解

#### 1. spring-boot-starter-security
- 提供 Spring Security 框架
- 管理使用者認證與授權
- 處理會話（Session）管理
- 提供 CSRF 防護（已禁用）

#### 2. spring-boot-starter-oauth2-client
- 實現 OAuth2 客戶端邏輯
- 自動處理授權碼流程
- 負責 Token 交換與管理
- 整合於 Spring Security 認證體系

---

## YAML 配置詳解

### 檔案位置

```
src/main/resources/
├── application.yaml           # 通用配置
└── application-dev.yaml       # 開發環境配置 ← 當前使用
```

### application-dev.yaml 配置分析

#### 資料庫配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/venue_reservation_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 1qaz@WSX3edc
    driver-class-name: com.mysql.cj.jdbc.Driver
```

| 參數 | 說明 |
|------|------|
| `url` | MySQL 連接字串，指向本地 `venue_reservation_system` 資料庫 |
| `username` | 資料庫使用者（本地開發用 `root`） |
| `password` | 資料庫密碼 |
| `useSSL=false` | 本地開發不使用 SSL |
| `serverTimezone=UTC` | 時區設定為 UTC |

#### OAuth2 客戶端註冊配置

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          ncu-portal:
            client-name: OSANewWeb
            client-id: 20260422093447s0XnWjs7zmqR
            client-secret: vHz7h1CkTKFeMgEOYcrAwxvbtZErKMOBVepIF9YaD2d0K4V1y
            client-authentication-method: client_secret_basic
            authorization-grant-type: authorization_code
            redirect-uri: "http://localhost:8080/login/oauth2/code/ncu-portal"
            scope:
              - identifier
              - chinese-name
              - email
```

**配置項說明**:

| 項目 | 值 | 說明 |
|------|-----|------|
| **ncu-portal** | (標籤) | 客戶端名稱，決定登入路由 `/oauth2/authorization/ncu-portal` |
| client-name | OSANewWeb | 在 Portal 端登記的應用名稱 |
| client-id | 20260422093447... | Portal 核發的客戶端 ID |
| client-secret | vHz7h1CkTKFeMgEOYcrAwxvbtZErKMOBVepIF9YaD2d0K4V1y | Portal 核發的客戶端密鑰（**務必保密**） |
| **client-authentication-method** | client_secret_basic | 將 client_id 與 client_secret 編碼為 HTTP Basic Auth（而非 Body 中） |
| authorization-grant-type | authorization_code | 使用授權碼授予流程 |
| **redirect-uri** | http://localhost:8080/login/oauth2/code/ncu-portal | 授權後 Portal 將使用者重導向回本服務的路由 |
| scope | 陣列 | 要求的使用者資訊範圍 |

**授權範圍說明**:

| Scope | 用途 |
|-------|------|
| `identifier` | 使用者唯一識別碼（學號） |
| `chinese-name` | 使用者中文姓名 |
| `email` | 使用者電子郵件 |

#### OAuth2 提供者配置

```yaml
        provider:
          ncu-portal:
            authorization-uri: https://portal.ncu.edu.tw/oauth2/authorization
            token-uri: https://portal.ncu.edu.tw/oauth2/token
            user-info-uri: https://portal.ncu.edu.tw/apis/oauth/v1/info
            user-name-attribute: identifier
```

**提供者端點說明**:

| 端點 | URL | 用途 |
|------|-----|------|
| authorization-uri | https://portal.ncu.edu.tw/oauth2/authorization | 使用者授權端點（前端重導向至此） |
| token-uri | https://portal.ncu.edu.tw/oauth2/token | Token 交換端點（後端調用） |
| user-info-uri | https://portal.ncu.edu.tw/apis/oauth/v1/info | 取得使用者資訊端點（可選） |
| user-name-attribute | identifier | **關鍵**：用 `identifier` 欄位作為 OAuth2User 的 username |

#### 日誌配置

```yaml
logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.security.oauth2: DEBUG
```

在開發階段開啟 `DEBUG` 日誌，便於追蹤 OAuth2 流程：
- Token 交換的請求/回應
- 授權決策日誌
- Session 建立過程

---

## Spring Security 設定

### 檔案位置

```
src/main/java/tw/edu/ncu/osa/venue_reservation_service/config/SecurityConfig.java
```

### 核心設定分解

#### 1. SecurityFilterChain 配置

```java
@Bean
public SecurityFilterChain securityFilterChain(
    HttpSecurity http, 
    ClientRegistrationRepository clientRegistrationRepository
) throws Exception {
    http
        // CORS 支援（與前端跨域通信）
        .cors(Customizer.withDefaults())
        
        // CSRF 禁用（因使用 OAuth2）
        .csrf(csrf -> csrf.disable())
        
        // 授權規則
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/test/**").authenticated()  // 需認證
            .anyRequest().permitAll()                          // 其他允許通過
        )
        
        // OAuth2 登入配置
        .oauth2Login(oauth2 -> oauth2
            .authorizationEndpoint(auth -> auth
                .authorizationRequestResolver(
                    authorizationRequestResolver(clientRegistrationRepository)
                )
            )
            .tokenEndpoint(token -> token
                .accessTokenResponseClient(accessTokenResponseClient())
            )
        );

    return http.build();
}
```

**各區塊說明**:

| 配置 | 作用 |
|------|------|
| `.cors(Customizer.withDefaults())` | 啟用 CORS，允許跨域請求（由 WebConfig 詳細配置） |
| `.csrf(csrf -> csrf.disable())` | 禁用 CSRF 防護，因 OAuth2 已具備安全性 |
| `.requestMatchers("/api/test/**").authenticated()` | `/api/test/**` 路由需要使用者已認證 |
| `.anyRequest().permitAll()` | 其他路由允許匿名訪問 |
| `.oauth2Login(...)` | 啟用 OAuth2 登入流程 |

#### 2. PKCE 禁用（關鍵修正 1）

```java
private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
    ClientRegistrationRepository repository
) {
    DefaultOAuth2AuthorizationRequestResolver resolver = 
        new DefaultOAuth2AuthorizationRequestResolver(repository, "/oauth2/authorization");

    resolver.setAuthorizationRequestCustomizer(requestBuilder -> {
        // 移除 PKCE 參數
        requestBuilder.attributes(attrs -> {
            attrs.remove("code_challenge");
            attrs.remove("code_challenge_method");
            attrs.remove("code_verifier");
        });
        requestBuilder.additionalParameters(params -> {
            params.remove("code_challenge");
            params.remove("code_challenge_method");
        });
    });
    return resolver;
}
```

**為何禁用 PKCE？**

- NCU Portal 的 OAuth2 實現不支持 PKCE
- Spring Boot 4.0+ 預設啟用 PKCE
- 必須主動移除相關參數以符合 Portal 要求

#### 3. Token 交換配置（關鍵修正 2 & 3）

```java
@Bean
public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> 
    accessTokenResponseClient() {
    
    RestClientAuthorizationCodeTokenResponseClient tokenResponseClient = 
        new RestClientAuthorizationCodeTokenResponseClient();

    // 建立 RestClient（用於 Token 交換 HTTP 請求）
    RestClient restClient = RestClient.builder()
        // 允許 Response Body 多次讀取（用於日誌記錄）
        .requestFactory(
            new BufferingClientHttpRequestFactory(
                new SimpleClientHttpRequestFactory()
            )
        )
        
        // 預設 Header
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        
        // 訊息轉換器（JSON ↔ OAuth2 Token 物件）
        .messageConverters(converters -> {
            converters.add(0, new OAuth2AccessTokenResponseHttpMessageConverter());
        })
        
        // 日誌攔截器
        .requestInterceptor((request, body, execution) -> {
            log.info("=== Token Request Start ===");
            log.info("URL: {} {}", request.getMethod(), request.getURI());
            log.info("Body: {}", new String(body, StandardCharsets.UTF_8));

            var response = execution.execute(request, body);

            byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());
            log.info("Response Status: {}", response.getStatusCode());
            log.info("Response Body: {}", new String(responseBody, StandardCharsets.UTF_8));
            log.info("=== Token Request End ===");

            return response;
        })
        .build();

    // 核心修正：手動指定 Token 請求參數
    tokenResponseClient.setParametersConverter(grantRequest -> {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        
        parameters.add("grant_type", grantRequest.getGrantType().getValue());
        parameters.add("code", grantRequest.getAuthorizationExchange()
            .getAuthorizationResponse().getCode());
        parameters.add("redirect_uri", grantRequest.getAuthorizationExchange()
            .getAuthorizationRequest().getRedirectUri());
        
        // 關鍵：Portal 要求 Body 中也要有 client_id
        parameters.add("client_id", grantRequest.getClientRegistration().getClientId());
        parameters.add("client_secret", grantRequest.getClientRegistration()
            .getClientSecret());
        
        return parameters;
    });

    tokenResponseClient.setRestClient(restClient);
    return tokenResponseClient;
}
```

**Token 交換參數**:

| 參數 | 來源 | 用途 |
|------|------|------|
| grant_type | 常數 | 固定為 `authorization_code` |
| code | URL 參數 | Portal 回傳的授權碼 |
| redirect_uri | YAML 配置 | 必須與初始註冊時的 redirect-uri 相同 |
| client_id | YAML 配置 | 本服務在 Portal 端的客戶端 ID |
| client_secret | YAML 配置 | 本服務在 Portal 端的客戶端密鑰 |

**為何 client_id 需要在 Body 中？**

- 雖然配置了 `client_secret_basic`，Spring 會自動將 client_id:client_secret 編碼為 HTTP Basic Auth Header
- 但 Portal 額外要求 Body 中也要包含 client_id 和 client_secret
- 此為 Portal 特定的實現要求

---

## CORS 跨域與攔截器配置

### 檔案位置

```
src/main/java/tw/edu/ncu/osa/venue_reservation_service/config/WebConfig.java
```

### CORS 配置

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        // 允許的源
        .allowedOrigins(
            "http://localhost",
            "http://localhost:5173",      // Vite 開發伺服器
            "http://localhost:3000",      // 備用開發端口
            "http://127.0.0.1:5173"       // 本地迴環地址
        )
        // 允許的 HTTP 方法
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        // 允許的請求頭
        .allowedHeaders("*")
        // 允許認證 Cookie
        .allowCredentials(true)
        // 預檢快取時間
        .maxAge(3600);
}
```

**配置項說明**:

| 項目 | 值 | 說明 |
|------|-----|------|
| addMapping | /api/** | 作用於所有 API 路由 |
| allowedOrigins | 4 個源 | 允許這些前端應用跨域請求 |
| allowedMethods | 5 種 | GET、POST、PUT、DELETE、OPTIONS |
| allowedHeaders | * | 允許所有請求頭（包括自訂 Header） |
| allowCredentials | true | **重要**：允許 Cookie 與認證資訊 |
| maxAge | 3600 | 瀏覽器快取預檢結果 1 小時 |

**為何需要 allowCredentials(true)？**

- 允許瀏覽器在跨域請求時發送 Cookie
- Session ID 通常儲存在 Cookie 中
- 不設定此項，前端無法維持已登入狀態

### 攔截器配置

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(mockAuthInterceptor)
        .addPathPatterns("/api/**")                              // 作用路由
        .excludePathPatterns("/api/test/**", "/api/public/**");  // 排除路由
}
```

**攔截器說明**:

| 設定 | 說明 |
|------|------|
| mockAuthInterceptor | 自訂攔截器，用於模擬 / 驗證認證資訊 |
| addPathPatterns("/api/**") | 攔截所有 `/api/` 開頭的請求 |
| excludePathPatterns(...) | 排除測試端點和公開端點 |

---

## 測試端點

### 檔案位置

```
src/main/java/tw/edu/ncu/osa/venue_reservation_service/controller/TestController.java
```

### 端點定義

```java
@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/userinfo")
    public Map<String, Object> getUserInfo(
        @AuthenticationPrincipal OAuth2User principal
    ) {
        return principal.getAttributes();
    }
}
```

### 使用說明

#### 請求

```http
GET http://localhost:8080/api/test/userinfo
Authorization: Bearer <access_token>  (或 Cookie 中的 JSESSIONID)
```

#### 回應（成功）

```json
{
  "identifier": "100001",
  "chinese-name": "王小明",
  "email": "100001@ncu.edu.tw",
  "sub": "100001",
  "aud": "20260422093447s0XnWjs7zmqR",
  "iss": "https://portal.ncu.edu.tw",
  "iat": 1725000000,
  "exp": 1725003600
}
```

#### 回應（未認證）

```json
{
  "timestamp": "2025-05-01T10:30:00.000+0800",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

### @AuthenticationPrincipal 說明

- Spring Security 自動注入當前已認證的使用者物件
- `OAuth2User` 是一個 Principal 實現，包含 Portal 回傳的所有欄位
- 可通過 `principal.getAttributes()` 存取所有屬性

---

## 完整認證流程

### 時序圖

```
使用者              前端應用              本服務              Portal
  │                  │                    │                   │
  ├─ 點擊登入 ─────→ │                    │                   │
  │                  │                    │                   │
  │                  ├─ GET /oauth2/authorization/ncu-portal ─→
  │                  │  (redirect to Portal auth endpoint)    │
  │                  │                    │                   │
  │                  │◀─ 302 Redirect ────┤◀──────────────────┤
  │                  │  https://portal.ncu.edu.tw/oauth2/     │
  │                  │  authorization?                        │
  │                  │  client_id=...                         │
  │                  │  redirect_uri=...                      │
  │                  │  response_type=code                    │
  │                  │  scope=...                             │
  │◀─ 重導向到Portal ┤                    │                   │
  │                  │                    │                   │
  ├─ 在Portal登入 ──────────────────────────────────────────→│
  │ (輸入帳密、驗證)  │                    │                   │
  │                  │                    │                   │
  │                  │                    │                   │◀─ 核准授權
  │                  │◀─ 302 Redirect ────┤◀──────────────────┤
  │                  │  http://localhost:8080/login/oauth2/   │
  │                  │  code/ncu-portal?code=AUTH_CODE&       │
  │                  │  state=...                             │
  │                  │                    │                   │
  │                  ├─ GET /login/oauth2/code/ncu-portal ──→ │
  │                  │  (後端接收授權碼)   │                   │
  │                  │                    │                   │
  │                  │                    ├─ POST /oauth2/token → │
  │                  │                    │ (交換 Token)       │
  │                  │                    │ grant_type=auth... │
  │                  │                    │ code=AUTH_CODE     │
  │                  │                    │ client_id=...      │
  │                  │                    │ client_secret=...  │
  │                  │                    │                   │
  │                  │                    │◀─ 200 OK ─────────┤
  │                  │                    │ {access_token:..} │
  │                  │                    │                   │
  │                  │                    ├─ 建立 Session      │
  │                  │                    │ 儲存 OAuth2User    │
  │                  │                    │                   │
  │                  │◀─ 302 Redirect ────┤                   │
  │                  │  (或自動跳轉到前端)│                   │
  │                  │ + Set-Cookie      │                   │
  │                  │ JSESSIONID=...    │                   │
  │                  │                    │                   │
  │◀─ 登入成功 ───── ┤                    │                   │
  │                  │                    │                   │
  ├─ 後續請求 ──────→ │                    │                   │
  │  (攜帶 Cookie)    │                    │                   │
  │                  ├──────────────────→ │                   │
  │                  │ Cookie: JSESSIONID │                   │
  │                  │                    │                   │
  │                  │◀─ 200 OK ────────── ┤                   │
  │                  │ (已認證回應)        │                   │
  │                  │                    │                   │
```

### 步驟詳解

#### 第 1 步：使用者點擊登入

使用者在前端點擊「NCU Portal 登入」按鈕。

#### 第 2 步：前端重導向至授權端點

```
GET http://localhost:8080/oauth2/authorization/ncu-portal
```

Spring Security 攔截此請求，自動組建授權 URL 並重導向至 Portal：

```
https://portal.ncu.edu.tw/oauth2/authorization?
  client_id=20260422093447s0XnWjs7zmqR
  &redirect_uri=http://localhost:8080/login/oauth2/code/ncu-portal
  &response_type=code
  &scope=identifier+chinese-name+email
  &state=<隨機狀態碼>
```

（注：PKCE 參數已被移除）

#### 第 3 步：使用者在 Portal 登入與授權

使用者在 Portal 輸入帳號密碼並驗證後，授權本服務獲取其基本資訊。

#### 第 4 步：Portal 回傳授權碼

Portal 生成授權碼（一次性有效），重導向回本服務：

```
HTTP 302 Found
Location: http://localhost:8080/login/oauth2/code/ncu-portal?
  code=AUTH_CODE_XYZ
  &state=<狀態碼>
```

#### 第 5 步：後端交換 Access Token

Spring Security 自動執行此步驟（不涉及使用者）。

本服務向 Portal Token 端點發送 POST 請求：

```http
POST https://portal.ncu.edu.tw/oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic <base64(client_id:client_secret)>

grant_type=authorization_code
&code=AUTH_CODE_XYZ
&redirect_uri=http://localhost:8080/login/oauth2/code/ncu-portal
&client_id=20260422093447s0XnWjs7zmqR
&client_secret=vHz7h1CkTKFeMgEOYcrAwxvbtZErKMOBVepIF9YaD2d0K4V1y
```

Portal 驗證後回傳 Access Token：

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "identifier chinese-name email"
}
```

#### 第 6 步：建立使用者會話

Spring Security 使用 Access Token 獲取使用者資訊（若啟用 user-info-uri），或直接從 Token Claims 解析。

建立 `OAuth2User` 物件，存入 Spring Security Context，並生成 Session Cookie：

```http
Set-Cookie: JSESSIONID=ABC123DEF456; Path=/; HttpOnly
```

#### 第 7 步：重導向至前端

後端重導向至前端（通常是首頁或儀表板），前端接收到 Cookie，後續請求自動帶入。

#### 第 8 步：後續認證請求

所有後續 API 請求自動攜帶 Cookie：

```http
GET http://localhost:8080/api/test/userinfo
Cookie: JSESSIONID=ABC123DEF456
```

Spring Security 驗證 Cookie，允許存取受保護資源。

---

## 核心修正說明

### 為何需要這些修正？

本實現中有三個重要的修正，皆因 NCU Portal 的特殊實現要求。

### 修正 1：禁用 PKCE

**問題**:
- Spring Boot 4.0+ 預設為所有 OAuth2 授權碼流啟用 PKCE
- PKCE 是 RFC 7636 標準，提高安全性
- 但 NCU Portal 的實現不支持 PKCE

**解決方案**:

```java
resolver.setAuthorizationRequestCustomizer(requestBuilder -> {
    requestBuilder.attributes(attrs -> {
        attrs.remove("code_challenge");
        attrs.remove("code_challenge_method");
        attrs.remove("code_verifier");
    });
    requestBuilder.additionalParameters(params -> {
        params.remove("code_challenge");
        params.remove("code_challenge_method");
    });
});
```

移除授權請求中的 PKCE 參數。

**影響**:
- 降低安全性（但符合 Portal 實現）
- 沒有 code_challenge 驗證，授權碼交換時攻擊風險略增

### 修正 2：使用 BufferingClientHttpRequestFactory

**問題**:
- HttpClient 預設只能讀取一次 Response Body
- 日誌記錄器讀取 Body 後，Spring 的訊息轉換器無法再讀取

**解決方案**:

```java
BufferingClientHttpRequestFactory factory = 
    new BufferingClientHttpRequestFactory(
        new SimpleClientHttpRequestFactory()
    );

RestClient restClient = RestClient.builder()
    .requestFactory(factory)
    ...
    .build();
```

緩衝工廠允許 Body 被多次讀取。

**影響**:
- 增加記憶體使用（Response Body 會被緩衝）
- 便於日誌記錄和除錯

### 修正 3：Token 請求 Body 中補充 client_id 與 client_secret

**問題**:
- RFC 6749 標準規定，Token 端點可接受 HTTP Basic Auth 或 Body 參數
- Spring 預設採用 HTTP Basic Auth（`client_authentication_method: client_secret_basic`）
- NCU Portal 要求 Body 中也要有 client_id 和 client_secret

**解決方案**:

```java
tokenResponseClient.setParametersConverter(grantRequest -> {
    MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    parameters.add("grant_type", grantRequest.getGrantType().getValue());
    parameters.add("code", ...);
    parameters.add("redirect_uri", ...);
    // 補充 Body 中的 client 認證信息
    parameters.add("client_id", grantRequest.getClientRegistration().getClientId());
    parameters.add("client_secret", grantRequest.getClientRegistration().getClientSecret());
    return parameters;
});
```

手動控制 Token 交換參數。

**影響**:
- client_secret 會在 HTTP Body 中傳送（同時也在 Basic Auth Header 中）
- 需確保使用 HTTPS（生產環境）
- 提高與 Portal 的相容性

---

## 常見問題與排查

### Q1: 登入後仍然返回 401 Unauthorized

**可能原因**:

1. **Session 過期或未正確建立**
   - 檢查瀏覽器 Cookie 中是否有 `JSESSIONID`
   - 啟用 DEBUG 日誌，查看是否有異常日誌

2. **跨域 Cookie 未被發送**
   - 確認前端請求時設定 `credentials: 'include'`（Fetch API）或 `withCredentials: true`（Axios）
   - 確認 CORS 設定中有 `allowCredentials(true)`

3. **端點的授權規則設定不當**
   - 檢查 `SecurityFilterChain` 的 `.authorizeHttpRequests()`
   - 確認目標端點是否被標記為 `.authenticated()`

**排查步驟**:

```bash
# 1. 查看後端日誌
tail -f logs/venue-reservation-service.log | grep -i "oauth\|security\|token"

# 2. 檢查 Portal 交互日誌
# SecurityConfig 中已有詳細日誌，查看 Token Request/Response
```

### Q2: Token 交換失敗，Portal 返回 400 或 403

**可能原因**:

1. **redirect_uri 不匹配**
   - YAML 中的 `redirect-uri` 必須與 Portal 登記時的完全相同
   - 包括 http vs https、端口號等

2. **client_id 或 client_secret 錯誤**
   - 檢查 YAML 中是否正確複製
   - 是否包含多餘的空格

3. **授權碼已過期**
   - 授權碼通常只有 10 分鐘有效期
   - 使用者從登入到授權之間停留過久

**排查步驟**:

```yaml
# 啟用更詳細的日誌
logging:
  level:
    org.springframework.security: TRACE
    org.springframework.security.oauth2: TRACE
    org.springframework.web.client: DEBUG
```

查看 Token 交換的具體請求與回應。

### Q3: 跨域請求被阻止 (CORS error)

**錯誤訊息** (瀏覽器控制台):
```
Access to XMLHttpRequest at 'http://localhost:8080/api/test/userinfo' 
from origin 'http://localhost:5173' has been blocked by CORS policy
```

**排查步驟**:

1. 確認 WebConfig 中的 `allowedOrigins` 是否包含前端應用的 URL
2. 確認 `allowedMethods` 是否包含使用的 HTTP 方法
3. 確認 `allowCredentials(true)` 已設定

```java
.allowedOrigins("http://localhost:5173")  // 加入前端 URL
.allowedMethods("GET", "POST", "OPTIONS")
.allowCredentials(true)
```

### Q4: 如何在本地開發環境中調試 OAuth2 流程？

**建議方法**:

1. **查看完整的日誌輸出**
   
   ```yaml
   logging:
     level:
       org.springframework.security: DEBUG
       org.springframework.security.oauth2: DEBUG
       org.springframework.web.servlet.mvc.method.annotation: DEBUG
   ```

2. **使用瀏覽器開發者工具**
   - 檢查 Network 標籤中的重導向流程
   - 檢查 Cookies 標籤中的 JSESSIONID

3. **在代碼中加入中斷點**
   
   在 IDE 中，在 `SecurityConfig` 或 `TestController` 中設定中斷點，運行 Debug 模式

4. **檢查 Portal 的回應**
   
   SecurityConfig 中的 `requestInterceptor` 已記錄所有 Token 交換的請求與回應

### Q5: 生產環境中如何安全存儲 client_secret？

**建議做法**:

1. **不將 secrets 提交至版本控制**
   - 將 `application-dev.yaml` 中的 secrets 移至環境變數
   
   ```yaml
   spring:
     security:
       oauth2:
         client:
           registration:
             ncu-portal:
               client-secret: ${OAUTH2_CLIENT_SECRET}
   ```
   
   運行時指定環境變數:
   ```bash
   export OAUTH2_CLIENT_SECRET=vHz7h1CkTKFeMgEOYcrAwxvbtZErKMOBVepIF9YaD2d0K4V1y
   java -jar venue-reservation-service.jar --spring.profiles.active=prod
   ```

2. **使用 Spring Cloud Config**
   - 集中管理配置，支援加密存儲

3. **使用 Kubernetes Secrets / Docker Secrets**
   - 若部署在容器化環境

### Q6: 如何退出登入（Logout）？

**實現方式**:

Spring Security 提供內建的登出支援。在 SecurityConfig 中配置：

```java
.logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessUrl("/")
    .invalidateHttpSession(true)
    .deleteCookies("JSESSIONID")
)
```

前端調用：

```javascript
// 退出登入
window.location.href = "/logout";
```

### Q7: 如何同時支持 OAuth2 登入和傳統帳密登入？

**實現策略**:

1. 在 `SecurityFilterChain` 中同時配置 OAuth2 和 Form 登入
   
   ```java
   .oauth2Login(...)
   .formLogin(form -> form
       .loginPage("/login")
       .defaultSuccessUrl("/dashboard")
   )
   ```

2. 實現傳統的 `UserDetailsService`

此為進階用法，不在本文檔範圍內。

---

## 參考資源

- [Spring Security OAuth2 官方文檔](https://spring.io/projects/spring-security)
- [RFC 6749 - OAuth 2.0 Authorization Framework](https://tools.ietf.org/html/rfc6749)
- [RFC 7636 - PKCE (Proof Key for Code Exchange)](https://tools.ietf.org/html/rfc7636)
- [NCU Portal API 文檔](https://portal.ncu.edu.tw) (內部)

---

## 文檔版本記錄

| 版本 | 日期 | 更新內容 |
|------|------|----------|
| 0.1 | 2025-05-01 | 初版完成，涵蓋完整 OAuth2 串接說明 |

---

**最後更新**: 2025-05-01  
**維護者**: OSA Development Team

