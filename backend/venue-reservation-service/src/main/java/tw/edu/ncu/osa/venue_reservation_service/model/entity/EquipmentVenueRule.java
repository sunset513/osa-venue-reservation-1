package tw.edu.ncu.osa.venue_reservation_service.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EquipmentVenueRule {
    private Long id;
    private Long equipmentId;
    private Long venueId;
    private String ruleNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
