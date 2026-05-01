package tw.edu.ncu.osa.venue_reservation_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 設備借用紀錄查詢請求物件 (Data Transfer Object)
 * 用於接收前端分頁查詢參數
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EquipmentBorrowQueryDTO {

    /**
     * 頁碼（從 1 開始）
     * 默認值：1
     */
    private Integer pageNum = 1;

    /**
     * 每頁筆數
     * 默認值：10，最大值：100
     */
    private Integer pageSize = 10;

    /**
     * 驗證分頁參數的有效性
     * @return 若參數有效返回 true
     */
    public boolean isValid() {
        // ==========================================
        // 參數驗證邏輯
        // ==========================================

        // 檢查 pageNum 是否為正整數
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }

        // 檢查 pageSize 是否在有效範圍內
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }

        return true;
    }
}

