package tw.edu.ncu.osa.venue_reservation_service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Equipment;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBorrowRecordVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentWithStatusVO;
import java.time.LocalDate;
import java.util.List;

/**
 * 設備資訊資料持久層 Mapper
 * 負責與 equipments 及相關表的交互
 */
@Mapper
public interface EquipmentMapper {
    
    // ==========================================
    // 查詢操作
    // ==========================================
    
    /**
     * 根據場地 ID 查詢該場地可供借用的所有設備
     * 透過 venue_equipment_map 表進行關聯查詢
     * @param venueId 場地 ID
     * @return 該場地可借用的設備清單
     */
    List<Equipment> selectEquipmentsByVenueId(@Param("venueId") Long venueId);

    /**
     * 查詢所有未被軟刪除的設備，包含場地與數量資訊
     * @return 設備清單（含 venueId、venueName、quantity，但不含 isInUse）
     */
    List<EquipmentWithStatusVO> selectAllEquipmentsWithoutStatus();

    /**
     * 檢查設備在特定時間點是否被使用中
     * 判斷依據：是否存在 status=1 或 2 的預約，且 booking_date=today，且 time_slots 位元包含 currentHour
     * @param equipmentId 設備 ID
     * @param today 查詢日期
     * @param currentHour 當前小時（0-23）
     * @return 使用中的預約數量
     */
    int countEquipmentInUseAtTime(
            @Param("equipmentId") Long equipmentId,
            @Param("today") LocalDate today,
            @Param("currentHour") Integer currentHour
    );

    // ==========================================
    // 設備 CRUD 操作
    // ==========================================

    /**
     * 根據設備 ID 查詢設備
     * @param id 設備 ID
     * @return 設備實體
     */
    Equipment selectById(@Param("id") Long id);

    /**
     * 根據設備名稱查詢設備
     * @param name 設備名稱
     * @return 設備實體
     */
    Equipment selectByName(@Param("name") String name);

    /**
     * 新增設備
     * @param equipment 設備實體
     * @return 影響的行數
     */
    int insert(Equipment equipment);

    /**
     * 更新設備
     * @param equipment 設備實體
     * @return 影響的行數
     */
    int update(Equipment equipment);

    /**
     * 軟刪除設備（將 deleted_at 設為當前時間）
     * @param id 設備 ID
     * @return 影響的行數
     */
    int softDelete(@Param("id") Long id);

    /**
     * 檢查設備是否有進行中的預約（status = 1 或 2）
     * @param equipmentId 設備 ID
     * @return 進行中的預約數量
     */
    int countActiveBookingsByEquipmentId(@Param("equipmentId") Long equipmentId);

    // ==========================================
    // 場地設備關聯 (venue_equipment_map) 操作
    // ==========================================

    /**
     * 新增場地與設備的關聯記錄
     * @param venueId 場地 ID
     * @param equipmentId 設備 ID
     * @param quantity 設備數量
     * @return 影響的行數
     */
    int insertVenueEquipmentMap(
            @Param("venueId") Long venueId,
            @Param("equipmentId") Long equipmentId,
            @Param("quantity") Integer quantity
    );

    /**
     * 更新場地設備的數量
     * @param venueId 場地 ID
     * @param equipmentId 設備 ID
     * @param quantity 新的數量
     * @return 影響的行數
     */
    int updateVenueEquipmentMapQuantity(
            @Param("venueId") Long venueId,
            @Param("equipmentId") Long equipmentId,
            @Param("quantity") Integer quantity
    );

    /**
     * 刪除場地設備的關聯記錄
     * @param venueId 場地 ID
     * @param equipmentId 設備 ID
     * @return 影響的行數
     */
    int deleteVenueEquipmentMap(
            @Param("venueId") Long venueId,
            @Param("equipmentId") Long equipmentId
    );

    /**
     * 根據設備 ID 刪除所有場地關聯（用於設備刪除時的清理）
     * @param equipmentId 設備 ID
     * @return 影響的行數
     */
    int deleteVenueEquipmentMapByEquipmentId(@Param("equipmentId") Long equipmentId);

    // ==========================================
    // 設備借用歷史查詢
    // ==========================================

    /**
     * 查詢設備借用歷史紀錄的總筆數（已批准的預約）
     * @return 借用紀錄總筆數
     */
    int selectEquipmentBorrowRecordsCount();

    /**
     * 分頁查詢設備借用歷史紀錄（已批准的預約）
     * 按借用日期倒序排列，同日期按預約 ID 倒序排列
     * @param offset 分頁偏移量 (pageNum - 1) * pageSize
     * @param pageSize 每頁筆數
     * @return 借用紀錄列表（已包含時段的位元遮罩）
     */
    List<EquipmentBorrowRecordVO> selectEquipmentBorrowRecords(
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );
}



