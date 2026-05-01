package tw.edu.ncu.osa.venue_reservation_service.service;

import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentBorrowQueryDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentCreateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentUpdateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBorrowRecordPageVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentListByVenueVO;
import java.util.List;

/**
 * 設備管理業務服務介面
 * 定義設備 CRUD 與借用歷史查詢相關的業務操作契約
 */
public interface EquipmentService {

    // ==========================================
    // 設備查詢
    // ==========================================

    /**
     * 查詢所有設備，按場地分組，包含使用狀態
     * 一次查詢返回所有場地的設備資訊
     * @return 按場地分組的設備清單，含使用狀態
     */
    List<EquipmentListByVenueVO> queryAllEquipmentsWithStatus();

    // ==========================================
    // 設備 CRUD 操作
    // ==========================================

    /**
     * 新增設備
     * 檢查名稱唯一性，若軟刪除則復原，並在 venue_equipment_map 中建立關聯
     * @param request 新增設備請求 DTO
     * @return 新增成功的設備 ID
     * @throws RuntimeException 若設備名稱已存在（未被軟刪除）或場地不存在
     */
    Long createEquipment(EquipmentCreateDTO request);

    /**
     * 修改設備
     * 檢查新名稱唯一性（排除自身），並同步更新 venue_equipment_map
     * @param request 修改設備請求 DTO
     * @throws RuntimeException 若新名稱已存在或設備不存在
     */
    void updateEquipment(EquipmentUpdateDTO request);

    /**
     * 刪除設備（軟刪除）
     * 檢查是否有進行中的預約，若有則拋錯，否則執行軟刪除
     * @param equipmentId 設備 ID
     * @throws RuntimeException 若設備有進行中的預約或設備不存在
     */
    void deleteEquipment(Long equipmentId);

    // ==========================================
    // 設備借用歷史查詢
    // ==========================================

    /**
     * 分頁查詢設備借用歷史紀錄
     * 返回已批准預約（status=2）涉及的設備借用情況，按借用日期倒序排列
     * @param queryDTO 分頁查詢參數（含 pageNum、pageSize）
     * @return 分頁結果，包含借用紀錄清單
     */
    EquipmentBorrowRecordPageVO getEquipmentBorrowHistory(EquipmentBorrowQueryDTO queryDTO);
}

