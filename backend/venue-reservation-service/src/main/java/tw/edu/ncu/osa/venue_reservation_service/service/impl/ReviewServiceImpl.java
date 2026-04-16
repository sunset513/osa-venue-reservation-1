package tw.edu.ncu.osa.venue_reservation_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.edu.ncu.osa.venue_reservation_service.mapper.BookingMapper;
import tw.edu.ncu.osa.venue_reservation_service.mapper.ReviewMapper;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.ReviewRequestDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Booking;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.BookingVO;
import tw.edu.ncu.osa.venue_reservation_service.service.ReviewService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 審核服務實現類
 * 處理預約審核相關的業務邏輯，包括待審核列表查詢、審核通過、狀態變更和軟刪除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final BookingMapper bookingMapper;

    // ==========================================
    // 1. 查詢預約列表
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<BookingVO> getPendingBookings(Long venueId, LocalDate startDate, LocalDate endDate, Integer status) {
        log.info("【ReviewService】[getPendingBookings] 開始查詢預約列表");

        // 如果未指定場地 ID，使用預設值（例如：1L）
        if (venueId == null) {
            venueId = 1L;
            log.info("【ReviewService】[getPendingBookings] 未指定場地 ID，使用預設值 venueId=1");
        }

        // 設定預設日期範圍（當月）
        if (startDate == null || endDate == null) {
            YearMonth currentMonth = YearMonth.now();
            startDate = currentMonth.atDay(1);
            endDate = currentMonth.atEndOfMonth();
            log.info("【ReviewService】[getPendingBookings] 未指定日期範圍，使用當月：{} 至 {}", startDate, endDate);
        }

        // 驗證日期範圍有效性
        if (startDate.isAfter(endDate)) {
            log.warn("【ReviewService】[getPendingBookings] 開始日期晚於結束日期，startDate={}, endDate={}", startDate, endDate);
            throw new RuntimeException("開始日期不能晚於結束日期");
        }

        // 驗證狀態值有效性
        if (status != null && (status < 0 || status > 3)) {
            log.warn("【ReviewService】[getPendingBookings] 無效的狀態值，status={}", status);
            throw new RuntimeException("狀態值無效，請輸入 0-3 或不填");
        }

        log.info("【ReviewService】[getPendingBookings] 查詢參數 - venueId={}, startDate={}, endDate={}, status={}",
                venueId, startDate, endDate, status);

        // 查詢預約列表
        List<BookingVO> bookings = reviewMapper.selectBookingsByVenueAndDateRange(
                venueId, startDate, endDate, status
        );
        log.info("【ReviewService】[getPendingBookings] 查詢到 {} 筆預約", bookings.size());
        log.debug("【ReviewService】[getPendingBookings] 預約清單：{}", bookings);

        return bookings;
    }

    // ==========================================
    // 2. 查詢預約詳細資訊
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public BookingVO getBookingDetails(Long bookingId) {
        log.info("【ReviewService】[getBookingDetails] 開始查詢預約詳細資訊，bookingId={}", bookingId);

        // 驗證預約 ID 有效性
        if (bookingId == null || bookingId <= 0) {
            log.warn("【ReviewService】[getBookingDetails] 無效的預約 ID，bookingId={}", bookingId);
            throw new RuntimeException("預約案編號無效");
        }

        // 查詢預約詳細資訊（含設備清單）
        BookingVO bookingDetail = reviewMapper.selectBookingWithEquipments(bookingId);

        if (bookingDetail == null) {
            log.warn("【ReviewService】[getBookingDetails] 查詢的預約案不存在，bookingId={}", bookingId);
            throw new RuntimeException("查詢的預約案不存在");
        }

        log.info("【ReviewService】[getBookingDetails] 成功查詢預約詳細資訊，bookingId={}", bookingId);
        log.debug("【ReviewService】[getBookingDetails] 預約詳細資訊：{}", bookingDetail);

        return bookingDetail;
    }

    // ==========================================
    // 3. 審核預約（通過）
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewBooking(Long bookingId) {
        log.info("【ReviewService】[reviewBooking] 開始審核預約申請，bookingId={}", bookingId);

        // 驗證預約 ID 有效性
        if (bookingId == null || bookingId <= 0) {
            log.warn("【ReviewService】[reviewBooking] 無效的預約 ID，bookingId={}", bookingId);
            throw new RuntimeException("預約案編號無效");
        }

        // 1. 查詢預約原始資料
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            log.warn("【ReviewService】[reviewBooking] 預約案不存在，bookingId={}", bookingId);
            throw new RuntimeException("查詢的預約案不存在");
        }
        log.info("【ReviewService】[reviewBooking] 成功查詢預約原始資料，bookingId={}, 當前狀態={}", bookingId, booking.getStatus());

        // 2. 檢查預約狀態是否為「審核中(1)」
        if (booking.getStatus() != 1) {
            log.warn("【ReviewService】[reviewBooking] 該預約案已被審核，無法重複審核，bookingId={}, 當前狀態={}", bookingId, booking.getStatus());
            throw new RuntimeException("該預約案已被審核，無法重複審核");
        }

        // 3. 再次進行衝突檢查（防呆機制）
        log.info("【ReviewService】[reviewBooking] 進行時段衝突檢查，venueId={}, bookingDate={}, mask={}",
                booking.getVenueId(), booking.getBookingDate(), String.format("0x%06X", booking.getTimeSlots()));
        int conflicts = bookingMapper.countConflictingApprovedBookings(
                booking.getVenueId(),
                booking.getBookingDate(),
                booking.getTimeSlots()
        );
        log.info("【ReviewService】[reviewBooking] 衝突檢查結果：{}筆衝突預約", conflicts);

        if (conflicts > 0) {
            log.warn("【ReviewService】[reviewBooking] 該時段已被佔用，無法通過審核");
            throw new RuntimeException("該時段已被其他已通過之申請佔用");
        }

        // 4. 更新該預約狀態為「已通過(2)」
        int updateResult = bookingMapper.updateStatusWithVersion(bookingId, 2, booking.getVersion());
        if (updateResult == 0) {
            log.warn("【ReviewService】[reviewBooking] 樂觀鎖更新失敗，版本已過期，bookingId={}, 預期版本={}", bookingId, booking.getVersion());
            throw new RuntimeException("預約案已被他人修改，請重新查詢");
        }
        log.info("【ReviewService】[reviewBooking] 成功更新預約狀態為「已通過(2)」，bookingId={}", bookingId);

        // 5. 查詢與該預約衝突的其他「審核中」預約
        log.info("【ReviewService】[reviewBooking] 查詢衝突的其他「審核中」預約");
        List<Booking> pendingConflicts = bookingMapper.selectPendingConflictingBookings(
                booking.getVenueId(),
                booking.getBookingDate(),
                booking.getTimeSlots(),
                bookingId
        );

        // 6. 批量拒絕衝突的「審核中」預約
        if (!pendingConflicts.isEmpty()) {
            List<Long> pendingBookingIds = new ArrayList<>();
            for (Booking pendingBooking : pendingConflicts) {
                pendingBookingIds.add(pendingBooking.getId());
            }
            log.info("【ReviewService】[reviewBooking] 發現 {} 筆衝突的「審核中」預約，準備批量拒絕", pendingBookingIds.size());
            reviewMapper.batchUpdateStatus(pendingBookingIds, 3);
            log.info("【ReviewService】[reviewBooking] 成功批量拒絕衝突的預約");
        } else {
            log.info("【ReviewService】[reviewBooking] 未發現衝突的其他「審核中」預約");
        }

        log.info("【ReviewService】[reviewBooking] 審核預約申請完成，bookingId={}", bookingId);
    }

    // ==========================================
    // 4. 更新審核狀態
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReviewStatus(ReviewRequestDTO request) {
        log.info("【ReviewService】[updateReviewStatus] 開始更新審核狀態，bookingId={}, newStatus={}",
                request.getBookingId(), request.getStatus());

        Long bookingId = request.getBookingId();
        Integer newStatus = request.getStatus();

        // 驗證參數有效性
        if (bookingId == null || bookingId <= 0) {
            log.warn("【ReviewService】[updateReviewStatus] 無效的預約 ID，bookingId={}", bookingId);
            throw new RuntimeException("預約案編號無效");
        }

        if (newStatus == null || newStatus < 0 || newStatus > 4) {
            log.warn("【ReviewService】[updateReviewStatus] 無效的狀態值，status={}", newStatus);
            throw new RuntimeException("審核狀態值無效");
        }

        // 1. 查詢預約原始資料
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            log.warn("【ReviewService】[updateReviewStatus] 預約案不存在，bookingId={}", bookingId);
            throw new RuntimeException("查詢的預約案不存在");
        }

        Integer oldStatus = booking.getStatus();
        log.info("【ReviewService】[updateReviewStatus] 預約原始狀態={}，目標狀態={}", oldStatus, newStatus);

        // 2. 狀態從「拒絕(3)」改為「通過(2)」時需進行衝突檢查
        if (oldStatus == 3 && newStatus == 2) {
            log.info("【ReviewService】[updateReviewStatus] 檢測到從「拒絕(3)」改為「通過(2)」的狀態變更，進行衝突檢查");

            // 進行衝突檢查
            int conflicts = bookingMapper.countConflictingApprovedBookings(
                    booking.getVenueId(),
                    booking.getBookingDate(),
                    booking.getTimeSlots()
            );
            log.info("【ReviewService】[updateReviewStatus] 衝突檢查結果：{}筆衝突預約", conflicts);

            if (conflicts > 0) {
                log.warn("【ReviewService】[updateReviewStatus] 該時段已被佔用，無法改為「通過(2)」");
                throw new RuntimeException("該時段已被其他已通過之申請佔用");
            }

            // 衝突檢查通過，拒絕其他衝突的「審核中」預約
            List<Booking> pendingConflicts = bookingMapper.selectPendingConflictingBookings(
                    booking.getVenueId(),
                    booking.getBookingDate(),
                    booking.getTimeSlots(),
                    bookingId
            );

            if (!pendingConflicts.isEmpty()) {
                List<Long> pendingIds = new ArrayList<>();
                for (Booking pendingBooking : pendingConflicts) {
                    pendingIds.add(pendingBooking.getId());
                }
                log.info("【ReviewService】[updateReviewStatus] 發現 {} 筆衝突的「審核中」預約，準備批量拒絕", pendingIds.size());
                reviewMapper.batchUpdateStatus(pendingIds, 3);
                log.info("【ReviewService】[updateReviewStatus] 成功批量拒絕衝突的預約");
            } else {
                log.info("【ReviewService】[updateReviewStatus] 未發現衝突的「審核中」預約");
            }
        }

        // 3. 更新狀態（使用樂觀鎖確保併發安全）
        int updateResult = bookingMapper.updateStatusWithVersion(bookingId, newStatus, booking.getVersion());
        if (updateResult == 0) {
            log.warn("【ReviewService】[updateReviewStatus] 樂觀鎖更新失敗，版本已過期，bookingId={}, 預期版本={}", bookingId, booking.getVersion());
            throw new RuntimeException("預約案已被他人修改，請重新查詢");
        }

        log.info("【ReviewService】[updateReviewStatus] 成功更新審核狀態，bookingId={}, 舊狀態={}, 新狀態={}",
                bookingId, oldStatus, newStatus);
    }

    // ==========================================
    // 5. 刪除預約案（軟刪除）
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBooking(Long bookingId) {
        log.info("【ReviewService】[deleteBooking] 開始刪除預約案，bookingId={}", bookingId);

        // 驗證預約 ID 有效性
        if (bookingId == null || bookingId <= 0) {
            log.warn("【ReviewService】[deleteBooking] 無效的預約 ID，bookingId={}", bookingId);
            throw new RuntimeException("預約案編號無效");
        }

        // 1. 檢查預約是否存在
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            log.warn("【ReviewService】[deleteBooking] 預約案不存在，bookingId={}", bookingId);
            throw new RuntimeException("查詢的預約案不存在");
        }
        log.info("【ReviewService】[deleteBooking] 預約案存在，準備進行軟刪除");

        // 2. 刪除相關的設備紀錄
        int equipmentDeleteCount = reviewMapper.deleteBookingEquipmentsByBookingId(bookingId);
        log.info("【ReviewService】[deleteBooking] 成功刪除 {} 筆相關的設備紀錄", equipmentDeleteCount);

        // 3. 軟刪除預約案（將狀態改為 4：已刪除）
        int softDeleteResult = reviewMapper.deleteSoftBooking(bookingId);
        if (softDeleteResult == 0) {
            log.warn("【ReviewService】[deleteBooking] 軟刪除失敗，bookingId={}", bookingId);
            throw new RuntimeException("刪除預約案失敗");
        }
        log.info("【ReviewService】[deleteBooking] 成功軟刪除預約案，bookingId={}", bookingId);

        log.info("【ReviewService】[deleteBooking] 預約案刪除完成，bookingId={}", bookingId);
    }
}



