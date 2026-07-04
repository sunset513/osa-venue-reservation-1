package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EquipmentBookingVO {
    private Long id;
    private String userId;
    private LocalDate borrowDate;
    private List<Integer> slots;
    private Integer status;
    private String purpose;
    private String contactInfo;
    private Long relatedVenueBookingId;
    private Long relatedVenueId;
    private String relatedVenueName;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String rejectReason;
    private Integer version;
    private List<EquipmentBookingItemVO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
