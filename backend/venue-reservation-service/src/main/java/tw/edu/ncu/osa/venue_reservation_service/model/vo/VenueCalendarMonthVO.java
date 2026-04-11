package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 場地日曆月視圖 VO (Value Object)
 * 用於 API 回傳場地月份日曆資料，只顯示每日是否有預約，無時段詳情
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "場地月份日曆視圖物件，用於月份視圖展示",
    example = """
    {
      "year": 2026,
      "month": 4,
      "days": [
        {
          "date": "2026-04-01",
          "hasApprovedBooking": false,
          "hasUserBooking": false
        },
        {
          "date": "2026-04-02",
          "hasApprovedBooking": true,
          "hasUserBooking": true
        }
      ],
      "bookings": [
        {
          "id": 501,
          "venueName": "會議室 A",
          "bookingDate": "2026-04-10",
          "slots": [8, 9],
          "status": 2,
          "createdAt": "2026-04-03T10:00:00"
        }
      ]
    }
    """
)
public class VenueCalendarMonthVO {
    
    // ==========================================
    // 基礎資訊
    // ==========================================
    
    @Schema(
        description = "年份",
        example = "2026",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer year;
    
    @Schema(
        description = "月份（1-12）",
        example = "4",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer month;
    
    // ==========================================
    // 月份日期集合
    // ==========================================
    
    @Schema(
        description = "該月所有日期的摘要資訊（每日是否有已通過審核的預約或用戶自己的預約）",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<DaySimpleSummary> days;

    @Schema(
        description = """
        該月所有預約資訊（包括所有狀態：撤回、審核中、已通過、已拒絕）。
        前端可根據各預約的 status 與 slots 判斷時段占用情況。
        """,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<BookingVO> bookings;
    
    // ==========================================
    // 內部類：日期簡化摘要
    // ==========================================
    
    /**
     * 單一日期的簡化摘要（僅標記有無預約）
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "單日日期摘要資訊")
    public static class DaySimpleSummary {
        
        @Schema(
            description = "日期（ISO 8601 格式：YYYY-MM-DD）",
            example = "2026-04-01",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String date;
        
        @Schema(
            description = """
            是否有已通過審核的預約。
            true 表示該日有至少一筆狀態為「已通過（2）」的預約，
            false 表示該日沒有已通過審核的預約
            """,
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Boolean hasApprovedBooking;
        
        @Schema(
            description = """
            是否有用戶自己的預約（無論審核狀態）。
            true 表示該日有至少一筆屬於當前登入用戶的預約（狀態可為 0、1、2、3），
            false 表示該日沒有用戶自己的預約
            """,
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Boolean hasUserBooking;
    }
}

