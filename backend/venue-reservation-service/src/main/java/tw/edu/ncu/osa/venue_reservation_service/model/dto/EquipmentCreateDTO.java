package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "新增設備主檔請求 DTO")
public class EquipmentCreateDTO {
    @NotBlank(message = "設備名稱不可為空")
    @Size(max = 100, message = "設備名稱過長")
    @Schema(description = "設備名稱", example = "無線麥克風")
    private String name;

    @NotNull(message = "設備總數量不可為空")
    @Min(value = 1, message = "設備總數量至少為 1")
    @Schema(description = "同一時段可核准借出的總數量", example = "4")
    private Integer totalQuantity;

    @Size(max = 255, message = "設備說明過長")
    private String description;

    @Size(max = 255, message = "借用注意事項過長")
    private String borrowNote;

    @Valid
    @Schema(description = "允許場地規則。空陣列或 null 表示不限場地")
    private List<EquipmentVenueRuleDTO> venueRules;
}
