package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EquipmentBookingCreateDTO {
    @NotNull(message = "借用日期不可為空")
    @FutureOrPresent(message = "借用日期不能是過去的時間")
    private LocalDate borrowDate;

    @NotEmpty(message = "請至少選擇一個借用時段")
    private List<@Min(0) @Max(23) Integer> slots;

    @NotBlank(message = "請填寫借用用途")
    @Size(max = 255, message = "用途描述過長")
    private String purpose;

    @Valid
    @NotNull(message = "聯絡資訊不可為空")
    private BookingRequestDTO.ContactDTO contactInfo;

    private Long relatedVenueBookingId;

    @Valid
    @NotEmpty(message = "請至少選擇一項設備")
    private List<EquipmentBookingItemDTO> items;
}
