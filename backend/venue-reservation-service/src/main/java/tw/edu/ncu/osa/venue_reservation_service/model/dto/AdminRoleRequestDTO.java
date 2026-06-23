package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "系統管理員角色請求物件")
public class AdminRoleRequestDTO {

    @Schema(description = "Portal identifier", example = "114423011")
    @NotBlank(message = "管理員 userId 不可為空")
    private String userId;

    @Schema(description = "管理員等級：0=可進入 review，1=可管理 admin role", example = "1")
    @NotNull(message = "管理員等級不可為空")
    @Min(value = 0, message = "管理員等級只能是 0 或 1")
    @Max(value = 1, message = "管理員等級只能是 0 或 1")
    private Integer level;
}
