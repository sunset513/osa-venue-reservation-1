package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.Data;

@Data
public class BookingWithEquipmentCreateVO {
    private Long bookingId;
    private Long equipmentBookingId;
}
