package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EquipmentReviewStatusUpdateDTO {
    @NotNull(message = "審核狀態不可為空")
    @Min(value = 1, message = "審核狀態值無效")
    @Max(value = 3, message = "審核狀態值無效")
    private Integer status;
}
