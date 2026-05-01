package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改設備請求物件 (Data Transfer Object)
 * 用於接收前端修改設備的請求參數
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EquipmentUpdateDTO {

    /**
     * 設備 ID
     * 必填欄位，表示要修改的設備
     */
    private Long id;

    /**
     * 設備名稱
     * 可選欄位，若提供則更改設備名稱
     * 唯一性需在業務層檢查（排除自身）
     */
    private String equipmentName;

    /**
     * 場地 ID
     * 可選欄位，若提供則更改所屬場地
     * 若場地改變，venue_equipment_map 需同步更新
     */
    private Long venueId;

    /**
     * 設備數量
     * 可選欄位，若提供則更新該場地的設備數量
     */
    private Integer quantity;
}

