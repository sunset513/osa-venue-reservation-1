package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EquipmentBookingItemDTO {
    @NotNull(message = "設備 ID 不可為空")
    @Schema(description = "設備 ID", example = "1")
    private Long equipmentId;

    @NotNull(message = "借用數量不可為空")
    @Min(value = 1, message = "借用數量至少為 1")
    @Schema(description = "借用數量", example = "2")
    private Integer quantity;
}
