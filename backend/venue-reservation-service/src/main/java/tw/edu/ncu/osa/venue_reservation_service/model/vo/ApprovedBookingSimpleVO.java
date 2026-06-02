package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 已通過預約簡化資訊 VO
 * 用於公開查詢場地已通過活動時的回傳物件
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "已通過預約簡化資訊物件",
    example = """
    {
      "bookingId": 501,
      "slots": [8, 9],
      "purpose": "專案討論"
    }
    """
)
public class ApprovedBookingSimpleVO {

    @Schema(
        description = "預約申請案的唯一編號",
        example = "501",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long bookingId;

    @Schema(
        description = "已通過預約的時段清單，使用 24 小時制索引（0-23 表示各小時）",
        example = "[8, 9]",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<Integer> slots;

    @Schema(
        description = "場地使用用途說明",
        example = "專案討論",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String purpose;
}

