package tw.edu.ncu.osa.venue_reservation_service.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String userId;   // portal 的 identifier
    private String name;     // 姓名
    private String email;    // 電子郵件
    private String role;     // USER, ADMIN
    private Long unitId;     // 用戶所屬單位
    private LocalDateTime loginAt;
    private LocalDateTime deletedAt;
}
