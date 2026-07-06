package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.Data;

@Data
public class EquipmentAllowedVenueVO {
    private Long venueId;
    private String venueName;
    private String ruleNote;
}
