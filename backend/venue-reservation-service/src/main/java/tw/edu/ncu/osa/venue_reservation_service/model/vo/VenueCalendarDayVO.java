package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 場地日曆日視圖 VO (Value Object)
 * 用於 API 回傳場地單日日曆資料，包含最詳細的時段與預約資訊
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "場地單日日曆視圖物件，用於日份視圖展示，包含最詳細的時段與預約資訊",
    example = """
    {
      "venueId": 101,
      "venueName": "會議室 A",
      "date": "2026-04-06",
      "dayOfWeek": "星期一",
      "approvedSlots": [8, 9],
      "userSlots": [8],
      "userBookingDetails": [
        {
          "bookingId": 501,
          "slots": [8],
          "status": 2,
          "purpose": "專案討論",
          "createdAt": "2026-04-03T10:00:00"
        }
      ]
    }
    """
)
public class VenueCalendarDayVO {
    
    // ==========================================
    // 場地與日期資訊
    // ==========================================
    
    @Schema(
        description = "場地的唯一識別碼",
        example = "101",
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
        description = "查詢日期（ISO 8601 格式：YYYY-MM-DD）",
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
    
    // ==========================================
    // 時段占用情況
    // ==========================================
    
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
    
    // ==========================================
    // 用戶預約詳情
    // ==========================================
    
    @Schema(
        description = """
        該日期用戶所有預約的詳細資訊。
        包含用戶在該日期的所有預約（無論審核狀態），
        前端可根據各預約的狀態與時段詳情進一步展示
        """,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<UserBookingDetail> userBookingDetails;
    
    // ==========================================
    // 內部類：用戶預約詳情
    // ==========================================
    
    /**
     * 用戶單筆預約的詳細資訊
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "用戶單筆預約的詳細資訊")
    public static class UserBookingDetail {
        
        @Schema(
            description = "預約案的唯一編號",
            example = "501",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Long bookingId;
        
        @Schema(
            description = """
            該筆預約的時段列表。
            使用 24 小時制索引（0-23），表示該預約佔用的時段
            """,
            example = "[8]",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private List<Integer> slots;
        
        @Schema(
            description = """
            預約狀態
            - 0: 已撤回（申請人主動撤回）
            - 1: 審核中（尚未被核准或拒絕）
            - 2: 已通過（申請已獲批准）
            - 3: 已拒絕（申請被駁回）
            """,
            example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"0", "1", "2", "3"}
        )
        private Integer status;
        
        @Schema(
            description = "場地使用用途說明",
            example = "舉辦專案討論會議",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String purpose;
        
        @Schema(
            description = "預約建立時間（ISO 8601 格式：YYYY-MM-DDTHH:mm:ss）",
            example = "2026-04-03T10:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private LocalDateTime createdAt;
    }
}

