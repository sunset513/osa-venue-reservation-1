package tw.edu.ncu.osa.venue_reservation_service.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局統一回傳包裝類
 * 所有 API 回應都採用此格式封裝，確保前後端通訊協議一致
 * @param <T> 回傳資料的類型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "通用 API 回應物件，所有端點都使用此格式進行回應",
    example = """
    {
      "success": true,
      "message": "操作成功",
      "data": {}
    }
    """
)
public class Result<T> {

    @Schema(
        description = "操作是否成功。true 表示成功，false 表示失敗",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean success;

    @Schema(
        description = """
        提示訊息。
        操作成功時：通常為 "操作成功"
        操作失敗時：包含具體的錯誤原因說明（用於前端展示給用戶）
        """,
        example = "操作成功",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String message;

    @Schema(
        description = """
        實際承載的回應資料。
        對於查詢類操作（GET）：返回查詢結果物件或列表
        對於新增操作（POST）：返回新建資源的 ID 或新建資源物件
        對於修改/刪除操作（PUT/DELETE）：通常為 null
        """,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private T data;

    /**
     * 成功回傳 - 帶數據
     * 用於返回查詢結果或新建資源
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, "操作成功", data);
    }

    /**
     * 成功回傳 - 無數據
     * 用於 Delete、Update 等不需要返回具體資料的操作
     */
    public static <T> Result<T> success() {
        return new Result<>(true, "操作成功", null);
    }

    /**
     * 失敗回傳 - 自定義訊息
     * 用於業務邏輯異常或驗證失敗時返回錯誤訊息
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(false, message, null);
    }
}