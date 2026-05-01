package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * 設備借用歷史紀錄輸出物件 (Value Object)
 * 用於 API 回傳給前端的設備借用紀錄資訊
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "單筆設備借用紀錄，包含場地、設備、借用時間與事由",
    example = """
    {
      "venueId": 1,
      "venueName": "會議室 A",
      "equipmentId": 5,
      "equipmentName": "投影機",
      "borrowDate": "2026-05-15",
      "timeSlots": "09:00-12:00",
      "purpose": "系主任會議"
    }
    """
)
public class EquipmentBorrowRecordVO {

    /**
     * 場地唯一識別碼
     */
    @Schema(
        description = "場地唯一識別碼",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long venueId;

    /**
     * 場地名稱
     */
    @Schema(
        description = "場地名稱，如「會議室 A」",
        example = "會議室 A",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String venueName;

    /**
     * 設備唯一識別碼
     */
    @Schema(
        description = "設備唯一識別碼",
        example = "5",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long equipmentId;

    /**
     * 設備名稱
     */
    @Schema(
        description = "設備名稱，如「投影機」、「麥克風」等",
        example = "投影機",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String equipmentName;

    /**
     * 借用日期
     */
    @Schema(
        description = "借用日期",
        example = "2026-05-15",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDate borrowDate;

    /**
     * 借用時段（已轉換為可讀字串，如「09:00-12:00」或「09:00-12:00, 14:00-15:00」）
     */
    @Schema(
        description = "借用時段，以可讀格式表示，如『09:00-12:00』或『09:00-12:00, 14:00-15:00』",
        example = "09:00-12:00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String timeSlots;

    /**
     * 借用事由
     */
    @Schema(
        description = "借用用途說明",
        example = "系主任會議",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String purpose;
}

