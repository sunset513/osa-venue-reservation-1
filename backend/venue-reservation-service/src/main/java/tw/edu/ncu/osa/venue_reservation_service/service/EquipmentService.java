package tw.edu.ncu.osa.venue_reservation_service.service;

import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentCreateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentUpdateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentVenueRuleDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentStatusVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentVO;

import java.time.LocalDate;
import java.util.List;

public interface EquipmentService {
    List<EquipmentVO> listEquipments(boolean includeDeleted);

    EquipmentVO getEquipment(Long id);

    List<EquipmentStatusVO> getEquipmentStatuses(LocalDate date, Integer hour);

    Long createEquipment(EquipmentCreateDTO request);

    void updateEquipment(Long id, EquipmentUpdateDTO request);

    void deleteEquipment(Long id);

    void restoreEquipment(Long id);

    void updateVenueRules(Long id, List<EquipmentVenueRuleDTO> venueRules);
}
