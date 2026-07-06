package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EquipmentStatusVO {
    private Long equipmentId;
    private String equipmentName;
    private Integer totalQuantity;
    private Integer borrowedQuantity;
    private Integer availableQuantity;
    private Boolean inUse;
    private List<ActiveBooking> activeBookings;

    @Data
    public static class ActiveBooking {
        private Long equipmentBookingId;
        private String userId;
        private LocalDate borrowDate;
        private List<Integer> slots;
        private Integer quantity;
        private String purpose;
        private String contactInfo;
        private Long relatedVenueBookingId;
        private Long relatedVenueId;
        private String relatedVenueName;
    }
}
