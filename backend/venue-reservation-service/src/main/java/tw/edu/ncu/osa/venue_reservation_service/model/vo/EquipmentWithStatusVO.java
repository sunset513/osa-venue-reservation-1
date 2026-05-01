package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 設備資訊及使用狀態輸出物件 (Value Object)
 * 用於 API 回傳單筆設備的詳細資訊與即時使用狀態
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "單筆設備的詳細資訊，包含使用狀態",
    example = """
    {
      "venueId": 1,
      "venueName": "會議室 A",
      "equipmentId": 5,
      "equipmentName": "投影機",
      "quantity": 2,
      "isInUse": false
    }
    """
)
public class EquipmentWithStatusVO {

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
        description = "場地名稱",
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
        description = "設備名稱",
        example = "投影機",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String equipmentName;

    /**
     * 設備數量
     */
    @Schema(
        description = "該場地擁有的設備數量",
        example = "2",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer quantity;

    /**
     * 是否正在使用中
     * 基於當前時間與預約資訊推算
     */
    @Schema(
        description = "設備是否正在使用中。true 表示有進行中的預約占用該設備，false 表示閒置",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean isInUse;
}

