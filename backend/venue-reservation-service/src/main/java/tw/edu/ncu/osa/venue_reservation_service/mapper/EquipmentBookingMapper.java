package tw.edu.ncu.osa.venue_reservation_service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.EquipmentBooking;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.EquipmentBookingItem;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBookingItemVO;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface EquipmentBookingMapper {
    int insertBooking(EquipmentBooking booking);

    int updateBooking(EquipmentBooking booking);

    int insertItem(EquipmentBookingItem item);

    int deleteItemsByBookingId(@Param("bookingId") Long bookingId);

    EquipmentBooking selectById(@Param("id") Long id);

    List<EquipmentBookingItem> selectItemsByBookingId(@Param("bookingId") Long bookingId);

    List<EquipmentBookingItemVO> selectItemVOsByBookingId(@Param("bookingId") Long bookingId);

    Long countMyBookings(
            @Param("userId") String userId,
            @Param("statusList") List<Integer> statusList,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("equipmentId") Long equipmentId,
            @Param("relatedVenueBookingId") Long relatedVenueBookingId,
            @Param("standaloneOnly") Boolean standaloneOnly
    );

    List<EquipmentBooking> selectMyBookings(
            @Param("userId") String userId,
            @Param("statusList") List<Integer> statusList,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("equipmentId") Long equipmentId,
            @Param("relatedVenueBookingId") Long relatedVenueBookingId,
            @Param("standaloneOnly") Boolean standaloneOnly,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    Long countReviewBookings(
            @Param("statusList") List<Integer> statusList,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("equipmentId") Long equipmentId,
            @Param("relatedVenueBookingId") Long relatedVenueBookingId,
            @Param("standaloneOnly") Boolean standaloneOnly
    );

    List<EquipmentBooking> selectReviewBookings(
            @Param("statusList") List<Integer> statusList,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("equipmentId") Long equipmentId,
            @Param("relatedVenueBookingId") Long relatedVenueBookingId,
            @Param("standaloneOnly") Boolean standaloneOnly,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    int updateStatusWithVersion(
            @Param("id") Long id,
            @Param("newStatus") Integer newStatus,
            @Param("reviewedBy") String reviewedBy,
            @Param("oldVersion") Integer oldVersion
    );

    List<EquipmentBooking> selectPendingConflictingBookings(
            @Param("approvedBookingId") Long approvedBookingId,
            @Param("borrowDate") LocalDate borrowDate,
            @Param("timeSlots") Integer timeSlots,
            @Param("equipmentIds") List<Long> equipmentIds
    );

    int batchRejectPendingBookings(
            @Param("ids") List<Long> ids,
            @Param("reviewedBy") String reviewedBy
    );

    int sumApprovedQuantityAtHour(
            @Param("equipmentId") Long equipmentId,
            @Param("borrowDate") LocalDate borrowDate,
            @Param("hourMask") Integer hourMask,
            @Param("excludeEquipmentBookingId") Long excludeEquipmentBookingId
    );
}
