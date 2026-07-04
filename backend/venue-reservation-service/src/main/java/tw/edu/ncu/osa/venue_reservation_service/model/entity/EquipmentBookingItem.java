package tw.edu.ncu.osa.venue_reservation_service.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EquipmentBookingItem {
    private Long id;
    private Long equipmentBookingId;
    private Long equipmentId;
    private Integer quantity;
}
