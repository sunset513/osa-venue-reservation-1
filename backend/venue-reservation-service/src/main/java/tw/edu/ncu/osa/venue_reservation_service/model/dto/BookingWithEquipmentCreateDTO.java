package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BookingWithEquipmentCreateDTO {
    @Valid
    @NotNull(message = "場地預約資料不可為空")
    private BookingRequestDTO booking;

    @Valid
    private List<EquipmentBookingItemDTO> equipmentItems;
}
