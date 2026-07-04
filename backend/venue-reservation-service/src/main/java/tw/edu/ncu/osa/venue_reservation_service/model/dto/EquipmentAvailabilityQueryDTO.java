package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EquipmentAvailabilityQueryDTO {
    @NotNull(message = "借用日期不可為空")
    private LocalDate borrowDate;

    @NotEmpty(message = "請至少選擇一個時段")
    private List<@Min(0) @Max(23) Integer> slots;

    private Long relatedVenueBookingId;
    private Long excludeEquipmentBookingId;

    @Valid
    @NotEmpty(message = "請至少選擇一項設備")
    private List<EquipmentBookingItemDTO> items;
}
