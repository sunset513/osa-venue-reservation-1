package tw.edu.ncu.osa.venue_reservation_service.service;

import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentBookingQueryDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBookingPageVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBookingVO;

public interface EquipmentReviewService {
    EquipmentBookingPageVO queryBookings(EquipmentBookingQueryDTO query);

    EquipmentBookingVO getBooking(Long id);

    void approveBooking(Long id);

    void rejectBooking(Long id, String rejectReason);
}
