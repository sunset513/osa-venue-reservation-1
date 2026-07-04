package tw.edu.ncu.osa.venue_reservation_service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Equipment;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.EquipmentVenueRule;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentAllowedVenueVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentStatusRowVO;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface EquipmentMapper {
    List<Equipment> selectAll(@Param("includeDeleted") boolean includeDeleted);

    Equipment selectById(@Param("id") Long id);

    Equipment selectByName(@Param("name") String name);

    int insert(Equipment equipment);

    int update(Equipment equipment);

    int softDelete(@Param("id") Long id);

    int restore(@Param("id") Long id);

    int countFutureActiveEquipmentBookings(@Param("equipmentId") Long equipmentId);

    List<EquipmentVenueRule> selectVenueRulesByEquipmentId(@Param("equipmentId") Long equipmentId);

    List<EquipmentAllowedVenueVO> selectAllowedVenuesByEquipmentId(@Param("equipmentId") Long equipmentId);

    List<EquipmentStatusRowVO> selectEquipmentStatusRows(
            @Param("date") LocalDate date,
            @Param("hourMask") Integer hourMask
    );

    int deleteVenueRules(@Param("equipmentId") Long equipmentId);

    int insertVenueRule(EquipmentVenueRule rule);
}
