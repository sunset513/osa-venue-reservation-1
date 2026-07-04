package tw.edu.ncu.osa.venue_reservation_service.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 場地資訊實體類
 * 對應資料庫 venues 表，管理各單位下的場地基本資訊
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Venue {
    
    /**
     * 場地唯一識別碼
     */
    private Long id;
    
    /**
     * 所屬單位 ID (外鍵關聯至 units 表)
     */
    private Long unitId;
    
    /**
     * 場地名稱 (如：會議室 A)
     */
    private String name;
    
    /**
     * 容納人數上限
     */
    private Integer capacity;
    
    /**
     * 場地介紹或借用規則說明
     */
    private String description;
    
    /**
     * 建立時間
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新時間
     */
    private LocalDateTime updatedAt;
}

