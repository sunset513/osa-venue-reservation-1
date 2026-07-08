package tw.edu.ncu.osa.venue_reservation_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tw.edu.ncu.osa.venue_reservation_service.interceptor.GatewaySessionAuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private GatewaySessionAuthInterceptor gatewaySessionAuthInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // ==========================================
        // 跨域資源共享 (CORS) 配置
        // ==========================================
        registry.addMapping("/api/**")
                // 允許來自前端的跨域請求
                .allowedOrigins(
                        "http://localhost",
                        "http://localhost:5173",      // Vite 開發服務器
                        "http://localhost:3000",      // 備用開發端口
                        "http://127.0.0.1:5173"    // 本地迴環地址
                )
                // 允許的 HTTP 方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允許的請求頭
                .allowedHeaders("*")
                // 允許認證相關的請求頭
                .allowCredentials(true)
                // 預檢請求的緩存時間（秒）
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(gatewaySessionAuthInterceptor)
                .addPathPatterns("/api/**")         // 攔截所有 API 請求
                .excludePathPatterns(
                        "/api/bookings/approved/three-venues"
                ); // 排除免驗證查詢接口
    }

}
