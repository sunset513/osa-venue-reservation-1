package tw.edu.ncu.osa.venue_reservation_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.edu.ncu.osa.venue_reservation_service.mapper.EquipmentBookingMapper;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentAvailabilityQueryDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentBookingItemDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentBookingQueryDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.EquipmentBooking;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBookingPageVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBookingVO;
import tw.edu.ncu.osa.venue_reservation_service.service.EquipmentReviewService;
import tw.edu.ncu.osa.venue_reservation_service.util.BookingUtils;
import tw.edu.ncu.osa.venue_reservation_service.util.UserContext;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipmentReviewServiceImpl implements EquipmentReviewService {
    private final EquipmentBookingMapper equipmentBookingMapper;
    private final EquipmentBookingSupport support;

    @Override
    @Transactional(readOnly = true)
    public EquipmentBookingPageVO queryBookings(EquipmentBookingQueryDTO query) {
        query = normalize(query);
        Long total = equipmentBookingMapper.countReviewBookings(
                query.getStatusList(), query.getStartDate(), query.getEndDate(),
                query.getEquipmentId(), query.getRelatedVenueBookingId(), query.getStandaloneOnly());
        var bookings = equipmentBookingMapper.selectReviewBookings(
                query.getStatusList(), query.getStartDate(), query.getEndDate(),
                query.getEquipmentId(), query.getRelatedVenueBookingId(), query.getStandaloneOnly(),
                query.getPageSize(), query.getOffset());
        return toPage(query, total, bookings.stream().map(support::toVO).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentBookingVO getBooking(Long id) {
        return support.toVO(support.requireBooking(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentBookingVO> getBookingsByVenueBooking(Long bookingId) {
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException("場地預約 ID 不可為空或為負數");
        }
        var bookings = equipmentBookingMapper.selectReviewBookings(
                null, null, null, null, bookingId, false, 100, 0);
        return bookings.stream().map(support::toVO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Long countStandalonePendingBookings() {
        return equipmentBookingMapper.countReviewBookings(
                List.of(1), null, null, null, null, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveBooking(Long id) {
        updateBookingStatus(id, 2);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectBooking(Long id) {
        updateBookingStatus(id, 3);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBookingStatus(Long id, Integer status) {
        if (status == null || status < 1 || status > 3) {
            throw new RuntimeException("設備審核狀態值無效");
        }
        EquipmentBooking booking = support.requireBooking(id);
        if (Integer.valueOf(0).equals(booking.getStatus())) {
            throw new RuntimeException("已撤回的設備借用申請不可由審核端重新啟用");
        }

        // Approved equipment bookings occupy stock, so any transition into
        // approved status must re-run the same availability and venue-rule
        // checks used by the dedicated approve endpoint.
        if (Integer.valueOf(2).equals(status)) {
            support.assertApprovalAvailable(toAvailabilityQuery(booking));
        }

        int updated = equipmentBookingMapper.updateStatusWithVersion(
                id, status, UserContext.getUser().getUserId(), booking.getVersion());
        if (updated == 0) {
            throw new RuntimeException("設備借用申請已被他人修改，請重新查詢");
        }
    }

    private EquipmentAvailabilityQueryDTO toAvailabilityQuery(EquipmentBooking booking) {
        EquipmentAvailabilityQueryDTO query = new EquipmentAvailabilityQueryDTO();
        query.setBorrowDate(booking.getBorrowDate());
        query.setSlots(BookingUtils.parseMaskToList(booking.getTimeSlots()));
        query.setRelatedVenueBookingId(booking.getRelatedVenueBookingId());
        query.setExcludeEquipmentBookingId(booking.getId());
        query.setItems(equipmentBookingMapper.selectItemsByBookingId(booking.getId()).stream().map(item -> {
            EquipmentBookingItemDTO dto = new EquipmentBookingItemDTO();
            dto.setEquipmentId(item.getEquipmentId());
            dto.setQuantity(item.getQuantity());
            return dto;
        }).toList());
        return query;
    }

    private EquipmentBookingQueryDTO normalize(EquipmentBookingQueryDTO query) {
        if (query == null) {
            query = new EquipmentBookingQueryDTO();
        }
        if (query.getPageNo() == null || query.getPageNo() < 1) {
            query.setPageNo(1);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(20);
        }
        if (query.getPageSize() > 100) {
            query.setPageSize(100);
        }
        return query;
    }

    private EquipmentBookingPageVO toPage(EquipmentBookingQueryDTO query, Long total, java.util.List<EquipmentBookingVO> items) {
        EquipmentBookingPageVO page = new EquipmentBookingPageVO();
        page.setTotal(total);
        page.setPageNo(query.getPageNo());
        page.setPageSize(query.getPageSize());
        page.setTotalPages(EquipmentBookingPageVO.calculateTotalPages(total, query.getPageSize()));
        page.setHasNext(query.getPageNo() < page.getTotalPages());
        page.setItems(items == null ? new ArrayList<>() : items);
        return page;
    }
}
