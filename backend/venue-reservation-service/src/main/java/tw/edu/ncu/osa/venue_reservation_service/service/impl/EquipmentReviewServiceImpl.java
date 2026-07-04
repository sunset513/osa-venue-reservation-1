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
                query.getStatusList(), query.getStartDate(), query.getEndDate(), query.getEquipmentId());
        var bookings = equipmentBookingMapper.selectReviewBookings(
                query.getStatusList(), query.getStartDate(), query.getEndDate(),
                query.getEquipmentId(), query.getPageSize(), query.getOffset());
        return toPage(query, total, bookings.stream().map(support::toVO).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentBookingVO getBooking(Long id) {
        return support.toVO(support.requireBooking(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveBooking(Long id) {
        EquipmentBooking booking = support.requireBooking(id);
        if (!Integer.valueOf(1).equals(booking.getStatus())) {
            throw new RuntimeException("僅審核中設備借用申請可核准");
        }
        support.assertApprovalAvailable(toAvailabilityQuery(booking));
        int updated = equipmentBookingMapper.updateStatusWithVersion(
                id, 2, UserContext.getUser().getUserId(), null, booking.getVersion());
        if (updated == 0) {
            throw new RuntimeException("設備借用申請已被他人修改，請重新查詢");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectBooking(Long id, String rejectReason) {
        EquipmentBooking booking = support.requireBooking(id);
        if (!Integer.valueOf(1).equals(booking.getStatus())) {
            throw new RuntimeException("僅審核中設備借用申請可拒絕");
        }
        int updated = equipmentBookingMapper.updateStatusWithVersion(
                id, 3, UserContext.getUser().getUserId(), rejectReason, booking.getVersion());
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
