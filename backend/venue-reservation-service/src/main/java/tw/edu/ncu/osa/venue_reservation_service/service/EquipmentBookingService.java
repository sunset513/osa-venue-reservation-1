package tw.edu.ncu.osa.venue_reservation_service.service;

import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentAvailabilityQueryDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentBookingCreateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentBookingQueryDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentBookingUpdateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentAvailabilityVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBookingPageVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBookingVO;

public interface EquipmentBookingService {
    Long createBooking(EquipmentBookingCreateDTO request);

    EquipmentBookingVO getMyBooking(Long id);

    EquipmentBookingPageVO queryMyBookings(EquipmentBookingQueryDTO query);

    void updateBooking(Long id, EquipmentBookingUpdateDTO request);

    void withdrawBooking(Long id);

    EquipmentAvailabilityVO checkAvailability(EquipmentAvailabilityQueryDTO query);
}
