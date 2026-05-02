package tw.edu.ncu.osa.venue_reservation_service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Booking;
import java.time.LocalDate;
import java.util.List;

/**
 * 預約資料持久層 Mapper
 * 負責預約案與相關設備的資料庫操作
 */
@Mapper
public interface BookingMapper {
    
    // ==========================================
    // 衝突檢查
    // ==========================================
    
    /**
     * 檢查是否有衝突的已通過預約
     * @param venueId 場地 ID
     * @param date 預約日期
     * @param mask 24-bit 時段遮罩
     * @return 衝突的已通過預約數量
     */
    int countConflictingApprovedBookings(
            @Param("venueId") Long venueId,
            @Param("date") LocalDate date,
            @Param("mask") int mask
    );

    // ==========================================
    // 基本 CRUD 操作
    // ==========================================
    
    /**
     * 插入預約主表
     * @param booking 預約實體
     * @return 受影響行數
     */
    int insertBooking(Booking booking);
    
    /**
     * 根據預約 ID 查詢預約資訊
     * @param id 預約案 ID
     * @return 預約實體物件
     */
    Booking selectById(@Param("id") Long id);
    
    /**
     * 根據用戶 ID 查詢該用戶所有預約申請
     * @param userId 用戶 ID
     * @return 預約實體列表，按建立時間倒序排列
     */
    List<Booking> selectByUserId(@Param("userId") String userId);

    // ==========================================
    // 更新操作
    // ==========================================
    
    /**
     * 帶樂觀鎖的狀態更新
     * 透過版本號驗證確保更新時資料未被他人修改
     * @param id 預約案 ID
     * @param newStatus 新狀態 (0:撤回, 1:審核中, 2:通過, 3:拒絕)
     * @param oldVersion 舊版本號
     * @return 受影響行數 (0 表示版本號不符，更新失敗)
     */
    int updateStatusWithVersion(
            @Param("id") Long id,
            @Param("newStatus") Integer newStatus,
            @Param("oldVersion") Integer oldVersion
    );
    
    /**
     * 更新預約的完整資訊
     * @param booking 包含更新資訊的預約實體
     * @return 受影響行數
     */
    int updateBooking(Booking booking);

    /**
     * 刪除預約的所有設備關聯
     * @param bookingId 預約案 ID
     * @return 受影響行數
     */
    int deleteBookingEquipmentByBookingId(@Param("bookingId") Long bookingId);

    // ==========================================
    // 關聯操作
    // ==========================================
    
    /**
     * 插入預約設備關聯表
     * @param bookingId 預約案 ID
     * @param equipmentId 設備 ID
     * @return 受影響行數
     */
    int insertBookingEquipment(@Param("bookingId") Long bookingId, @Param("equipmentId") Long equipmentId);

    /**
     * 查詢與指定預約衝突的「審核中」預約
     * @param venueId 場地 ID
     * @param date 預約日期
     * @param mask 24-bit 時段遮罩
     * @param excludeBookingId 要排除的預約 ID（通常是當前通過的預約）
     * @return 衝突的「審核中」預約列表
     */
    List<Booking> selectPendingConflictingBookings(
            @Param("venueId") Long venueId,
            @Param("date") LocalDate date,
            @Param("mask") int mask,
            @Param("excludeBookingId") Long excludeBookingId
    );

    // ==========================================
    // 日曆視圖查詢
    // ==========================================

    /**
     * 按日期範圍查詢已通過審核的預約
     * 用於日曆視圖顯示場地的已占用時段
     * @param venueId 場地 ID
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return 預約實體列表，按日期升序排列
     */
    List<Booking> selectApprovedBookingsByDateRange(
            @Param("venueId") Long venueId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 按日期範圍查詢用戶在特定場地的所有預約
     * 用於日曆視圖顯示用戶的預約紀錄
     * @param userId 用戶 ID
     * @param venueId 場地 ID
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return 預約實體列表，按日期升序排列
     */
    List<Booking> selectUserBookingsByDateRange(
            @Param("userId") String userId,
            @Param("venueId") Long venueId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 按日期範圍查詢場地的全部預約（用於月曆視圖）
     * 返回指定場地該月份的所有預約，包括全部狀態
     * @param venueId 場地 ID
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return BookingVO 列表，按日期升序排列
     */
    List<tw.edu.ncu.osa.venue_reservation_service.model.vo.BookingVO> selectBookingsByDateRangeForCalendar(
            @Param("venueId") Long venueId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ==========================================
    // 高級查詢：多維度篩選和分頁
    // ==========================================

    /**
     * 查詢當前用戶的預約列表，支援多維度篩選和分頁
     * 支援按場地 ID、預約狀態、日期範圍進行篩選
     * 預設按 created_at 倒序（最新優先）
     * @param userId 用戶 ID
     * @param venueId 場地 ID（可選，為 null 表示查詢所有場地）
     * @param statusList 預約狀態列表（可選，為 null 表示查詢所有狀態）
     * @param startDate 預約日期範圍開始（可選，為 null 表示無下限）
     * @param endDate 預約日期範圍結束（可選，為 null 表示無上限）
     * @param limit 分頁：每頁記錄數
     * @param offset 分頁：位移量（OFFSET）
     * @return BookingVO 列表，包含場地名稱和設備名稱，按 created_at 倒序排列
     */
    List<tw.edu.ncu.osa.venue_reservation_service.model.vo.BookingVO> queryMyBookingsWithFilters(
            @Param("userId") String userId,
            @Param("venueId") Long venueId,
            @Param("statusList") List<Integer> statusList,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    /**
     * 計算符合篩選條件的預約總數
     * @param userId 用戶 ID
     * @param venueId 場地 ID（可選）
     * @param statusList 預約狀態列表（可選）
     * @param startDate 預約日期範圍開始（可選）
     * @param endDate 預約日期範圍結束（可選）
     * @return 符合條件的預約總數
     */
    Long countMyBookingsWithFilters(
            @Param("userId") String userId,
            @Param("venueId") Long venueId,
            @Param("statusList") List<Integer> statusList,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}