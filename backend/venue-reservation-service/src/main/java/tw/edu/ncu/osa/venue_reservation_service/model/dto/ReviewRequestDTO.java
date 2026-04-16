package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 審核申請提交請求 DTO
 * 用於接收管理員提交的審核申請資料
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "審核申請提交請求物件",
    example = """
    {
      "bookingId": 10,
      "status": 3
    }
    """
)
public class ReviewRequestDTO {

    @Schema(
        description = "預約申請案編號",
        example = "10",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "預約案編號不可為空")
    private Long bookingId;

    @Schema(
        description = """
        審核狀態
        - 0: 已撤回
        - 1: 審核中
        - 2: 已通過
        - 3: 已拒絕
        - 4: 已刪除（僅用於軟刪除）
        """,
        example = "3",
        requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = {"0", "1", "2", "3", "4"}
    )
    @NotNull(message = "審核狀態不可為空")
    @Min(value = 0, message = "審核狀態範圍不正確")
    @Max(value = 4, message = "審核狀態範圍不正確")
    private Integer status;
}

