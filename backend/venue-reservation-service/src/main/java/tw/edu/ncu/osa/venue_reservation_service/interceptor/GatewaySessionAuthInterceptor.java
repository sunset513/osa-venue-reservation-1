package tw.edu.ncu.osa.venue_reservation_service.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tw.edu.ncu.osa.venue_reservation_service.common.result.Result;
import tw.edu.ncu.osa.venue_reservation_service.config.AuthProperties;
import tw.edu.ncu.osa.venue_reservation_service.mapper.AdminRoleMapper;
import tw.edu.ncu.osa.venue_reservation_service.mapper.UserMapper;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.AdminRole;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.User;
import tw.edu.ncu.osa.venue_reservation_service.util.UserContext;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewaySessionAuthInterceptor implements HandlerInterceptor {
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";
    private static final int LEVEL_ONE_ADMIN = 1;

    private final StringRedisTemplate redisTemplate;
    private final AuthProperties authProperties;
    private final UserMapper userMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        String sessionId = resolveSessionId(request);

        if (sessionId == null || sessionId.isBlank()) {
            log.warn("Gateway session missing - method={}, path={}", request.getMethod(), path);
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "尚未登入或登入狀態已失效");
            return false;
        }

        String redisKey = authProperties.getGatewayRedisPrefix() + sessionId;
        String rawProfile;
        try {
            rawProfile = redisTemplate.opsForValue().get(redisKey);
        } catch (Exception ex) {
            log.error("Unable to read Gateway session from Redis - method={}, path={}", request.getMethod(), path, ex);
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "登入狀態目前無法驗證");
            return false;
        }

        if (rawProfile == null || rawProfile.isBlank()) {
            log.warn("Gateway session expired - method={}, path={}", request.getMethod(), path);
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "登入狀態已過期");
            return false;
        }

        GatewayProfile profile = parseProfile(rawProfile, redisKey);
        if (profile == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "登入資料無效");
            return false;
        }

        String displayName = firstNonBlank(profile.chineseName(), profile.identifier());
        User user;
        AdminRole adminRole;
        try {
            user = userMapper.selectByUserId(profile.identifier());
            if (user != null && user.getDeletedAt() != null) {
                log.warn("Banned user login denied - method={}, path={}, identifier={}",
                        request.getMethod(), path, profile.identifier());
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "帳號已停用，無法登入");
                return false;
            }

            adminRole = adminRoleMapper.selectActiveByUserId(profile.identifier());
            String role = adminRole == null ? ROLE_USER : ROLE_ADMIN;
            if (user == null) {
                userMapper.insertPortalUser(profile.identifier(), displayName, profile.email(), role);
            } else {
                userMapper.updateLoginAt(profile.identifier());
            }
            user = userMapper.selectByUserId(profile.identifier());
            user.setRole(role);
        } catch (Exception ex) {
            log.error("Unable to sync Gateway user - method={}, path={}, identifier={}",
                    request.getMethod(), path, profile.identifier(), ex);
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "登入者資料目前無法同步");
            return false;
        }

        UserContext.setUser(new User(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getUnitId(),
                user.getLoginAt(),
                user.getDeletedAt()
        ));

        if (requiresReviewer(path) && !ROLE_ADMIN.equals(user.getRole())) {
            log.warn("Reviewer permission denied - method={}, path={}, identifier={}",
                    request.getMethod(), path, profile.identifier());
            UserContext.remove();
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "無審核權限");
            return false;
        }

        if (requiresEquipmentManager(path, request.getMethod()) && !ROLE_ADMIN.equals(user.getRole())) {
            log.warn("Equipment manager permission denied - method={}, path={}, identifier={}",
                    request.getMethod(), path, profile.identifier());
            UserContext.remove();
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "無設備管理權限");
            return false;
        }

        if (requiresLevelOneAdmin(path) && !isLevelOneAdmin(adminRole)) {
            log.warn("Admin role management permission denied - method={}, path={}, identifier={}",
                    request.getMethod(), path, profile.identifier());
            UserContext.remove();
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "無管理員帳號管理權限");
            return false;
        }

        if (!refreshSession(redisKey, sessionId, response)) {
            UserContext.remove();
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "登入狀態目前無法展延");
            return false;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.remove();
    }

    private GatewayProfile parseProfile(String rawProfile, String redisKey) {
        try {
            JsonNode root = objectMapper.readTree(rawProfile);
            if (!root.isObject()) {
                throw new IllegalArgumentException("Gateway session is not a JSON object");
            }

            String identifier = readText(root, "identifier");
            if (identifier == null || identifier.isBlank()) {
                throw new IllegalArgumentException("Gateway session does not contain identifier");
            }

            return new GatewayProfile(
                    identifier,
                    firstNonBlank(readText(root, "chineseName"), readText(root, "chinese-name")),
                    readText(root, "email")
            );
        } catch (Exception ex) {
            log.warn("Invalid Gateway session profile; deleting Redis key={}", redisKey, ex);
            deleteInvalidSession(redisKey);
            return null;
        }
    }

    private String readText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }

        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private boolean refreshSession(String redisKey, String sessionId, HttpServletResponse response) {
        try {
            Boolean refreshed = redisTemplate.expire(redisKey, authProperties.getSessionTtl());
            if (!Objects.equals(refreshed, Boolean.TRUE)) {
                return false;
            }
        } catch (Exception ex) {
            log.warn("Failed to refresh Redis session TTL for key={}", redisKey, ex);
            return false;
        }

        ResponseCookie cookie = ResponseCookie.from(authProperties.getSessionCookieName(), sessionId)
                .path("/")
                .httpOnly(true)
                .secure(authProperties.isSessionSecureCookie())
                .sameSite(authProperties.getNormalizedSameSite())
                .maxAge(authProperties.getSessionTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return true;
    }

    private void deleteInvalidSession(String redisKey) {
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception ex) {
            log.warn("Failed to delete invalid Gateway session key={}", redisKey, ex);
        }
    }

    private boolean requiresReviewer(String path) {
        return "/api/reviews".equals(path) || path.startsWith("/api/reviews/")
                || "/api/equipment-reviews".equals(path) || path.startsWith("/api/equipment-reviews/");
    }

    private boolean requiresEquipmentManager(String path, String method) {
        if (!"/api/equipments".equals(path) && !path.startsWith("/api/equipments/")) {
            return false;
        }
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private boolean requiresLevelOneAdmin(String path) {
        return "/api/admin-roles".equals(path) || path.startsWith("/api/admin-roles/");
    }

    private boolean isLevelOneAdmin(AdminRole adminRole) {
        return adminRole != null
                && adminRole.getDeletedAt() == null
                && adminRole.getLevel() != null
                && adminRole.getLevel() == LEVEL_ONE_ADMIN;
    }

    private String resolveSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (authProperties.getSessionCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(message)));
    }

    private record GatewayProfile(String identifier, String chineseName, String email) {
    }
}
