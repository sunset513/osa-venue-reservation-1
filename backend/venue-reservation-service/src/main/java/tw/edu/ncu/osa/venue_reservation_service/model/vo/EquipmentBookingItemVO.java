package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.Data;

@Data
public class EquipmentBookingItemVO {
    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private Integer quantity;
}
