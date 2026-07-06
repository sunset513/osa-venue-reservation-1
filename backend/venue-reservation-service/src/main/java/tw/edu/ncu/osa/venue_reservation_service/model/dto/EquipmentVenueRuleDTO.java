package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EquipmentVenueRuleDTO {
    @NotNull(message = "場地 ID 不可為空")
    @Schema(description = "允許使用此設備的場地 ID", example = "1")
    private Long venueId;

    @Size(max = 255, message = "規則備註過長")
    private String ruleNote;
}
