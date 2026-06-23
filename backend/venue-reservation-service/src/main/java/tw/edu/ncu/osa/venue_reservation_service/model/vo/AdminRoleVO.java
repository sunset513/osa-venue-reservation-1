package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminRoleVO {
    private Long id;
    private String userId;
    private Integer level;
    private Boolean active;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
