package tw.edu.ncu.osa.venue_reservation_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import tw.edu.ncu.osa.venue_reservation_service.common.result.Result;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.ReviewRequestDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.BookingVO;
import tw.edu.ncu.osa.venue_reservation_service.service.ReviewService;
import java.time.LocalDate;
import java.util.List;

/**
 * 審核管理 API 控制器
 * 負責處理預約審核相關的 HTTP 請求，包括待審核列表查詢、審核通過、狀態變更和刪除
 */
@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(
    name = "審核管理",
    description = "提供預約審核的核心業務功能，包括待審核列表查詢、預約詳情查詢、審核通過、狀態變更及刪除。" +
        "供管理員使用，可查看所有待審核預約、查詢詳細資訊、進行審核決策（通過/拒絕）、修改審核狀態及刪除預約案"
)
public class ReviewController {

    private final ReviewService reviewService;

    // ==========================================
    // 1. 獲取待審核預約列表
    // ==========================================

    /**
     * 獲取預約列表
     * 根據查詢參數獲取預約案列表，支援按場地、日期範圍及狀態進行過濾
     * @param venueId 場地 ID （預設值為 1）
     * @param startDate 開始日期 (預設為當月初)
     * @param endDate 結束日期 (預設為當月末)
     * @param status 預約狀態 (0:未提交, 1:審核中, 2:已通過, 3:已拒絕，預設查詢全部，除了已刪除的)
     * @return 預約列表
     */
    @GetMapping("/pending")
    @Operation(
        summary = "獲取預約列表",
        description = """
            根據查詢參數獲取預約案列表，支援按場地、日期範圍及狀態進行過濾。
            
            **查詢參數說明：**
            - venueId: 場地 ID（預設值為 1，若要查詢其他場地請指定）
            - startDate: 開始日期，ISO 8601 格式（YYYY-MM-DD），預設為當月初
            - endDate: 結束日期，ISO 8601 格式（YYYY-MM-DD），預設為當月末
            - status: 預約狀態（非必填）
              - 0: 未提交
              - 1: 審核中
              - 2: 已通過
              - 3: 已拒絕
              - 不填：查詢全部狀態（除了已刪除的 status=4）
            
            **應用場景：**
            管理員登入系統後，可使用此 API 獲取預約列表，
            支援按特定狀態過濾或查看全部狀態的預約，進一步查看詳細資訊後決定是否核准或拒絕
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功取得預約列表",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "成功回應，data 欄位包含預約列表"
                )
            )
        )
    })
    @SecurityRequirement(name = "Mock-Authorization")
    public Result<List<BookingVO>> getPendingBookings(
            @RequestParam(required = false)
            @Parameter(
                description = "場地 ID（預設值為 1）",
                example = "1"
            )
            Long venueId,
            @RequestParam(required = false)
            @Parameter(
                description = "開始日期，ISO 8601 格式（YYYY-MM-DD）。預設為當月初",
                example = "2026-04-01"
            )
            LocalDate startDate,
            @RequestParam(required = false)
            @Parameter(
                description = "結束日期，ISO 8601 格式（YYYY-MM-DD）。預設為當月末",
                example = "2026-04-30"
            )
            LocalDate endDate,
            @RequestParam(required = false)
            @Parameter(
                description = "預約狀態（0:未提交, 1:審核中, 2:已通過, 3:已拒絕，不填則查詢全部除了已刪除的）",
                example = "1"
            )
            Integer status) {
        log.info("【ReviewController】收到請求：獲取預約列表，venueId={}, startDate={}, endDate={}, status={}",
                venueId, startDate, endDate, status);
        try {
            List<BookingVO> bookings = reviewService.getPendingBookings(venueId, startDate, endDate, status);
            log.info("【ReviewController】成功獲取預約列表，共 {} 筆預約", bookings.size());
            return Result.success(bookings);
        } catch (Exception e) {
            log.error("【ReviewController】獲取預約列表失敗", e);
            throw e;
        }
    }

    // ==========================================
    // 2. 獲取預約詳細資訊
    // ==========================================

    /**
     * 獲取特定預約案詳細資訊
     * 查詢特定預約案的詳細資訊，供管理員審核使用
     * @param bookingId 預約申請案編號
     * @return 預約詳細資訊
     */
    @GetMapping("/bookings/{id}")
    @Operation(
        summary = "獲取預約詳細資訊",
        description = """
            查詢特定預約案的詳細資訊，供管理員審核使用。
            
            **返回數據包含：**
            - 預約基本資訊：申請案編號、場地名稱、預約日期、時段
            - 審核資訊：狀態、申請時間
            - 使用詳情：目的、預估人數、聯絡人資訊
            - 設備清單：所借用的設備名稱
            
            **應用場景：**
            管理員在待審核列表中點擊特定預約，獲取完整的預約資訊，
            用於判斷是否應該核准或拒絕該申請
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功取得預約詳細資訊",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "成功回應，data 欄位包含預約詳細資訊"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "預約案不存在",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "失敗回應",
                    example = """
                    {
                      "success": false,
                      "message": "查詢的預約案不存在",
                      "data": null
                    }
                    """
                )
            )
        )
    })
    @SecurityRequirement(name = "Mock-Authorization")
    public Result<BookingVO> getBookingDetails(
            @PathVariable(name = "id")
            @Parameter(
                description = "預約申請案編號",
                example = "1",
                required = true
            )
            Long bookingId) {
        log.info("【ReviewController】收到請求：獲取預約詳細資訊，bookingId={}", bookingId);
        try {
            BookingVO bookingDetail = reviewService.getBookingDetails(bookingId);
            log.info("【ReviewController】成功獲取預約詳細資訊，bookingId={}", bookingId);
            return Result.success(bookingDetail);
        } catch (Exception e) {
            log.error("【ReviewController】獲取預約詳細資訊失敗，bookingId={}", bookingId, e);
            throw e;
        }
    }

    // ==========================================
    // 3. 審核預約（通過）
    // ==========================================

    /**
     * 審核預約申請（通過）
     * 核准預約申請。審核通過前會再次檢查衝突，若通過則自動拒絕其他衝突的同場地、同日期、同時段申請
     * @param bookingId 預約申請案編號
     * @return 操作結果
     */
    @PostMapping("/bookings/{id}/approve")
    @Operation(
        summary = "審核預約申請（通過）",
        description = """
            核准預約申請。
            
            **業務流程：**
            1. 驗證預約案存在且狀態為「審核中」
            2. 再次檢查時段衝突（防呆機制）
            3. 若無衝突，將狀態變更為「已通過」
            4. 自動拒絕其他衝突的同場地、同日期、同時段的「審核中」申請
            
            **注意事項：**
            - 僅當預約狀態為「審核中(1)」時才能進行核准
            - 若發現衝突，不允許核准，會返回「該時段已被其他已通過之申請佔用」的錯誤訊息
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "預約審核通過成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "成功回應，無 data 內容",
                    example = """
                    {
                      "success": true,
                      "message": "操作成功",
                      "data": null
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "業務邏輯異常",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "失敗回應",
                    example = """
                    {
                      "success": false,
                      "message": "該時段已被其他已通過之申請佔用",
                      "data": null
                    }
                    """
                )
            )
        )
    })
    @SecurityRequirement(name = "Mock-Authorization")
    public Result<Void> reviewBooking(
            @PathVariable(name = "id")
            @Parameter(
                description = "預約申請案編號",
                example = "1",
                required = true
            )
            Long bookingId) {
        log.info("【ReviewController】收到請求：審核預約申請，bookingId={}", bookingId);
        try {
            reviewService.reviewBooking(bookingId);
            log.info("【ReviewController】成功審核預約申請，bookingId={}", bookingId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("【ReviewController】審核預約申請失敗，bookingId={}", bookingId, e);
            throw e;
        }
    }

    // ==========================================
    // 4. 更新審核狀態
    // ==========================================

    /**
     * 更新預約審核狀態
     * 修改審核狀態。支援多種狀態變更，狀態從「拒絕(3)」改為「通過(2)」時會進行衝突檢查
     * @param bookingId 預約申請案編號
     * @param request 審核狀態更新請求
     * @return 操作結果
     */
    @PutMapping("/bookings/{id}/status")
    @Operation(
        summary = "更新預約審核狀態",
        description = """
            修改預約的審核狀態。支援多種狀態變更，提供更靈活的審核控制。
            
            **支援的狀態變更：**
            - 1(審核中) → 3(拒絕)：允許拒絕審核中的申請
            - 2(通過) → 3(拒絕)：允許撤銷已通過的申請
            - 3(拒絕) → 1(審核中)：允許將拒絕的申請改為審核中
            - 3(拒絕) → 2(通過)：允許，但需進行衝突檢查
            - 其他狀態變更：1→0、2→0、0→1、0→2 等都允許
            
            **衝突檢查：**
            當從「拒絕(3)」改為「通過(2)」時，系統會再次檢查時段是否已被佔用。
            若發現衝突，操作將被拒絕；若無衝突，會自動拒絕其他衝突的「審核中」申請。
            
            **應用場景：**
            用於靈活的審核流程管理，例如：
            - 將原本拒絕的申請改為審核中，重新審核
            - 將原本拒絕的申請改為通過（當時段不衝突時）
            - 撤銷已通過的申請
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "審核狀態更新成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "成功回應，無 data 內容"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "業務邏輯異常",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "失敗回應"
                )
            )
        )
    })
    @SecurityRequirement(name = "Mock-Authorization")
    public Result<Void> updateReviewStatus(
            @PathVariable(name = "id")
            @Parameter(
                description = "預約申請案編號",
                example = "1",
                required = true
            )
            Long bookingId,
            @Valid @RequestBody ReviewRequestDTO request) {
        log.info("【ReviewController】收到請求：更新審核狀態，bookingId={}, newStatus={}",
                bookingId, request.getStatus());
        try {
            // 確保 bookingId 與請求中的 bookingId 一致
            request.setBookingId(bookingId);
            reviewService.updateReviewStatus(request);
            log.info("【ReviewController】成功更新審核狀態，bookingId={}", bookingId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("【ReviewController】更新審核狀態失敗，bookingId={}", bookingId, e);
            throw e;
        }
    }

    // ==========================================
    // 5. 刪除預約案
    // ==========================================

    /**
     * 刪除預約案
     * 刪除特定預約案。使用軟刪除方式，將狀態改為「已刪除(4)」，同時刪除相關的設備紀錄
     * @param bookingId 預約申請案編號
     * @return 操作結果
     */
    @DeleteMapping("/bookings/{id}")
    @Operation(
        summary = "刪除預約案",
        description = """
            刪除特定預約案。使用軟刪除方式進行操作。
            
            **刪除流程：**
            1. 驗證預約案存在
            2. 刪除該預約案相關的設備紀錄
            3. 將預約案狀態改為「已刪除(4)」（軟刪除）
            
            **軟刪除說明：**
            不直接從資料庫刪除預約案，而是將狀態標記為「已刪除」，
            可保留歷史紀錄用於審計追蹤。
            
            **應用場景：**
            管理員認為某筆預約案不應保留時，使用此 API 進行刪除。
            刪除後無法恢復，刪除記錄將保留在資料庫中供審計使用。
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "預約案刪除成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "成功回應，無 data 內容",
                    example = """
                    {
                      "success": true,
                      "message": "操作成功",
                      "data": null
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "預約案不存在",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "失敗回應"
                )
            )
        )
    })
    @SecurityRequirement(name = "Mock-Authorization")
    public Result<Void> deleteBooking(
            @PathVariable(name = "id")
            @Parameter(
                description = "預約申請案編號",
                example = "1",
                required = true
            )
            Long bookingId) {
        log.info("【ReviewController】收到請求：刪除預約案，bookingId={}", bookingId);
        try {
            reviewService.deleteBooking(bookingId);
            log.info("【ReviewController】成功刪除預約案，bookingId={}", bookingId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("【ReviewController】刪除預約案失敗，bookingId={}", bookingId, e);
            throw e;
        }
    }
}



