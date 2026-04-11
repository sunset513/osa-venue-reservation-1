package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理單位輸出物件 (Value Object)
 * 用於 API 回傳給前端的單位資訊
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "管理單位資訊物件，用於標識系統中不同的管理單位",
    example = """
    {
      "id": 1,
      "name": "學生事務處",
      "code": "SAA"
    }
    """
)
public class UnitVO {
    
    @Schema(
        description = "單位唯一識別碼",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;
    
    @Schema(
        description = "單位名稱，如「學生事務處」、「圖書館」等",
        example = "學生事務處",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;
    
    @Schema(
        description = "單位代碼，用於系統內部識別，例如 SAA 代表 Student Affairs Administration",
        example = "SAA",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String code;
}

