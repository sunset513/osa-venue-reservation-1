package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 場地日曆周視圖 VO (Value Object)
 * 用於 API 回傳場地周份日曆資料，包含詳細的時段資訊
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "場地周份日曆視圖物件，用於周份視圖展示，包含每日詳細的時段占用情況",
    example = """
    {
      "weekStart": "2026-04-06",
      "weekEnd": "2026-04-12",
      "days": [
        {
          "date": "2026-04-06",
          "dayOfWeek": "星期一",
          "approvedSlots": [8, 9],
          "userSlots": [8]
        }
      ]
    }
    """
)
public class VenueCalendarWeekVO {
    
    // ==========================================
    // 周份資訊
    // ==========================================
    
    @Schema(
        description = "周開始日期（周一，ISO 8601 格式：YYYY-MM-DD）",
        example = "2026-04-06",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String weekStart;
    
    @Schema(
        description = "周結束日期（周日，ISO 8601 格式：YYYY-MM-DD）",
        example = "2026-04-12",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String weekEnd;
    
    // ==========================================
    // 周內日期集合
    // ==========================================
    
    @Schema(
        description = "周內 7 日（周一至周日）的詳細資訊，包含時段占用情況",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<DayDetailSummary> days;
    
    // ==========================================
    // 內部類：日期詳細摘要
    // ==========================================
    
    /**
     * 單一日期的詳細摘要（包含時段資訊）
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "單日詳細資訊，包含時段占用情況")
    public static class DayDetailSummary {
        
        @Schema(
            description = "日期（ISO 8601 格式：YYYY-MM-DD）",
            example = "2026-04-06",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String date;
        
        @Schema(
            description = "星期幾，中文表示（例如：星期一、星期二...星期日）",
            example = "星期一",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String dayOfWeek;
        
        @Schema(
            description = """
            已通過審核的預約時段列表。
            使用 24 小時制索引（0-23），表示 0:00-23:00 的 24 個小時。
            例如 [8, 9] 表示 08:00-09:00 和 09:00-10:00 這兩個時段已被佔用
            """,
            example = "[8, 9]",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private List<Integer> approvedSlots;
        
        @Schema(
            description = """
            當前登入用戶的預約時段列表（無論審核狀態）。
            使用 24 小時制索引（0-23），表示用戶在該日期有預約的時段
            """,
            example = "[8]",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private List<Integer> userSlots;
    }
}

