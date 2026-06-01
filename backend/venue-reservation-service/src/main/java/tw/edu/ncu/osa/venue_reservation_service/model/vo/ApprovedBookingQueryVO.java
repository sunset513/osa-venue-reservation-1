package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已通過預約查詢用 VO
 * 用於 Mapper 查詢結果的資料承接
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "已通過預約查詢用物件")
public class ApprovedBookingQueryVO {

    @Schema(
        description = "預約申請案的唯一編號",
        example = "501",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long bookingId;

    @Schema(
        description = "場地 ID",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long venueId;

    @Schema(
        description = "場地名稱",
        example = "會議室 A",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String venueName;

    @Schema(
        description = "預約時段遮罩（24-bit）",
        example = "768",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer timeSlots;

    @Schema(
        description = "場地使用用途說明",
        example = "專案討論",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String purpose;
}

