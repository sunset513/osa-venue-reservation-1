package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 預約查詢條件 DTO
 * 用於接收預約查詢的篩選條件，支援多維度查詢、排序和分頁
 */
@Data
@Schema(
    description = "預約查詢條件物件，支援多維度篩選和分頁",
    example = """
    {
      "venueId": 1,
      "statusList": [1, 2],
      "startDate": "2026-04-01",
      "endDate": "2026-04-30",
      "pageNo": 1,
      "pageSize": 20
    }
    """
)
public class BookingQueryDTO {

    @Schema(
        description = "場地 ID（可選）。為空表示查詢所有場地",
        example = "1",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Long venueId;

    @Schema(
        description = """
        預約狀態列表（可選）。為空表示查詢所有狀態
        - 0: 已撤回
        - 1: 審核中
        - 2: 已通過
        - 3: 已拒絕
        """,
        example = "[1, 2]",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private List<Integer> statusList;

    @Schema(
        description = "預約日期範圍開始（可選，ISO 8601 格式：YYYY-MM-DD）。為空表示無下限",
        example = "2026-04-01",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private LocalDate startDate;

    @Schema(
        description = "預約日期範圍結束（可選，ISO 8601 格式：YYYY-MM-DD）。為空表示無上限",
        example = "2026-04-30",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private LocalDate endDate;

    @Schema(
        description = "頁碼（最小值為 1）",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Min(value = 1, message = "頁碼最小值為 1")
    private Integer pageNo;

    @Schema(
        description = "每頁記錄數（最小值為 1，建議最大值為 100）",
        example = "20",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Min(value = 1, message = "每頁記錄數最小值為 1")
    private Integer pageSize;

    // ==========================================
    // 便利方法
    // ==========================================

    /**
     * 計算分頁時的 OFFSET 值
     * @return SQL OFFSET 值
     */
    public Integer getOffset() {
        if (pageNo == null || pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }
        return (pageNo - 1) * pageSize;
    }
}

