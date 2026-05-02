package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 場地分組的設備清單輸出物件 (Value Object)
 * 用於 API 回傳按場地分組的設備資訊
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "按場地分組的設備清單",
    example = """
    {
      "venueName": "會議室 A",
      "equipmentList": [
        {
          "venueId": 1,
          "venueName": "會議室 A",
          "equipmentId": 5,
          "equipmentName": "投影機",
          "quantity": 2,
          "isInUse": false
        }
      ]
    }
    """
)
public class EquipmentListByVenueVO {

    /**
     * 場地名稱
     */
    @Schema(
        description = "場地名稱",
        example = "會議室 A",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String venueName;

    /**
     * 該場地的設備清單
     */
    @Schema(
        description = "該場地的所有設備清單",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<EquipmentWithStatusVO> equipmentList;
}

