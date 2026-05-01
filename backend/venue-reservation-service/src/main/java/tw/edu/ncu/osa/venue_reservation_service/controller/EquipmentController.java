package tw.edu.ncu.osa.venue_reservation_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tw.edu.ncu.osa.venue_reservation_service.common.result.Result;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentBorrowQueryDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentCreateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentUpdateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBorrowRecordPageVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentListByVenueVO;
import tw.edu.ncu.osa.venue_reservation_service.service.EquipmentService;

import java.util.List;

/**
 * 設備管理 RESTful API 控制層
 * 提供設備 CRUD 與借用歷史查詢等相關終端點
 */
@Slf4j
@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Tag(
        name = "設備管理",
        description = "提供設備相關的功能，包括查詢、新增、修改、刪除與借用歷史查詢。" +
                "本模組提供管理員進行設備管理的必要功能。"
)
public class EquipmentController {

    private final EquipmentService equipmentService;

    // ==========================================
    // 設備查詢
    // ==========================================

    /**
     * 查詢所有設備及使用狀態
     * 返回兩個場地的設備清單，包括設備名稱、數量與當前使用狀態
     *
     * @return 操作結果與場地分組的設備清單
     */
    @GetMapping
    @Operation(
            summary = "查詢所有設備及使用狀態",
            description = """
                    查詢所有未被軟刪除的設備，包含使用狀態。系統會根據當前時間判定設備是否被借用。
                    
                    **應用場景：**
                    前端在初始化設備管理頁面時調用此 API，顯示兩個場地的設備清單，
                    每個設備顯示名稱、數量與即時使用狀態。
                    
                    **返回數據結構：**
                    按場地分組，每個場地包含該場地的所有設備及其狀態
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "成功獲取設備及使用狀態"
            )
    })
    @SecurityRequirement(name = "Mock-Authorization")
    public Result<List<EquipmentListByVenueVO>> queryAllEquipments() {
        log.info("【EquipmentController】收到請求：查詢所有設備及使用狀態");

        try {
            List<EquipmentListByVenueVO> result = equipmentService.queryAllEquipmentsWithStatus();
            log.info("【EquipmentController】成功查詢設備清單，共 {} 個場地", result.size());
            log.debug("【EquipmentController】返回結果：{}", result);

            return Result.success(result);
        } catch (Exception e) {
            log.error("【EquipmentController】查詢設備清單失敗", e);
            throw e;
        }
    }

    // ==========================================
    // 設備 CRUD 操作
    // ==========================================

    /**
     * 新增設備
     *
     * @param request 新增設備請求 DTO
     * @return 操作結果與新增的設備 ID
     */
    @PostMapping
    @Operation(
            summary = "新增設備",
            description = """
                    新增一筆設備資料。系統會檢查設備名稱唯一性，若名稱已被軟刪除的設備占用，
                    則自動復原該設備而非新增重複。
                    
                    **特殊邏輯：**
                    1. 檢查設備名稱是否已存在（未被軟刪除）
                    2. 若已存在則拋錯
                    3. 若名稱被軟刪除的設備占用則復原
                    4. 新增後自動關聯到指定場地
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "設備新增成功，返回設備 ID"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "設備名稱已存在或其他參數錯誤"
            )
    })
    @SecurityRequirement(name = "Mock-Authorization")
    public Result<Long> createEquipment(
            @RequestBody EquipmentCreateDTO request
    ) {
        log.info("【EquipmentController】收到請求：新增設備，名稱={}, 場地ID={}, 數量={}",
                request.getEquipmentName(), request.getVenueId(), request.getQuantity());

        try {
            Long equipmentId = equipmentService.createEquipment(request);
            log.info("【EquipmentController】成功新增設備，ID={}", equipmentId);

            return Result.success(equipmentId);
        } catch (Exception e) {
            log.error("【EquipmentController】新增設備失敗", e);
            throw e;
        }
    }

    /**
     * 修改設備
     *
     * @param id      設備 ID
     * @param request 修改設備請求 DTO
     * @return 操作結果
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "修改設備",
            description = """
                    修改設備的名稱、所屬場地與數量。除了設備 ID 必填外，其他欄位均為可選。
                    系統會檢查新名稱的唯一性（排除自身），並同步更新場地設備的關聯。
                    
                    **修改場地邏輯：**
                    1. 若同時提供 venueId 與 quantity：更新場地 & 數量
                    2. 若只提供 venueId：只更新場地
                    3. 若同時提供 venueId 與 quantity：兩個都更新
                    4. 若只提供 quantity：無法更新（需提供 venueId）
                    
                    **特殊邏輯：**
                    1. 驗證設備存在
                    2. 若提供新名稱則檢查新名稱唯一性
                    3. 更新設備資訊
                    4. 根據提供的字段更新場地關聯
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "設備修改成功"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "新名稱已存在、設備不存在或其他參數錯誤"
            )
    })
    @SecurityRequirement(name = "Mock-Authorization")
    public Result<String> updateEquipment(
            @PathVariable
            @Parameter(
                    description = "設備唯一識別碼",
                    example = "5"
            )
            Long id,

            @RequestBody EquipmentUpdateDTO request
    ) {
        log.info("【EquipmentController】收到請求：修改設備，ID={}, 新名稱={}, 新場地ID={}, 新數量={}",
                id, request.getEquipmentName(), request.getVenueId(), request.getQuantity());

        try {
            request.setId(id);
            equipmentService.updateEquipment(request);
            log.info("【EquipmentController】成功修改設備，ID={}", id);

            return Result.success("設備修改成功");
        } catch (Exception e) {
            log.error("【EquipmentController】修改設備失敗，ID={}", id, e);
            throw e;
        }
    }

    /**
     * 刪除設備（軟刪除）
     *
     * @param id 設備 ID
     * @return 操作結果
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "刪除設備",
            description = """
                    軟刪除一筆設備資料。系統會檢查該設備是否有進行中的預約（status=1 或 2），
                    若有則拒絕刪除，否則執行軟刪除。
                    
                    **特殊邏輯：**
                    1. 驗證設備存在
                    2. 檢查是否有進行中的預約
                    3. 若有則拋錯，若無則軟刪除
                    4. 清除場地關聯
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "設備刪除成功"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "設備有進行中的預約、設備不存在或其他參數錯誤"
            )
    })
    @SecurityRequirement(name = "Mock-Authorization")
    public Result<String> deleteEquipment(
            @PathVariable
            @Parameter(
                    description = "設備唯一識別碼",
                    example = "5"
            )
            Long id
    ) {
        log.info("【EquipmentController】收到請求：刪除設備，ID={}", id);

        try {
            equipmentService.deleteEquipment(id);
            log.info("【EquipmentController】成功刪除設備，ID={}", id);

            return Result.success("設備刪除成功");
        } catch (Exception e) {
            log.error("【EquipmentController】刪除設備失敗，ID={}", id, e);
            throw e;
        }
    }

    // ==========================================
    // 設備借用歷史查詢
    // ==========================================

    /**
     * 查詢設備借用歷史紀錄（分頁）
     * 返回所有已批准預約（status=2）涉及的設備借用紀錄，包括場地、設備、借用時間與事由
     *
     * @param pageNum  頁碼（從 1 開始），默認值為 1
     * @param pageSize 每頁筆數（默認 10，最大 100）
     * @return 操作結果與分頁借用紀錄
     */
    @GetMapping("/history")
    @Operation(
            summary = "查詢設備借用歷史紀錄",
            description = """
                    分頁查詢已批准的設備借用紀錄。系統會返回所有 status=2（已通過）的預約所涉及的設備借用情況。
                    
                    **應用場景：**
                    管理員透過此 API 查看設備的使用歷史，了解設備在各個場地的借用情況，
                    以便進行設備管理與維護計畫的制定。
                    
                    **排序規則：**
                    - 主排序：借用日期倒序（最新的借用記錄最上方）
                    - 次排序：同日期按預約 ID 倒序
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "成功獲取借用歷史紀錄"
            )
    })
    @SecurityRequirement(name = "Mock-Authorization")
    public Result<EquipmentBorrowRecordPageVO> getEquipmentBorrowHistory(
            @RequestParam(value = "pageNum", defaultValue = "1")
            @Parameter(
                    description = "頁碼編號。從 1 開始，若提供無效值（<=0）將自動調整為 1",
                    example = "1"
            )
            Integer pageNum,

            @RequestParam(value = "pageSize", defaultValue = "10")
            @Parameter(
                    description = "每頁筆數。默認值為 10，最大值為 100。若超過 100 將自動調整為 100",
                    example = "10"
            )
            Integer pageSize
    ) {
        log.info("【EquipmentController】收到請求：查詢設備借用歷史紀錄，pageNum={}, pageSize={}", pageNum, pageSize);

        try {
            EquipmentBorrowQueryDTO queryDTO = new EquipmentBorrowQueryDTO(pageNum, pageSize);
            log.debug("【EquipmentController】組裝查詢 DTO：pageNum={}, pageSize={}", pageNum, pageSize);

            EquipmentBorrowRecordPageVO result = equipmentService.getEquipmentBorrowHistory(queryDTO);
            log.info("【EquipmentController】成功查詢設備借用歷史紀錄，totalCount={}，currentPage={}，返回 {} 筆紀錄",
                    result.getTotalCount(), result.getCurrentPage(), result.getData().size());
            log.debug("【EquipmentController】返回結果：{}", result);

            return Result.success(result);
        } catch (Exception e) {
            log.error("【EquipmentController】查詢設備借用歷史紀錄失敗，pageNum={}, pageSize={}", pageNum, pageSize, e);
            throw e;
        }
    }
}

