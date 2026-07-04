package tw.edu.ncu.osa.venue_reservation_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.edu.ncu.osa.venue_reservation_service.mapper.EquipmentMapper;
import tw.edu.ncu.osa.venue_reservation_service.mapper.VenueMapper;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentCreateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentUpdateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentVenueRuleDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Equipment;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.EquipmentVenueRule;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Venue;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentVO;
import tw.edu.ncu.osa.venue_reservation_service.service.EquipmentService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {
    private final EquipmentMapper equipmentMapper;
    private final VenueMapper venueMapper;

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentVO> listEquipments(boolean includeDeleted) {
        return equipmentMapper.selectAll(includeDeleted).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentVO getEquipment(Long id) {
        return toVO(requireEquipment(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEquipment(EquipmentCreateDTO request) {
        Equipment existing = equipmentMapper.selectByName(request.getName());
        if (existing != null && existing.getDeletedAt() == null) {
            throw new RuntimeException("設備名稱已存在");
        }
        if (existing != null) {
            existing.setTotalQuantity(request.getTotalQuantity());
            existing.setDescription(request.getDescription());
            existing.setBorrowNote(request.getBorrowNote());
            equipmentMapper.restore(existing.getId());
            equipmentMapper.update(existing);
            replaceVenueRules(existing.getId(), request.getVenueRules());
            return existing.getId();
        }

        Equipment equipment = new Equipment();
        equipment.setName(request.getName());
        equipment.setTotalQuantity(request.getTotalQuantity());
        equipment.setDescription(request.getDescription());
        equipment.setBorrowNote(request.getBorrowNote());
        equipmentMapper.insert(equipment);
        replaceVenueRules(equipment.getId(), request.getVenueRules());
        return equipment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEquipment(Long id, EquipmentUpdateDTO request) {
        Equipment equipment = requireEquipment(id);
        if (request.getName() != null && !request.getName().isBlank()
                && !request.getName().equals(equipment.getName())) {
            Equipment duplicated = equipmentMapper.selectByName(request.getName());
            if (duplicated != null && !duplicated.getId().equals(id)) {
                throw new RuntimeException("設備名稱已存在");
            }
            equipment.setName(request.getName());
        }
        if (request.getTotalQuantity() != null) {
            equipment.setTotalQuantity(request.getTotalQuantity());
        }
        if (request.getDescription() != null) {
            equipment.setDescription(request.getDescription());
        }
        if (request.getBorrowNote() != null) {
            equipment.setBorrowNote(request.getBorrowNote());
        }
        equipmentMapper.update(equipment);
        if (request.getVenueRules() != null) {
            replaceVenueRules(id, request.getVenueRules());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEquipment(Long id) {
        Equipment equipment = requireEquipment(id);
        if (equipment.getDeletedAt() != null) {
            return;
        }
        if (equipmentMapper.countFutureActiveEquipmentBookings(id) > 0) {
            throw new RuntimeException("該設備仍有未完成借用申請，無法刪除");
        }
        equipmentMapper.softDelete(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreEquipment(Long id) {
        requireEquipment(id);
        equipmentMapper.restore(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVenueRules(Long id, List<EquipmentVenueRuleDTO> venueRules) {
        requireEquipment(id);
        replaceVenueRules(id, venueRules);
    }

    private Equipment requireEquipment(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("設備 ID 不可為空或為負數");
        }
        Equipment equipment = equipmentMapper.selectById(id);
        if (equipment == null) {
            throw new RuntimeException("設備不存在");
        }
        return equipment;
    }

    private void replaceVenueRules(Long equipmentId, List<EquipmentVenueRuleDTO> venueRules) {
        equipmentMapper.deleteVenueRules(equipmentId);
        if (venueRules == null || venueRules.isEmpty()) {
            return;
        }
        for (EquipmentVenueRuleDTO dto : venueRules) {
            Venue venue = venueMapper.selectVenueById(dto.getVenueId());
            if (venue == null) {
                throw new RuntimeException("場地不存在：" + dto.getVenueId());
            }
            EquipmentVenueRule rule = new EquipmentVenueRule();
            rule.setEquipmentId(equipmentId);
            rule.setVenueId(dto.getVenueId());
            rule.setRuleNote(dto.getRuleNote());
            equipmentMapper.insertVenueRule(rule);
        }
    }

    private EquipmentVO toVO(Equipment equipment) {
        EquipmentVO vo = new EquipmentVO();
        vo.setId(equipment.getId());
        vo.setName(equipment.getName());
        vo.setTotalQuantity(equipment.getTotalQuantity());
        vo.setDescription(equipment.getDescription());
        vo.setBorrowNote(equipment.getBorrowNote());
        vo.setDeletedAt(equipment.getDeletedAt());
        vo.setCreatedAt(equipment.getCreatedAt());
        vo.setUpdatedAt(equipment.getUpdatedAt());
        var rules = equipmentMapper.selectAllowedVenuesByEquipmentId(equipment.getId());
        vo.setAllowedVenues(rules);
        vo.setVenueRestricted(rules != null && !rules.isEmpty());
        return vo;
    }
}
