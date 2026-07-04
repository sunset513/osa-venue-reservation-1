package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class EquipmentAvailabilityVO {
    private Boolean available;
    private List<ItemAvailability> items;
    private String message;

    @Data
    public static class ItemAvailability {
        private Long equipmentId;
        private String equipmentName;
        private Integer requestedQuantity;
        private Integer totalQuantity;
        private Integer minAvailableQuantity;
        private Boolean available;
        private Boolean venueRulePassed;
        private String message;
    }
}
