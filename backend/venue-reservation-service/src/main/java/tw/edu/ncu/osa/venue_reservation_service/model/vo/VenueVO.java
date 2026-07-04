package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 場地詳細資訊輸出物件 (Value Object)
 * 用於 API 回傳給前端的場地資訊
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "場地詳細資訊物件，包含場地基本資訊",
    example = """
    {
      "id": 1,
      "unitId": 1,
      "name": "會議室 A",
      "capacity": 50,
      "description": "位於 3 樓，可容納 50 人會議。借用需提前 3 天提出申請。"
    }
    """
)
public class VenueVO {
    
    @Schema(
        description = "場地唯一識別碼。目前系統中有 2 個場地，ID 分別為 1 和 2",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;
    
    @Schema(
        description = "所屬管理單位的唯一識別碼。目前系統中有 2 個單位，ID 分別為 1（學生事務處）和 2（圖書館）",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long unitId;
    
    @Schema(
        description = "場地名稱，如「會議室 A」、「自習室 B」等",
        example = "會議室 A",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;
    
    @Schema(
        description = "場地的最大容納人數上限，用於用戶在預約時的人數驗證",
        example = "50",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer capacity;
    
    @Schema(
        description = """
        場地介紹或借用規則說明。包含場地的位置、設施、借用期限、取消規則等詳細資訊，
        幫助用戶了解場地的使用條件與限制
        """,
        example = "位於 3 樓，配備投影機和音響系統。可容納 50 人會議。借用需提前 3 天提出申請，如需取消請至少提前 24 小時告知。",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String description;
}

