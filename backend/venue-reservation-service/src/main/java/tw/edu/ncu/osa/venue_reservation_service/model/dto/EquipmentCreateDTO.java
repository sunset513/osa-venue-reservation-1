package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增設備請求物件 (Data Transfer Object)
 * 用於接收前端新增設備的請求參數
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EquipmentCreateDTO {

    /**
     * 設備名稱
     * 必填欄位，唯一性需在業務層檢查
     */
    private String equipmentName;

    /**
     * 場地 ID
     * 必填欄位，表示該設備屬於哪個場地
     */
    private Long venueId;

    /**
     * 設備數量
     * 必填欄位，表示該場地擁有的該設備數量
     * 預設為 1，可根據實際情況調整
     */
    private Integer quantity;
}

