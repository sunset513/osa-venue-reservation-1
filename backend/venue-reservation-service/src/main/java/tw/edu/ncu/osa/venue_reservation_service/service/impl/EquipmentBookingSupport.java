package tw.edu.ncu.osa.venue_reservation_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tw.edu.ncu.osa.venue_reservation_service.mapper.BookingMapper;
import tw.edu.ncu.osa.venue_reservation_service.mapper.EquipmentBookingMapper;
import tw.edu.ncu.osa.venue_reservation_service.mapper.EquipmentMapper;
import tw.edu.ncu.osa.venue_reservation_service.mapper.VenueMapper;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentAvailabilityQueryDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentBookingItemDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Booking;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Equipment;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.EquipmentBooking;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.EquipmentVenueRule;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Venue;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentAvailabilityVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBookingVO;
import tw.edu.ncu.osa.venue_reservation_service.util.BookingUtils;
import tw.edu.ncu.osa.venue_reservation_service.util.UserContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EquipmentBookingSupport {
    private final EquipmentMapper equipmentMapper;
    private final EquipmentBookingMapper equipmentBookingMapper;
    private final BookingMapper bookingMapper;
    private final VenueMapper venueMapper;

    EquipmentBooking requireBooking(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("設備借用申請 ID 不可為空或為負數");
        }
        EquipmentBooking booking = equipmentBookingMapper.selectById(id);
        if (booking == null || Integer.valueOf(4).equals(booking.getStatus())) {
            throw new RuntimeException("設備借用申請不存在");
        }
        return booking;
    }

    EquipmentBookingVO toVO(EquipmentBooking booking) {
        EquipmentBookingVO vo = new EquipmentBookingVO();
        vo.setId(booking.getId());
        vo.setUserId(booking.getUserId());
        vo.setBorrowDate(booking.getBorrowDate());
        vo.setSlots(BookingUtils.parseMaskToList(booking.getTimeSlots()));
        vo.setStatus(booking.getStatus());
        vo.setPurpose(booking.getPurpose());
        vo.setContactInfo(booking.getContactInfo());
        vo.setRelatedVenueBookingId(booking.getRelatedVenueBookingId());
        vo.setReviewedBy(booking.getReviewedBy());
        vo.setReviewedAt(booking.getReviewedAt());
        vo.setRejectReason(booking.getRejectReason());
        vo.setVersion(booking.getVersion());
        vo.setCreatedAt(booking.getCreatedAt());
        vo.setUpdatedAt(booking.getUpdatedAt());
        vo.setItems(equipmentBookingMapper.selectItemVOsByBookingId(booking.getId()));

        if (booking.getRelatedVenueBookingId() != null) {
            Booking venueBooking = bookingMapper.selectById(booking.getRelatedVenueBookingId());
            if (venueBooking != null) {
                vo.setRelatedVenueId(venueBooking.getVenueId());
                Venue venue = venueMapper.selectVenueById(venueBooking.getVenueId());
                if (venue != null) {
                    vo.setRelatedVenueName(venue.getName());
                }
            }
        }
        return vo;
    }

    Map<Long, Integer> aggregateItems(List<EquipmentBookingItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("請至少選擇一項設備");
        }
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (EquipmentBookingItemDTO item : items) {
            if (item.getEquipmentId() == null || item.getEquipmentId() <= 0) {
                throw new RuntimeException("設備 ID 不可為空或為負數");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RuntimeException("借用數量至少為 1");
            }
            result.merge(item.getEquipmentId(), item.getQuantity(), Integer::sum);
        }
        return result;
    }

    EquipmentAvailabilityVO checkAvailability(EquipmentAvailabilityQueryDTO query, boolean requireVenueApproved) {
        Map<Long, Integer> requestedItems = aggregateItems(query.getItems());
        List<Integer> slots = query.getSlots();
        if (slots == null || slots.isEmpty()) {
            throw new RuntimeException("請至少選擇一個時段");
        }

        Booking relatedVenueBooking = null;
        if (query.getRelatedVenueBookingId() != null) {
            relatedVenueBooking = bookingMapper.selectById(query.getRelatedVenueBookingId());
            if (relatedVenueBooking == null || Integer.valueOf(4).equals(relatedVenueBooking.getStatus())) {
                throw new RuntimeException("相關場地預約不存在");
            }
        }

        EquipmentAvailabilityVO result = new EquipmentAvailabilityVO();
        result.setItems(new ArrayList<>());
        result.setAvailable(true);

        for (Map.Entry<Long, Integer> entry : requestedItems.entrySet()) {
            Equipment equipment = equipmentMapper.selectById(entry.getKey());
            if (equipment == null || equipment.getDeletedAt() != null) {
                throw new RuntimeException("設備不存在或已停用：" + entry.getKey());
            }

            EquipmentAvailabilityVO.ItemAvailability item = new EquipmentAvailabilityVO.ItemAvailability();
            item.setEquipmentId(equipment.getId());
            item.setEquipmentName(equipment.getName());
            item.setRequestedQuantity(entry.getValue());
            item.setTotalQuantity(equipment.getTotalQuantity());
            item.setVenueRulePassed(true);
            item.setAvailable(true);

            String ruleError = validateVenueRule(equipment, query, relatedVenueBooking, requireVenueApproved);
            if (ruleError != null) {
                item.setVenueRulePassed(false);
                item.setAvailable(false);
                item.setMessage(ruleError);
                result.setAvailable(false);
            }

            int minAvailable = equipment.getTotalQuantity();
            for (Integer slot : slots) {
                int hourMask = 1 << slot;
                int used = equipmentBookingMapper.sumApprovedQuantityAtHour(
                        equipment.getId(),
                        query.getBorrowDate(),
                        hourMask,
                        query.getExcludeEquipmentBookingId()
                );
                minAvailable = Math.min(minAvailable, equipment.getTotalQuantity() - used);
            }
            item.setMinAvailableQuantity(minAvailable);
            if (entry.getValue() > minAvailable) {
                item.setAvailable(false);
                item.setMessage("設備可借數量不足");
                result.setAvailable(false);
            }
            result.getItems().add(item);
        }

        result.setMessage(Boolean.TRUE.equals(result.getAvailable()) ? "設備可借用" : "設備不可借用");
        return result;
    }

    void assertVenueRulesForUser(EquipmentAvailabilityQueryDTO query) {
        EquipmentAvailabilityVO availability = checkAvailability(query, false);
        if (!Boolean.TRUE.equals(availability.getAvailable())) {
            throw new RuntimeException("設備數量不足或場地規則不符，無法建立申請");
        }
    }

    void assertApprovalAvailable(EquipmentAvailabilityQueryDTO query) {
        EquipmentAvailabilityVO availability = checkAvailability(query, true);
        if (!Boolean.TRUE.equals(availability.getAvailable())) {
            throw new RuntimeException("設備數量不足或場地規則不符，無法核准");
        }
    }

    private String validateVenueRule(
            Equipment equipment,
            EquipmentAvailabilityQueryDTO query,
            Booking relatedVenueBooking,
            boolean requireVenueApproved
    ) {
        List<EquipmentVenueRule> rules = equipmentMapper.selectVenueRulesByEquipmentId(equipment.getId());
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        if (relatedVenueBooking == null) {
            return "此設備需綁定符合規則的場地預約";
        }
        if (requireVenueApproved && !Integer.valueOf(2).equals(relatedVenueBooking.getStatus())) {
            return "限場地設備需綁定已通過的場地預約";
        }
        if (!relatedVenueBooking.getBookingDate().equals(query.getBorrowDate())) {
            return "設備借用日期需與相關場地預約日期相同";
        }
        int mask = BookingUtils.convertToMask(query.getSlots());
        if ((relatedVenueBooking.getTimeSlots() & mask) != mask) {
            return "設備借用時段需包含於相關場地預約時段內";
        }
        String currentUserId = UserContext.getUser() == null ? null : UserContext.getUser().getUserId();
        if (!requireVenueApproved && currentUserId != null && !relatedVenueBooking.getUserId().equals(currentUserId)) {
            return "不可綁定他人的場地預約";
        }
        boolean venueAllowed = rules.stream()
                .anyMatch(rule -> rule.getVenueId().equals(relatedVenueBooking.getVenueId()));
        return venueAllowed ? null : "此設備不可用於該場地";
    }
}
