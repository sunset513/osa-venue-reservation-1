package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class EquipmentBookingPageVO {
    private Long total;
    private Integer pageNo;
    private Integer pageSize;
    private Integer totalPages;
    private Boolean hasNext;
    private List<EquipmentBookingVO> items;

    public static Integer calculateTotalPages(Long total, Integer pageSize) {
        if (total == null || total == 0 || pageSize == null || pageSize == 0) {
            return 0;
        }
        return (int) Math.ceil(total.doubleValue() / pageSize.doubleValue());
    }
}
