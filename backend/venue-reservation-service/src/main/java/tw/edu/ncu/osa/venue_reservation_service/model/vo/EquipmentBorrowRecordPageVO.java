package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 設備借用歷史紀錄分頁輸出物件 (Value Object)
 * 用於 API 回傳分頁的設備借用紀錄資訊
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "設備借用歷史紀錄分頁結果，包含分頁資訊與借用紀錄清單",
    example = """
    {
      "totalCount": 150,
      "totalPages": 15,
      "currentPage": 1,
      "pageSize": 10,
      "data": [
        {
          "venueId": 1,
          "venueName": "會議室 A",
          "equipmentId": 5,
          "equipmentName": "投影機",
          "borrowDate": "2026-05-15",
          "timeSlots": "09:00-12:00",
          "purpose": "系主任會議"
        }
      ]
    }
    """
)
public class EquipmentBorrowRecordPageVO {

    /**
     * 借用紀錄總筆數
     */
    @Schema(
        description = "所有借用紀錄的總筆數",
        example = "150",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer totalCount;

    /**
     * 總頁數
     */
    @Schema(
        description = "根據頁面大小計算的總頁數",
        example = "15",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer totalPages;

    /**
     * 當前頁碼
     */
    @Schema(
        description = "當前頁碼（從 1 開始）",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer currentPage;

    /**
     * 每頁筆數
     */
    @Schema(
        description = "每頁顯示的筆數",
        example = "10",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer pageSize;

    /**
     * 當前頁的借用紀錄清單
     */
    @Schema(
        description = "當前頁的設備借用紀錄列表",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<EquipmentBorrowRecordVO> data;
}

