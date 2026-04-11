package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 設備資訊輸出物件 (Value Object)
 * 用於 API 回傳給前端的設備資訊
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "可借用設備的詳細資訊，包含設備編號與名稱",
    example = """
    {
      "id": 1,
      "name": "投影機"
    }
    """
)
public class EquipmentVO {
    
    @Schema(
        description = "設備唯一識別碼",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;
    
    @Schema(
        description = "設備名稱，如「投影機」、「麥克風」、「音響」等",
        example = "投影機",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;
}

