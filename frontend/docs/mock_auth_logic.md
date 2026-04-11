# 開發階段的暫時性身分校驗邏輯

## 後端邏輯

這是後端目前的身分校驗邏輯，因為後續要串 portal，所以先用這種簡陋的方式處理

```java
// venue_reservation_service/interceptor/MockAuthInterceptor.java
@Component
public class MockAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");

        // MVP 階段：只要有這個 mock-token，就自動登入一個測試學生
        if ("mock-token-123".equals(token)) {
            User mockUser = new User("110502000", "中央大學測試生", "STUDENT", 1L);
            UserContext.setUser(mockUser);
            return true;
        }

        // 未來這裡會改成解析真正的 JWT 或 Portal Token
        response.setStatus(401);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 請求處理完畢後，清空 ThreadLocal 確保安全
        UserContext.remove();
    }
}

// venue_reservation_service.config/WebConfig.java

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private MockAuthInterceptor mockAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mockAuthInterceptor)
                .addPathPatterns("/api/**")         // 攔截所有 API 請求
                .excludePathPatterns("/api/public/**"); // 排除公開 API
    }

    /**
     * 配置 ObjectMapper Bean，用於 JSON 序列化/反序列化
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
```

## 前端邏輯

目前在 api/request.js 中攔截發出去的請求，先加上 mock-token-123 再送到後端

```javascript
// src/api/index.js
import axios from "axios";

// 建立 Axios 實例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  timeout: 10000, // 請求超時時間 10 秒
  headers: {
    "Content-Type": "application/json",
  },
});

// Request 攔截器 (發送請求前)
request.interceptors.request.use(
  (config) => {
    // 配合後端 MockAuthInterceptor，帶入暫時性身分校驗 Token
    // 未來串接 Portal 或 JWT 時，可改從 localStorage 或 Pinia Store 讀取
    const mockToken = "mock-token-123";

    if (mockToken) {
      config.headers["Authorization"] = mockToken;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

..... 其他邏輯

export default request;

```
