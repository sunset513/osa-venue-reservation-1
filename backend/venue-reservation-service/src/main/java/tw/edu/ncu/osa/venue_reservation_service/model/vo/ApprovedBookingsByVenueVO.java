package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 場地已通過預約清單 VO
 * 用於公開查詢時依場地分組回傳
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "場地已通過預約清單（依場地分組）",
    example = """
    {
      "venueId": 1,
      "venueName": "會議室 A",
      "items": [
        {
          "bookingId": 501,
          "slots": [8, 9],
          "purpose": "專案討論"
        }
      ]
    }
    """
)
public class ApprovedBookingsByVenueVO {

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
        description = "已通過預約清單",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<ApprovedBookingSimpleVO> items;
}

