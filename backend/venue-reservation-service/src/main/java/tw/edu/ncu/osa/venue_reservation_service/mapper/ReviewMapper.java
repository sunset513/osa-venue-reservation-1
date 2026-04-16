package tw.edu.ncu.osa.venue_reservation_service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Booking;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.BookingVO;
import java.time.LocalDate;
import java.util.List;

/**
 * 審核模組 Mapper 接口
 * 負責審核相關的資料庫操作
 */
@Mapper
public interface ReviewMapper {

    // ==========================================
    // 查詢待審核預約列表
    // ==========================================

    /**
     * 根據場地與日期範圍查詢待審核的預約列表
     * @param venueId 場地 ID
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return 待審核的預約列表 (狀態為 1 的預約)
     */
    List<BookingVO> selectPendingBookingsByVenueAndDateRange(
            @Param("venueId") Long venueId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ==========================================
    // 查詢預約詳細資訊
    // ==========================================

    /**
     * 根據預約 ID 查詢預約詳細資訊（包含設備清單）
     * @param bookingId 預約申請案編號
     * @return 預約詳細資訊
     */
    BookingVO selectBookingWithEquipments(@Param("bookingId") Long bookingId);

    // ==========================================
    // 衝突檢查
    // ==========================================

    /**
     * 查詢與指定預約衝突的其他已通過預約
     * @param bookingId 要檢查的預約 ID
     * @param venueId 場地 ID
     * @param bookingDate 預約日期
     * @param mask 時段遮罩
     * @return 衝突的已通過預約清單
     */
    List<Booking> selectConflictingApprovedBookings(
            @Param("bookingId") Long bookingId,
            @Param("venueId") Long venueId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("mask") Integer mask
    );

    // ==========================================
    // 批量更新狀態
    // ==========================================

    /**
     * 批量更新預約狀態
     * @param bookingIds 要更新的預約 ID 清單
     * @param newStatus 新狀態
     * @return 更新的預約筆數
     */
    int batchUpdateStatus(
            @Param("bookingIds") List<Long> bookingIds,
            @Param("newStatus") Integer newStatus
    );

    // ==========================================
    // 軟刪除操作
    // ==========================================

    /**
     * 軟刪除預約（將狀態改為 4：已刪除）
     * @param bookingId 預約 ID
     * @return 更新的預約筆數
     */
    int deleteSoftBooking(@Param("bookingId") Long bookingId);

    /**
     * 刪除預約的關聯設備紀錄
     * @param bookingId 預約 ID
     * @return 刪除的設備紀錄筆數
     */
    int deleteBookingEquipmentsByBookingId(@Param("bookingId") Long bookingId);
}

