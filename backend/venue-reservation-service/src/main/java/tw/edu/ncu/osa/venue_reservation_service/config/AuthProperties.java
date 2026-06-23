package tw.edu.ncu.osa.venue_reservation_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
    private String sessionCookieName = "SESSIONID";
    private String gatewayRedisPrefix = "session:";
    private long sessionTtlSeconds = 1800;
    private boolean sessionSecureCookie = false;
    private String sessionSameSite = "Lax";

    public Duration getSessionTtl() {
        return Duration.ofSeconds(sessionTtlSeconds);
    }

    public String getNormalizedSameSite() {
        if (sessionSameSite == null || sessionSameSite.isBlank()) {
            return "Lax";
        }

        String value = sessionSameSite.trim().toLowerCase(Locale.ROOT);
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
}
