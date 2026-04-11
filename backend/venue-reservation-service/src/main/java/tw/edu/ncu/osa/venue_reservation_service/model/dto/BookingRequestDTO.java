package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 預約申請提交/修改請求 DTO
 * 用於接收前端提交的預約申請資料
 */
@Data
@Schema(
    description = "預約申請提交/修改請求物件",
    example = """
    {
      "venueId": 1,
      "bookingDate": "2026-04-10",
      "slots": [8, 9],
      "purpose": "專案討論",
      "participantCount": 5,
      "contactInfo": {
        "name": "王小明",
        "email": "xm@ncu.edu.tw",
        "phone": "0912345678"
      },
      "equipmentIds": [1, 2]
    }
    """
)
public class BookingRequestDTO {

    @Schema(
        description = "場地唯一識別碼",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "場地 ID 不可為空")
    private Long venueId;

    @Schema(
        description = "預約日期（ISO 8601 格式：YYYY-MM-DD）",
        example = "2026-04-10",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "預約日期不可為空")
    @FutureOrPresent(message = "預約日期不能是過去的時間")
    private LocalDate bookingDate;

    @Schema(
        description = "預約時段列表，使用 24 小時制索引（0-23 表示 0:00-23:00）。例如 [8, 9] 表示預約 08:00-09:00 和 09:00-10:00 兩個時段",
        example = "[8, 9]",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotEmpty(message = "請至少選擇一個預約時段")
    private List<@Min(0) @Max(23) Integer> slots;

    @Schema(
        description = "場地使用用途說明（最多 255 字符）",
        example = "舉辦專案討論會議",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "請填寫使用用途")
    @Size(max = 255, message = "用途描述過長")
    private String purpose;

    @Schema(
        description = "預計使用人數（最少 1 人）",
        example = "5",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Min(value = 1, message = "預估人數至少需為 1 人")
    private Integer participantCount;

    @Schema(
        description = "聯絡人詳細資訊物件",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Valid
    @NotNull(message = "聯絡資訊不可為空")
    private ContactDTO contactInfo;

    @Schema(
        description = "借用設備 ID 列表（可選）。若不借用設備可省略此欄位或傳入空陣列",
        example = "[1, 2]",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private List<Long> equipmentIds;

    /**
     * 聯絡人資訊 DTO
     */
    @Data
    @Schema(description = "聯絡人詳細資訊")
    public static class ContactDTO {

        @Schema(
            description = "聯絡人姓名",
            example = "王小明",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "聯絡人姓名不可為空")
        private String name;

        @Schema(
            description = "聯絡人電子郵件（需符合 Email 格式）",
            example = "xm@ncu.edu.tw",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Email(message = "電子郵件格式不正確")
        @NotBlank(message = "電子郵件不可為空")
        private String email;

        @Schema(
            description = "聯絡電話（建議使用台灣手機號碼格式，如 09XXXXXXXX）",
            example = "0912345678",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "聯絡電話不可為空")
        private String phone;
    }
}
