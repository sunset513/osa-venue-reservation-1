package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 預約申請 VO (Value Object)
 * 用於個人申請清單顯示時的回傳物件
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "預約申請詳情物件，用於查詢個人預約清單時的回應",
    example = """
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
    """
)
public class BookingVO {
    // ==========================================
    // 基礎資訊
    // ==========================================
    
    @Schema(
        description = "預約申請案的唯一編號",
        example = "501",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;
    
    @Schema(
        description = "場地名稱",
        example = "會議室 A",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String venueName;
    
    @Schema(
        description = "預約日期（ISO 8601 格式：YYYY-MM-DD）",
        example = "2026-04-10",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDate bookingDate;
    
    @Schema(
        description = "預約時段清單，使用 24 小時制索引（0-23 表示各小時）",
        example = "[8, 9]",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<Integer> slots;
    
    @Schema(
        description = """
        申請審核狀態
        - 0: 已撤回（申請人主動撤回）
        - 1: 審核中（尚未被核准或拒絕）
        - 2: 已通過（申請已獲批准）
        - 3: 已拒絕（申請被駁回）
        """,
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = {"0", "1", "2", "3"}
    )
    private Integer status;
    
    @Schema(
        description = "申請提交時間（ISO 8601 格式：YYYY-MM-DDTHH:mm:ss）",
        example = "2026-04-03T10:00:00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime createdAt;

    @Schema(
        description = "場地使用用途說明",
        example = "舉辦專案討論會議",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String purpose;

    @Schema(
        description = "預計使用人數",
        example = "5",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer pCount;

    @Schema(
        description = """
        聯絡人資訊（JSON 格式字串）
        包含 name（姓名）、phone（電話）、email（電子郵件）三個欄位
        """,
        example = """
        {"name":"王小明","phone":"0912345678","email":"xm@ncu.edu.tw"}
        """,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String contactInfo;

    @Schema(
        description = "所借用的設備名稱清單",
        example = """
        ["麥克風", "投影機", "音響"]
        """,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private List<String> equipments;
}
