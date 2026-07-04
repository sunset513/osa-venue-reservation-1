package tw.edu.ncu.osa.venue_reservation_service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.edu.ncu.osa.venue_reservation_service.mapper.EquipmentBookingMapper;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.*;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.EquipmentBooking;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.EquipmentBookingItem;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentAvailabilityVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBookingPageVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBookingVO;
import tw.edu.ncu.osa.venue_reservation_service.service.EquipmentBookingService;
import tw.edu.ncu.osa.venue_reservation_service.util.BookingUtils;
import tw.edu.ncu.osa.venue_reservation_service.util.UserContext;

import java.util.ArrayList;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentBookingServiceImpl implements EquipmentBookingService {
    private final EquipmentBookingMapper equipmentBookingMapper;
    private final EquipmentBookingSupport support;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBooking(EquipmentBookingCreateDTO request) {
        EquipmentAvailabilityQueryDTO availabilityQuery = toAvailabilityQuery(request, null);
        support.assertVenueRulesForUser(availabilityQuery);

        EquipmentBooking booking = new EquipmentBooking();
        booking.setUserId(UserContext.getUser().getUserId());
        booking.setBorrowDate(request.getBorrowDate());
        booking.setTimeSlots(BookingUtils.convertToMask(request.getSlots()));
        booking.setStatus(1);
        booking.setPurpose(request.getPurpose());
        booking.setContactInfo(writeContactInfo(request.getContactInfo()));
        booking.setRelatedVenueBookingId(request.getRelatedVenueBookingId());
        equipmentBookingMapper.insertBooking(booking);
        insertItems(booking.getId(), support.aggregateItems(request.getItems()));
        return booking.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentBookingVO getMyBooking(Long id) {
        EquipmentBooking booking = support.requireBooking(id);
        assertOwner(booking);
        return support.toVO(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentBookingPageVO queryMyBookings(EquipmentBookingQueryDTO query) {
        query = normalize(query);
        String userId = UserContext.getUser().getUserId();
        Long total = equipmentBookingMapper.countMyBookings(
                userId, query.getStatusList(), query.getStartDate(), query.getEndDate(),
                query.getEquipmentId(), query.getRelatedVenueBookingId(), query.getStandaloneOnly());
        var bookings = equipmentBookingMapper.selectMyBookings(
                userId, query.getStatusList(), query.getStartDate(), query.getEndDate(),
                query.getEquipmentId(), query.getRelatedVenueBookingId(), query.getStandaloneOnly(),
                query.getPageSize(), query.getOffset());
        return toPage(query, total, bookings.stream().map(support::toVO).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBooking(Long id, EquipmentBookingUpdateDTO request) {
        EquipmentBooking booking = support.requireBooking(id);
        assertOwner(booking);
        if (!Integer.valueOf(1).equals(booking.getStatus()) && !Integer.valueOf(2).equals(booking.getStatus())) {
            throw new RuntimeException("此狀態不可修改");
        }

        EquipmentAvailabilityQueryDTO availabilityQuery = toAvailabilityQuery(request, id);
        support.assertVenueRulesForUser(availabilityQuery);

        booking.setBorrowDate(request.getBorrowDate());
        booking.setTimeSlots(BookingUtils.convertToMask(request.getSlots()));
        booking.setStatus(1);
        booking.setPurpose(request.getPurpose());
        booking.setContactInfo(writeContactInfo(request.getContactInfo()));
        booking.setRelatedVenueBookingId(request.getRelatedVenueBookingId());
        int updated = equipmentBookingMapper.updateBooking(booking);
        if (updated == 0) {
            throw new RuntimeException("設備借用申請已被他人修改，請重新查詢");
        }
        equipmentBookingMapper.deleteItemsByBookingId(id);
        insertItems(id, support.aggregateItems(request.getItems()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawBooking(Long id) {
        EquipmentBooking booking = support.requireBooking(id);
        assertOwner(booking);
        if (!Integer.valueOf(1).equals(booking.getStatus()) && !Integer.valueOf(2).equals(booking.getStatus())) {
            throw new RuntimeException("此狀態不可撤回");
        }
        int updated = equipmentBookingMapper.updateStatusWithVersion(
                id, 0, null, null, booking.getVersion());
        if (updated == 0) {
            throw new RuntimeException("設備借用申請已被他人修改，請重新查詢");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentAvailabilityVO checkAvailability(EquipmentAvailabilityQueryDTO query) {
        return support.checkAvailability(query, false);
    }

    private void insertItems(Long bookingId, Map<Long, Integer> items) {
        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            EquipmentBookingItem item = new EquipmentBookingItem();
            item.setEquipmentBookingId(bookingId);
            item.setEquipmentId(entry.getKey());
            item.setQuantity(entry.getValue());
            equipmentBookingMapper.insertItem(item);
        }
    }

    private void assertOwner(EquipmentBooking booking) {
        if (!booking.getUserId().equals(UserContext.getUser().getUserId())) {
            throw new RuntimeException("無權限操作他人的設備借用申請");
        }
    }

    private String writeContactInfo(Object contactInfo) {
        try {
            return objectMapper.writeValueAsString(contactInfo);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("聯絡資訊格式錯誤");
        }
    }

    private EquipmentAvailabilityQueryDTO toAvailabilityQuery(EquipmentBookingCreateDTO request, Long excludeId) {
        EquipmentAvailabilityQueryDTO query = new EquipmentAvailabilityQueryDTO();
        query.setBorrowDate(request.getBorrowDate());
        query.setSlots(request.getSlots());
        query.setRelatedVenueBookingId(request.getRelatedVenueBookingId());
        query.setExcludeEquipmentBookingId(excludeId);
        query.setItems(request.getItems());
        return query;
    }

    private EquipmentAvailabilityQueryDTO toAvailabilityQuery(EquipmentBookingUpdateDTO request, Long excludeId) {
        EquipmentAvailabilityQueryDTO query = new EquipmentAvailabilityQueryDTO();
        query.setBorrowDate(request.getBorrowDate());
        query.setSlots(request.getSlots());
        query.setRelatedVenueBookingId(request.getRelatedVenueBookingId());
        query.setExcludeEquipmentBookingId(excludeId);
        query.setItems(request.getItems());
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
