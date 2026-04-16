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
    private String role;     // USER, ADMIN
    private Long unitId;     // 租用場地 ID
    private Integer isDeleted; // 0: normal, 1: deleted
    private LocalDateTime deletedAt;
}
