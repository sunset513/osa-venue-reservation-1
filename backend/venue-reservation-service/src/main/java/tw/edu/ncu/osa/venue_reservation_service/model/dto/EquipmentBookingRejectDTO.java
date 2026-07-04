package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EquipmentBookingRejectDTO {
    @NotBlank(message = "拒絕原因不可為空")
    @Size(max = 255, message = "拒絕原因過長")
    private String rejectReason;
}
