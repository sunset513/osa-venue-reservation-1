package tw.edu.ncu.osa.venue_reservation_service.service;

import tw.edu.ncu.osa.venue_reservation_service.model.dto.ReviewRequestDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.BookingVO;
import java.time.LocalDate;
import java.util.List;

/**
 * 審核業務服務介面
 * 定義審核相關的業務操作契約
 */
public interface ReviewService {

    // ==========================================
    // 查詢待審核預約
    // ==========================================

    /**
     * 獲取待審核的預約列表
     * 根據場地 ID 和日期範圍查詢所有狀態為「審核中」的預約案
     * @param venueId 場地 ID （預設值為 1）
     * @param startDate 開始日期 (預設為當月初)
     * @param endDate 結束日期 (預設為當月末)
     * @return 待審核的預約列表
     */
    List<BookingVO> getPendingBookings(Long venueId, LocalDate startDate, LocalDate endDate);

    // ==========================================
    // 查詢預約詳細資訊
    // ==========================================

    /**
     * 獲取特定預約案的詳細資訊
     * 用於管理員審核前查看預約的所有詳細資訊（包含設備清單）
     * @param bookingId 預約申請案編號
     * @return 預約詳細資訊
     * @throws RuntimeException 當預約不存在時拋出
     */
    BookingVO getBookingDetails(Long bookingId);

    // ==========================================
    // 審核預約（通過）
    // ==========================================

    /**
     * 審核預約申請並通過
     * 核准預約申請，並自動拒絕其他衝突的同場地、同日期、同時段申請
     * @param bookingId 預約申請案編號
     * @throws RuntimeException 當預約不存在、已被審核或時段衝突時拋出
     */
    void reviewBooking(Long bookingId);

    // ==========================================
    // 更新審核狀態
    // ==========================================

    /**
     * 修改審核狀態
     * 支援多種狀態變更，包括拒絕、撤銷拒絕、重新通過等
     * @param request 審核狀態更新請求
     * @throws RuntimeException 當預約不存在或狀態變更非法時拋出
     */
    void updateReviewStatus(ReviewRequestDTO request);

    // ==========================================
    // 刪除預約案
    // ==========================================

    /**
     * 刪除特定預約案
     * 使用軟刪除方式，將狀態改為「已刪除(4)」，並刪除相關的設備紀錄
     * @param bookingId 預約申請案編號
     * @throws RuntimeException 當預約不存在時拋出
     */
    void deleteBooking(Long bookingId);
}

