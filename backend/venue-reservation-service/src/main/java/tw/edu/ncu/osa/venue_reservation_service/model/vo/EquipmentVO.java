package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "設備主檔回傳物件")
public class EquipmentVO {
    private Long id;
    private String name;
    private Integer totalQuantity;
    private String description;
    private String borrowNote;
    private Boolean venueRestricted;
    private List<EquipmentAllowedVenueVO> allowedVenues;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
