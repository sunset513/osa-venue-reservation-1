package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EquipmentBookingQueryDTO {
    private List<Integer> statusList;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long equipmentId;
    private Long relatedVenueBookingId;
    private Boolean standaloneOnly;
    private Integer pageNo = 1;
    private Integer pageSize = 20;

    public Integer getOffset() {
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : pageSize;
        return (safePageNo - 1) * safePageSize;
    }
}
