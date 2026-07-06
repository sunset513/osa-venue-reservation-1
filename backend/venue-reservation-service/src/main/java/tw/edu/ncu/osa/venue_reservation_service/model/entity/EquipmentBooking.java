package tw.edu.ncu.osa.venue_reservation_service.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EquipmentBooking {
    private Long id;
    private String userId;
    private LocalDate borrowDate;
    private Integer timeSlots;
    private Integer status;
    private String purpose;
    private String contactInfo;
    private Long relatedVenueBookingId;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
