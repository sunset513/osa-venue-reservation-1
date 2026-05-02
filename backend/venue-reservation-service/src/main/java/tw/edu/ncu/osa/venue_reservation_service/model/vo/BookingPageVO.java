package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 預約分頁查詢結果 VO
 * 用於返回分頁查詢的預約列表及分頁資訊
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "預約分頁查詢結果物件",
    example = """
    {
      "total": 50,
      "pageNo": 1,
      "pageSize": 20,
      "totalPages": 3,
      "hasNext": true,
      "items": [
        {
          "id": 501,
          "venueName": "會議室 A",
          "bookingDate": "2026-04-10",
          "slots": [8, 9],
          "status": 1,
          "createdAt": "2026-04-03T10:00:00",
          "purpose": "專案討論",
          "pCount": 5,
          "contactInfo": "{\\"name\\":\\"王小明\\",\\"phone\\":\\"0912345678\\",\\"email\\":\\"xm@ncu.edu.tw\\"}",
          "equipments": ["麥克風", "投影機"]
        }
      ]
    }
    """
)
public class BookingPageVO {

    @Schema(
        description = "符合篩選條件的總記錄數",
        example = "50",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long total;

    @Schema(
        description = "當前頁碼",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer pageNo;

    @Schema(
        description = "每頁記錄數",
        example = "20",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer pageSize;

    @Schema(
        description = "總頁數（計算值：(total + pageSize - 1) / pageSize）",
        example = "3",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer totalPages;

    @Schema(
        description = "是否存在下一頁（計算值：pageNo < totalPages）",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean hasNext;

    @Schema(
        description = "當前頁的預約清單",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<BookingVO> items;

    // ==========================================
    // 便利方法
    // ==========================================

    /**
     * 根據總記錄數和頁面大小計算總頁數
     * @param total 總記錄數
     * @param pageSize 每頁記錄數
     * @return 總頁數
     */
    public static Integer calculateTotalPages(Long total, Integer pageSize) {
        if (total == null || total <= 0 || pageSize == null || pageSize <= 0) {
            return 0;
        }
        return (int) ((total + pageSize - 1) / pageSize);
    }

    /**
     * 判斷是否存在下一頁
     * @param pageNo 當前頁碼
     * @param totalPages 總頁數
     * @return 是否存在下一頁
     */
    public static Boolean calculateHasNext(Integer pageNo, Integer totalPages) {
        if (pageNo == null || totalPages == null) {
            return false;
        }
        return pageNo < totalPages;
    }
}

