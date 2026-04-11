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
import tw.edu.ncu.osa.venue_reservation_service.model.dto.BookingRequestDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.BookingVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarDayVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarMonthVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarWeekVO;
import tw.edu.ncu.osa.venue_reservation_service.service.BookingService;
import java.util.List;

/**
 * 預約管理 API 控制器
 * 負責處理預約相關的 HTTP 請求，包括提交預約、查詢預約、修改預約、撤回預約及查看日曆視圖等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(
    name = "預約管理",
    description = "提供場地預約的核心業務功能，包括預約申請、查詢、修改、撤回及日曆視圖查詢。" +
        "用戶可透過此模組申請使用場地、檢視個人申請狀態、修改已提交的申請、撤回不需要的申請，" +
        "以及查看場地在不同時間維度（月、周、日）的可用時段資訊"
)
public class BookingController {

    private final BookingService bookingService;

    // ==========================================
    // 1. 提交預約申請
    // ==========================================

    /**
     * 提交預約申請
     * 用戶提交新的場地預約申請，初始狀態為「審核中」。系統將驗證所有必填欄位，
     * 檢查時段是否衝突（只檢查已通過的預約），並儲存申請記錄
     * @param request 預約申請資料
     * @return 新建立的預約案 ID
     */
    @PostMapping
    @Operation(
        summary = "提交預約申請",
        description = """
            建立一筆新的場地預約申請。
            
            **業務流程：**
            1. 驗證請求參數符合規範（所有必填欄位已填寫、參數值合理）
            2. 驗證所選時段未被其他已通過的預約佔用
            3. 儲存預約申請，初始狀態為「審核中（1）」
            4. 返回新建預約的 ID
            
            **時段衝突檢查說明：**
            系統只檢查狀態為「已通過（2）」的預約時段。
            若所選時段與「審核中」或「已拒絕」的預約重疊，允許申請通過（由審核者決定衝突處理）
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "預約申請成功提交",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "成功回應，data 欄位包含新建預約的 ID",
                    example = """
                    {
                      "success": true,
                      "message": "操作成功",
                      "data": 501
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "參數驗證失敗或業務邏輯異常",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "失敗回應，包含具體錯誤原因",
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
    public Result<Long> createBooking(@Valid @RequestBody BookingRequestDTO request) {
        log.info("【BookingController】收到請求：提交預約申請");
        log.info("【BookingController】請求參數 - venueId={}, bookingDate={}, slots={}, purpose={}",
                request.getVenueId(), request.getBookingDate(), request.getSlots(), request.getPurpose());
        try {
            Long bookingId = bookingService.createBooking(request);
            log.info("【BookingController】成功建立預約申請，新預約 ID={}", bookingId);
            return Result.success(bookingId);
        } catch (Exception e) {
            log.error("【BookingController】提交預約申請失敗，venueId={}, bookingDate={}", 
                    request.getVenueId(), request.getBookingDate(), e);
            throw e;
        }
    }

    // ==========================================
    // 2. 查詢預約
    // ==========================================

    /**
     * 查看個人預約清單
     * 根據當前登入用戶的身份，取得其所有歷史申請記錄（無論審核狀態）
     * @return 當前登入用戶的預約申請列表
     */
    @GetMapping("/my")
    @Operation(
        summary = "查詢個人預約清單",
        description = """
            取得當前登入用戶的所有預約申請記錄，包含所有狀態的申請（已撤回、審核中、已通過、已拒絕）。
            
            **預約狀態說明：**
            - 0: 已撤回 - 申請人主動撤回的申請
            - 1: 審核中 - 尚未被核准或拒絕的新申請
            - 2: 已通過 - 申請已獲批准，可使用場地
            - 3: 已拒絕 - 申請被駁回，用戶需重新申請
            
            **應用場景：**
            前端可使用此 API 實現「我的申請」頁面，用戶可檢視所有歷史申請及其狀態變化
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功取得用戶預約列表",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "成功回應，data 欄位包含預約列表",
                    example = """
                    {
                      "success": true,
                      "message": "操作成功",
                      "data": [
                        {
                          "id": 501,
                          "venueName": "會議室 A",
                          "bookingDate": "2026-04-10",
                          "slots": [8, 9],
                          "status": 2,
                          "createdAt": "2026-04-03T10:00:00",
                          "purpose": "專案討論",
                          "pCount": 5,
                          "contactInfo": "{\\"name\\":\\"王小明\\",\\"phone\\":\\"0912345678\\",\\"email\\":\\"xm@ncu.edu.tw\\"}",
                          "equipments": ["麥克風", "投影機"]
                        }
                      ]
                    }
                    """
                )
            )
        )
    })
    @SecurityRequirement(name = "Mock-Authorization")
    public Result<List<BookingVO>> getMyBookings() {
        log.info("【BookingController】收到請求：查看個人預約清單");
        try {
            List<BookingVO> bookings = bookingService.getMyBookings();
            log.info("【BookingController】成功查詢個人預約清單，共 {} 筆預約", bookings.size());
            log.debug("【BookingController】返回預約數據：{}", bookings);
            return Result.success(bookings);
        } catch (Exception e) {
            log.error("【BookingController】查詢個人預約清單失敗", e);
            throw e;
        }
    }

    // ==========================================
    // 3. 修改預約
    // ==========================================

    /**
     * 修改預約申請
     * 用戶可修改尚未被核准或已核准的申請內容。修改後狀態將重置為「審核中」，
     * 以便審核者重新評估修改後的申請
     * @param bookingId 預約案 ID
     * @param request 修改後的預約資料
     * @return 操作結果
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "修改預約申請",
        description = """
            修改指定的預約申請內容。
            
            **修改規則：**
            1. 僅允許修改狀態為「審核中（1）」或「已通過（2）」的申請
            2. 修改後狀態自動重置為「審核中（1）」，需要重新審核
            3. 驗證修改後的時段是否與其他已通過預約衝突
            4. 更新預約資訊並記錄修改時間
            
            **限制條件：**
            - 已拒絕（3）或已撤回（0）的申請無法修改，需重新申請
            - 新選時段不可與其他已通過預約重疊
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "預約修改成功",
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
            description = "參數驗證失敗或業務邏輯異常",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "失敗回應",
                    example = """
                    {
                      "success": false,
                      "message": "已拒絕之申請無法修改",
                      "data": null
                    }
                    """
                )
            )
        )
    })
    @SecurityRequirement(name = "Mock-Authorization")
    public Result<Void> updateBooking(
            @PathVariable(name = "id")
            @Parameter(
                description = "要修改的預約申請 ID",
                example = "501",
                required = true
            )
            Long bookingId,
            @Valid @RequestBody BookingRequestDTO request) {
        log.info("【BookingController】收到請求：修改預約申請，bookingId={}", bookingId);
        log.info("【BookingController】修改參數 - venueId={}, bookingDate={}, slots={}", 
                request.getVenueId(), request.getBookingDate(), request.getSlots());
        try {
            bookingService.updateBooking(bookingId, request);
            log.info("【BookingController】成功修改預約申請，bookingId={}", bookingId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("【BookingController】修改預約申請失敗，bookingId={}", bookingId, e);
            throw e;
        }
    }

    // ==========================================
    // 4. 撤回預約
    // ==========================================

    /**
     * 撤回預約申請
     * 申請人主動撤回申請。僅限狀態為「審核中」或「已通過」的案件可撤回，
     * 撤回後狀態變更為「已撤回」，不可再進行任何修改
     * @param bookingId 預約案 ID
     * @return 操作結果
     */
    @PutMapping("/{id}/withdraw")
    @Operation(
        summary = "撤回預約申請",
        description = """
            申請人主動撤回已提交的預約申請。
            
            **撤回規則：**
            1. 僅允許撤回狀態為「審核中（1）」或「已通過（2）」的申請
            2. 撤回後狀態變更為「已撤回（0）」，不可再修改或重新激活
            3. 已拒絕（3）的申請無法撤回（已經被駁回）
            4. 已撤回（0）的申請無法重複撤回
            
            **應用場景：**
            用戶決定不需要該場地預約，或已取消相應活動計劃
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "預約撤回成功",
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
            description = "參數驗證失敗或業務邏輯異常",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "失敗回應",
                    example = """
                    {
                      "success": false,
                      "message": "已拒絕之申請無法撤回",
                      "data": null
                    }
                    """
                )
            )
        )
    })
    @SecurityRequirement(name = "Mock-Authorization")
    public Result<Void> withdrawBooking(
            @PathVariable(name = "id")
            @Parameter(
                description = "要撤回的預約申請 ID",
                example = "501",
                required = true
            )
            Long bookingId) {
        log.info("【BookingController】收到請求：撤回預約申請，bookingId={}", bookingId);
        try {
            bookingService.withdrawBooking(bookingId);
            log.info("【BookingController】成功撤回預約申請，bookingId={}", bookingId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("【BookingController】撤回預約申請失敗，bookingId={}", bookingId, e);
            throw e;
        }
    }

    // ==========================================
    // 5. 日曆視圖查詢
    // ==========================================

    /**
     * 獲取場地月曆視圖
     * 顯示該月每日是否有已占用時段和用戶預約，便於用戶直觀瀏覽月份中哪些日期可用。
     * 月視圖主要用於快速瀏覽，不展示詳細時段資訊，詳細時段需查詢周視圖或日視圖
     * @param venueId 場地 ID
     * @param year 年份
     * @param month 月份 (1-12)
     * @return 月曆視圖資料
     */
    @GetMapping("/calendar/month")
    @Operation(
        summary = "獲取場地月曆視圖",
        description = """
            取得指定場地在指定月份的日曆視圖。
            
            **返回數據包含：**
            1. 該月每日的簡化摘要：
               - date: 日期（ISO 8601 格式）
               - hasApprovedBooking: 該日是否有已通過的預約
               - hasUserBooking: 該日是否有用戶自己的預約
            2. 該月所有預約的詳細列表（前端可根據 slots 與 status 判斷時段占用）
            
            **應用場景：**
            前端用於展示月份日曆，用戶可快速瀏覽整個月份，
            點擊具體日期後進一步查看日視圖以了解詳細時段資訊
            
            **時段占用判斷邏輯：**
            - 若 hasApprovedBooking = true，表示該日有已通過的預約，部分時段被佔用
            - 若 hasUserBooking = true，表示該日有用戶自己的預約（無論審核狀態）
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功取得月曆視圖",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "成功回應，data 欄位包含月曆視圖物件"
                )
            )
        )
    })
    public Result<VenueCalendarMonthVO> getCalendarMonth(
            @RequestParam
            @Parameter(
                description = "場地的唯一識別碼",
                example = "1",
                required = true
            )
            Long venueId,
            @RequestParam
            @Parameter(
                description = "年份（西元年），例如 2026",
                example = "2026",
                required = true
            )
            Integer year,
            @RequestParam
            @Parameter(
                description = "月份（1-12），例如 4 表示四月",
                example = "4",
                required = true
            )
            Integer month) {
        log.info("【BookingController】收到請求：獲取場地月曆視圖，venueId={}, year={}, month={}",
                venueId, year, month);
        try {
            VenueCalendarMonthVO result =
                    bookingService.getVenueCalendarMonth(venueId, year, month);
            log.info("【BookingController】成功獲取月曆視圖，venueId={}, 共 {} 天數據", venueId, result.getDays().size());
            log.debug("【BookingController】返回月曆數據：{}", result);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【BookingController】獲取月曆視圖失敗，venueId={}, year={}, month={}", 
                    venueId, year, month, e);
            throw e;
        }
    }

    /**
     * 獲取場地周曆視圖
     * 顯示該周每日的詳細時段占用情況。相比月視圖，周視圖展示每個小時的占用狀態，
     * 便於用戶精確查看週內哪些時段可用
     * @param venueId 場地 ID
     * @param date 周開始日期 (必須為周一，ISO 8601 格式)
     * @return 周曆視圖資料
     */
    @GetMapping("/calendar/week")
    @Operation(
        summary = "獲取場地周曆視圖",
        description = """
            取得指定場地在指定周份的日曆視圖。
            
            **參數要求：**
            - date 參數必須為周一日期
            - 系統會自動計算周一至周日的七日數據
            
            **返回數據包含：**
            1. 周份基本資訊：weekStart（周一）、weekEnd（周日）
            2. 周內每日詳細資訊：
               - date: 日期
               - dayOfWeek: 星期幾（中文表示）
               - approvedSlots: 已通過預約的時段列表（0-23）
               - userSlots: 用戶自己的預約時段列表（0-23）
            
            **應用場景：**
            用戶在月視圖選定日期後，進一步查看周視圖以了解該周每日的詳細時段占用情況，
            用於判斷各時段是否可用
            
            **時段索引說明：**
            使用 24 小時制索引，0-23 分別表示 00:00-01:00、01:00-02:00...23:00-24:00 的時段
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功取得周曆視圖",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "成功回應，data 欄位包含周曆視圖物件"
                )
            )
        )
    })
    public Result<VenueCalendarWeekVO> getCalendarWeek(
            @RequestParam
            @Parameter(
                description = "場地的唯一識別碼",
                example = "1",
                required = true
            )
            Long venueId,
            @RequestParam
            @Parameter(
                description = "周開始日期（必須為周一，ISO 8601 格式：YYYY-MM-DD），例如 2026-04-06 是周一",
                example = "2026-04-06",
                required = true
            )
            java.time.LocalDate date) {
        log.info("【BookingController】收到請求：獲取場地周曆視圖，venueId={}, weekStart={}", venueId, date);
        try {
            tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarWeekVO result =
                    bookingService.getVenueCalendarWeek(venueId, date);
            log.info("【BookingController】成功獲取周曆視圖，venueId={}, weekStart={}, 共 {} 天數據", 
                    venueId, date, result.getDays().size());
            log.debug("【BookingController】返回周曆數據：{}", result);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【BookingController】獲取周曆視圖失敗，venueId={}, weekStart={}", venueId, date, e);
            throw e;
        }
    }

    /**
     * 獲取場地日曆視圖
     * 顯示該日的詳細時段占用情況和用戶預約列表。日視圖是最詳細的視圖，
     * 包含該日所有預約的詳細資訊（預約狀態、時段、用途等），
     * 用戶可進一步決策是否進行新預約
     * @param venueId 場地 ID
     * @param date 查詢日期 (ISO 8601 格式)
     * @return 日曆視圖資料
     */
    @GetMapping("/calendar/day")
    @Operation(
        summary = "獲取場地日曆視圖",
        description = """
            取得指定場地在指定日期的日曆視圖。
            
            **返回數據包含：**
            1. 場地與日期基本資訊：venueId、venueName、date、dayOfWeek
            2. 時段占用情況：
               - approvedSlots: 已通過預約的時段列表
               - userSlots: 用戶自己的預約時段列表
            3. 用戶預約詳情列表：
               - bookingId: 預約編號
               - slots: 該預約的時段列表
               - status: 預約狀態（0/1/2/3）
               - purpose: 使用用途
               - createdAt: 申請時間
            
            **應用場景：**
            前端在月視圖或周視圖點擊特定日期時，顯示該日最詳細的預約資訊。
            用戶可查看該日所有預約（含狀態）、利用情況，
            以及決定是否在空閒時段提交新的預約申請
            
            **時段索引說明：**
            使用 24 小時制索引，0-23 分別表示 00:00-01:00、01:00-02:00...23:00-24:00 的時段
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功取得日曆視圖",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "成功回應，data 欄位包含日曆視圖物件"
                )
            )
        )
    })
    public Result<VenueCalendarDayVO> getCalendarDay(
            @RequestParam
            @Parameter(
                description = "場地的唯一識別碼",
                example = "1",
                required = true
            )
            Long venueId,
            @RequestParam
            @Parameter(
                description = "查詢日期（ISO 8601 格式：YYYY-MM-DD），例如 2026-04-06",
                example = "2026-04-06",
                required = true
            )
            java.time.LocalDate date) {
        log.info("【BookingController】收到請求：獲取場地日曆視圖，venueId={}, date={}", venueId, date);
        try {
            tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueCalendarDayVO result =
                    bookingService.getVenueCalendarDay(venueId, date);
            log.info("【BookingController】成功獲取日曆視圖，venueId={}, date={}", venueId, date);
            log.debug("【BookingController】返回日曆數據：{}", result);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【BookingController】獲取日曆視圖失敗，venueId={}, date={}", venueId, date, e);
            throw e;
        }
    }
}