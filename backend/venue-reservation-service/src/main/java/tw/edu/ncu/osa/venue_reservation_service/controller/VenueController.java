package tw.edu.ncu.osa.venue_reservation_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tw.edu.ncu.osa.venue_reservation_service.common.result.Result;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.UnitVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.VenueVO;
import tw.edu.ncu.osa.venue_reservation_service.service.VenueService;
import java.util.List;

/**
 * 場地與組織 RESTful API 控制層
 * 提供場地、單位與設備資訊的公開查詢終端點
 */
@Slf4j
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(
    name = "場地與組織",
    description = "提供場地、單位與設備資訊的公開查詢功能。" +
        "用戶可透過此模組查詢所有可用的管理單位、各單位下的場地清單，" +
        "以及各場地的詳細資訊（容納人數、借用規則、可借設備等）。" +
        "本模組 API 為公開接口，無需身份認證"
)
public class VenueController {

    private final VenueService venueService;

    // ==========================================
    // 1. 單位相關 API
    // ==========================================

    /**
     * 取得所有管理單位清單
     * 返回系統中所有可用的管理單位資訊，用於場地查詢的單位篩選
     * @return 操作結果與所有單位資訊
     */
    @GetMapping("/units")
    @Operation(
        summary = "獲取所有管理單位清單",
        description = """
            取得系統中所有可用的管理單位資訊。
            
            **應用場景：**
            前端在初始化時調用此 API，獲取所有可用的管理單位列表，
            提供用戶進行單位篩選，進一步查詢該單位下的場地
            
            **返回數據包含：**
            1. 單位 ID：唯一識別碼
            2. 單位名稱：如「學生事務處」、「圖書館」等
            3. 單位代碼：內部編碼，用於系統識別
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功獲取單位清單",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "成功回應，data 欄位包含所有單位列表",
                    example = """
                    {
                      "success": true,
                      "message": "操作成功",
                      "data": [
                        {
                          "id": 1,
                          "name": "學生事務處",
                          "code": "SAA"
                        },
                        {
                          "id": 2,
                          "name": "圖書館",
                          "code": "LIB"
                        }
                      ]
                    }
                    """
                )
            )
        )
    })
    public Result<List<UnitVO>> getAllUnits() {
        log.info("【VenueController】收到請求：獲取所有管理單位清單");
        try {
            List<UnitVO> units = venueService.getAllUnits();
            log.info("【VenueController】成功獲取管理單位清單，共 {} 個單位", units.size());
            log.debug("【VenueController】返回單位數據：{}", units);
            return Result.success(units);
        } catch (Exception e) {
            log.error("【VenueController】獲取管理單位清單失敗", e);
            throw e;
        }
    }

    // ==========================================
    // 2. 場地相關 API
    // ==========================================

    /**
     * 根據單位 ID 查詢該單位下的場地清單
     * 返回指定單位管理的所有場地，用戶可據此進一步查看場地詳細資訊
     * @param unitId 單位 ID (必填)
     * @return 操作結果與場地清單資訊
     */
    @GetMapping("/venues")
    @Operation(
        summary = "查詢單位下的場地清單",
        description = """
            根據指定的單位 ID，查詢該單位管理的所有場地清單。
            
            **應用場景：**
            用戶在單位篩選後，調用此 API 獲取該單位下的所有可預約場地，
            進一步點擊具體場地查看詳細資訊
            
            **返回數據包含：**
            1. 場地基本資訊：ID、名稱、所屬單位 ID
            2. 容納人數：該場地的最大容納人數
            3. 場地介紹：借用規則、設施說明等
            4. 可借設備清單：該場地提供的所有可借用設備
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功獲取單位下的場地清單",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "成功回應，data 欄位包含場地列表"
                )
            )
        )
    })
    public Result<List<VenueVO>> getVenuesByUnit(
            @RequestParam
            @Parameter(
                description = "管理單位的唯一識別碼。目前系統中有 2 個單位，ID 分別為 1（學生事務處）和 2（圖書館）",
                example = "1",
                required = true
            )
            Long unitId) {
        log.info("【VenueController】收到請求：根據單位 ID 查詢場地清單，unitId={}", unitId);
        try {
            List<VenueVO> venues = venueService.getVenuesByUnitId(unitId);
            log.info("【VenueController】成功查詢到場地清單，unitId={}，共 {} 個場地", unitId, venues.size());
            log.debug("【VenueController】返回場地數據：{}", venues);
            return Result.success(venues);
        } catch (Exception e) {
            log.error("【VenueController】查詢場地清單失敗，unitId={}", unitId, e);
            throw e;
        }
    }

    /**
     * 取得單一場地的詳細資訊與可借用設備清單
     * 返回指定場地的完整資訊，包括容納人數、借用規則說明、可借用的設備清單等
     * @param id 場地 ID
     * @return 操作結果與場地詳細資訊
     */
    @GetMapping("/venues/{id}")
    @Operation(
        summary = "獲取場地詳細資訊",
        description = """
            根據場地 ID 獲取該場地的完整資訊。
            
            **應用場景：**
            用戶在場地列表中選擇具體場地後，調用此 API 獲取場地的詳細資訊，
            包括容納人數上限、借用規則、可借用的設備等，作為進一步預約的參考
            
            **返回數據包含：**
            1. 場地基本資訊：ID、名稱、所屬單位 ID
            2. 容納人數：該場地的最大容納人數上限
            3. 場地介紹：詳細的借用規則、設施說明等文字說明
            4. 可借設備詳細清單：所有可供借用的設備名稱與 ID
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功獲取場地詳細資訊",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "成功回應，data 欄位包含場地詳細資訊物件"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "場地不存在",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "失敗回應，場地 ID 不存在",
                    example = """
                    {
                      "success": false,
                      "message": "找不到指定的場地資訊",
                      "data": null
                    }
                    """
                )
            )
        )
    })
    public Result<VenueVO> getVenueById(
            @PathVariable
            @Parameter(
                description = "場地的唯一識別碼。目前系統中有 2 個場地，ID 分別為 1 和 2",
                example = "1",
                required = true
            )
            Long id) {
        log.info("【VenueController】收到請求：獲取場地詳細資訊，id={}", id);
        try {
            VenueVO venue = venueService.getVenueById(id);
            log.info("【VenueController】成功獲取場地詳細資訊，id={}，場地名稱={}", id, venue.getName());
            log.debug("【VenueController】返回場地數據：{}", venue);
            return Result.success(venue);
        } catch (Exception e) {
            log.error("【VenueController】獲取場地詳細資訊失敗，id={}", id, e);
            throw e;
        }
    }
}

