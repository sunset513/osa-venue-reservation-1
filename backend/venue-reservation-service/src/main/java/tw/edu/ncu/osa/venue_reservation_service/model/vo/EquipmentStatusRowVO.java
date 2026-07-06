package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EquipmentStatusRowVO {
    private Long equipmentId;
    private String equipmentName;
    private Integer totalQuantity;
    private Long equipmentBookingId;
    private String userId;
    private LocalDate borrowDate;
    private Integer timeSlots;
    private Integer quantity;
    private String purpose;
    private String contactInfo;
    private Long relatedVenueBookingId;
    private Long relatedVenueId;
    private String relatedVenueName;
}
