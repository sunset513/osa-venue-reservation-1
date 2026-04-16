# 審核核心引擎：模組 Mapper 設計文件 (V1.0)

**日期：** 2026-04-15  
**類別：** `tw.edu.ncu.osa.venue_reservation_service.mapper.ReviewMapper`

## 一、 設計概述

Mapper 層負責將 Java 物件的操作轉化為 SQL 指令，與資料庫進行交互。Review 模組使用 MyBatis 框架，核心設計重點在於：
1. **複用 BookingMapper 的現有功能**，避免重複開發
2. **實現審核特定的查詢邏輯**，如待審核列表、特定狀態的預約
3. **實現批量狀態更新**，用於連鎖拒絕等操作
4. **支援樂觀鎖更新**，確保並發安全性

本模組在 Review_Service 層呼叫 ReviewMapper 執行審核相關的資料持久化操作。

## 二、 Mapper 複用分析

### 2.1 複用 BookingMapper 的方法

以下 BookingMapper 的方法可直接複用或透過組合使用：

| BookingMapper 方法 | 複用場景 | 說明 |
| :--- | :--- | :--- |
| `selectById` | 獲取預約詳細資訊 | 根據 bookingId 查詢該筆預約案的詳細資訊 |
| `countConflictingApprovedBookings` | 衝突檢查 | 檢查是否存在與給定遮罩重疊且狀態為「已通過」的預約 |
| `selectApprovedBookingsByDateRange` | 獲取已通過預約 | 查詢日期範圍內的已通過預約，用於衝突檢查 |
| `updateStatusWithVersion` | 樂觀鎖狀態更新 | 執行帶版本檢查的狀態更新，防止 Race Condition |
| `selectBookingsByDateRangeForCalendar` | 待審核列表展示 | 查詢日期範圍內的預約並聯接場地和設備資訊 |

### 2.2 ReviewMapper 新增的方法

ReviewMapper 專屬的新增方法如下表所示。

## 三、 ReviewMapper 介面定義 (`ReviewMapper.java`)

`ReviewMapper` 介面定義了審核流程所需的持久化操作：

| 方法名稱 | 參數 | 回傳值 | 功能描述 |
| :--- | :--- | :--- | :--- |
| `selectPendingBookingsByVenueAndDateRange` | `venueId`, `startDate`, `endDate` | `List<BookingVO>` | 查詢特定場地、日期範圍內狀態為「審核中 (1)」的預約案，用於待審核列表 |
| `selectBookingWithEquipments` | `bookingId` | `BookingVO` | 查詢特定預約案的詳細資訊，包含聯接的設備名稱清單 |
| `selectConflictingPendingBookings` | `venueId`, `date`, `mask`, `excludeId` | `List<Booking>` | 查詢同場地、同日期、時段重疊且狀態為「審核中 (1)」的其他預約案（用於連鎖拒絕） |
| `batchUpdateStatus` | `bookingIds`, `newStatus` | `int` | 批量更新預約狀態（用於連鎖拒絕多筆「審核中」申請） |
| `deleteSoftBooking` | `bookingId` | `int` | 執行軟刪除，將預約狀態改為「已刪除 (4)」 |
| `deleteBookingEquipmentsByBookingId` | `bookingId` | `int` | 刪除特定預約的所有設備關聯紀錄 |

## 四、 SQL 映射規範 (`ReviewMapper.xml`)

### 4.1 結果映射 (Result Map)

複用 BookingMapper 中定義的 `bookingVOResultMap`，避免重複定義：

```xml
<!-- ReviewMapper.xml 中可引用或複用 BookingMapper 的結果映射 -->
<resultMap id="bookingVOResultMap" type="tw.edu.ncu.osa.venue_reservation_service.model.vo.BookingVO">
    <!-- 與 BookingMapper 相同 -->
</resultMap>
```

### 4.2 待審核預約列表查詢

**方法**：`selectPendingBookingsByVenueAndDateRange`

**功能**：查詢特定場地、日期範圍內狀態為「審核中 (1)」的預約案。

```xml
<select id="selectPendingBookingsByVenueAndDateRange" resultMap="bookingVOResultMap">
    SELECT
        b.id,
        v.name AS venue_name,
        b.booking_date,
        b.time_slots AS slots,
        b.status,
        b.created_at,
        b.purpose,
        b.p_count,
        b.contact_info,
        GROUP_CONCAT(DISTINCT e.name SEPARATOR ',') AS equipments
    FROM bookings b
    INNER JOIN venues v ON b.venue_id = v.id
    LEFT JOIN booking_equipment be ON b.id = be.booking_id
    LEFT JOIN equipments e ON be.equipment_id = e.id
    WHERE b.venue_id = #{venueId}
      AND b.booking_date BETWEEN #{startDate} AND #{endDate}
      AND b.status = 1
    GROUP BY b.id, v.name, b.booking_date, b.time_slots, b.status, b.created_at, b.purpose, b.p_count, b.contact_info
    ORDER BY b.created_at ASC
</select>
```

**技術說明**：
* 與 `selectBookingsByDateRangeForCalendar` 相似，但篩選條件為 `status = 1`（審核中）
* 使用 `GROUP_CONCAT` 聚合設備名稱，便於前端直接展示
* 按申請時間正序排列，確保先申請的案件優先審核

**性能優化**：
* 利用複合索引 `idx_venue_date (venue_id, booking_date)` 快速定位
* `LEFT JOIN` 設備表，避免設備為空時漏掉預約案

### 4.3 預約詳細資訊查詢

**方法**：`selectBookingWithEquipments`

**功能**：查詢特定預約案的詳細資訊，包含聯接的設備名稱清單。

```xml
<select id="selectBookingWithEquipments" resultMap="bookingVOResultMap">
    SELECT
        b.id,
        v.name AS venue_name,
        b.booking_date,
        b.time_slots AS slots,
        b.status,
        b.created_at,
        b.purpose,
        b.p_count,
        b.contact_info,
        GROUP_CONCAT(DISTINCT e.name SEPARATOR ',') AS equipments
    FROM bookings b
    INNER JOIN venues v ON b.venue_id = v.id
    LEFT JOIN booking_equipment be ON b.id = be.booking_id
    LEFT JOIN equipments e ON be.equipment_id = e.id
    WHERE b.id = #{bookingId}
    GROUP BY b.id, v.name, b.booking_date, b.time_slots, b.status, b.created_at, b.purpose, b.p_count, b.contact_info
</select>
```

**技術說明**：
* 一次查詢即可獲取完整的預約資訊，包括場地名稱和所有借用的設備
* 若預約案不存在或已被刪除，返回 null

**應用場景**：
* 管理員點擊預約案查看詳細資訊時調用
* 審核通過前的最後確認步驟

### 4.4 衝突預約查詢（用於連鎖拒絕）

**方法**：`selectConflictingPendingBookings`

**功能**：查詢同場地、同日期、時段重疊且狀態為「審核中 (1)」的其他預約案。

```xml
<select id="selectConflictingPendingBookings" resultType="tw.edu.ncu.osa.venue_reservation_service.model.entity.Booking">
    SELECT id, venue_id, user_id, booking_date, time_slots, status, purpose, p_count, contact_info, version, created_at, updated_at
    FROM bookings
    WHERE venue_id = #{venueId}
      AND booking_date = #{date}
      AND status = 1
      AND id != #{excludeId}
      AND (time_slots &amp; #{mask}) != 0
    ORDER BY created_at ASC
</select>
```

**技術說明**：
* 使用位元與運算 `(time_slots & #{mask}) != 0` 判定時段重疊
* `id != #{excludeId}` 排除被審核通過的當前預約案，避免自身被拒絕
* 返回 Booking 實體而非 VO，因為後續需要進行狀態更新
* 按申請時間排序，確保後申請的案件被拒絕

**應用場景**：
* 某筆預約審核通過時，自動拒絕所有衝突的「審核中」申請
* 查詢衝突申請的清單，用於批量拒絕操作

### 4.5 批量狀態更新

**方法**：`batchUpdateStatus`

**功能**：批量更新多筆預約的狀態（用於連鎖拒絕）。

```xml
<update id="batchUpdateStatus">
    UPDATE bookings
    SET status = #{newStatus},
        version = version + 1
    WHERE id IN
    <foreach collection="bookingIds" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</update>
```

**技術說明**：
* 使用 MyBatis 的 `<foreach>` 動態 SQL 生成 IN 子句
* 單次執行批量更新，提高資料庫效率
* 同時遞增 version，符合樂觀鎖邏輯
* 返回受影響的行數，便於驗證操作是否成功

**應用場景**：
* 某筆預約審核通過時，連鎖拒絕所有衝突的「審核中」申請
* 避免多次單個更新，提高效能

**效能優化**：
* 單次 UPDATE 語句搞定多筆記錄更新
* 減少往返資料庫的次數，提升並發処理能力

### 4.6 軟刪除操作

**方法**：`deleteSoftBooking`

**功能**：執行軟刪除，將預約狀態改為「已刪除 (4)」。

```xml
<update id="deleteSoftBooking">
    UPDATE bookings
    SET status = 4,
        version = version + 1
    WHERE id = #{bookingId}
</update>
```

**技術說明**：
* 非物理刪除，而是改變狀態值，保留歷史記錄
* 同時遞增 version，符合樂觀鎖邏輯
* 返回受影響的行數，0 表示未找到該預約案

**應用場景**：
* 管理員刪除預約案時調用
* 刪除後的預約案仍保留在資料庫中，便於審計和查詢歷史

### 4.7 級聯刪除設備關聯

**方法**：`deleteBookingEquipmentsByBookingId`

**功能**：刪除特定預約的所有設備關聯紀錄。

```xml
<delete id="deleteBookingEquipmentsByBookingId">
    DELETE FROM booking_equipment
    WHERE booking_id = #{bookingId}
</delete>
```

**技術說明**：
* 與軟刪除配合使用，物理刪除設備關聯記錄
* 不刪除預約案本身，只刪除關聯記錄
* 返回受影響的行數

**應用場景**：
* 配合 `deleteSoftBooking` 執行完整的刪除流程
* 在事務中保證兩個操作的原子性

**備註**：資料庫外鍵約束中已設定 `ON DELETE CASCADE`，理論上可省略此操作。但顯式執行可提高可控性和可讀性。

## 五、 複用 BookingMapper 的具體做法

### 5.1 在 ReviewService 中直接呼叫 BookingMapper

Review 模組的 Service 層可直接注入並呼叫 BookingMapper 的方法，無需在 ReviewMapper 中重複定義：

```java
@Service
public class ReviewService {
    
    @Autowired
    private BookingMapper bookingMapper;
    
    @Autowired
    private ReviewMapper reviewMapper;
    
    // 查詢預約詳細資訊 - 複用 BookingMapper
    public BookingVO getBookingDetails(Long bookingId) {
        Booking booking = bookingMapper.selectById(bookingId);
        // 轉換為 VO...
        return convertToVO(booking);
    }
    
    // 衝突檢查 - 複用 BookingMapper
    public boolean hasConflict(Long venueId, LocalDate date, int mask) {
        int count = bookingMapper.countConflictingApprovedBookings(venueId, date, mask);
        return count > 0;
    }
    
    // 待審核列表 - 使用 ReviewMapper 專用方法
    public List<BookingVO> getPendingBookings(Long venueId, LocalDate startDate, LocalDate endDate) {
        return reviewMapper.selectPendingBookingsByVenueAndDateRange(venueId, startDate, endDate);
    }
}
```

### 5.2 複用的優勢

1. **代碼簡潔**：避免重複定義相同的 SQL 邏輯
2. **維護性高**：修改衝突檢查邏輯時，只需在 BookingMapper 改一個地方
3. **一致性強**：Booking 和 Review 模組使用同一套衝突檢查邏輯
4. **測試友好**：BookingMapper 的衝突檢查邏輯已經過充分測試

## 六、 數據映射與性能考量

### 6.1 索引優化

所有查詢都嚴格遵循現有索引：

| 索引名稱 | 欄位 | 用途 | 適用場景 |
| :--- | :--- | :--- | :--- |
| `idx_venue_date` | (venue_id, booking_date) | 複合索引，優化場地+日期的查詢 | 待審核列表、衝突檢查 |
| `idx_user_id` | user_id | 單欄位索引，優化用戶查詢 | 不直接用於 Review 模組 |

### 6.2 特殊欄位處理

| 欄位名 | 資料庫類型 | Java 類型 | 處理方式 |
| :--- | :--- | :--- | :--- |
| `contact_info` | JSON | String | JSON 序列化由 Service 層處理 |
| `time_slots` | INT UNSIGNED | int | MyBatis 自動映射；位元與運算在 SQL 中執行 |
| `equipments` | （非資料庫欄位） | List\<String\> | GROUP_CONCAT 聚合；自定義 TypeHandler 轉換 |

### 6.3 性能最佳實踐

1. **使用結果映射 (Result Map)**：
   - 複用 `bookingVOResultMap`，減少重複定義
   - 自訂 TypeHandler 處理複雜類型轉換

2. **批量操作**：
   - 連鎖拒絕時使用 `batchUpdateStatus`，單次更新多筆記錄
   - 減少往返資料庫的次數

3. **查詢優化**：
   - 使用 `LEFT JOIN` 而非 `INNER JOIN` 處理設備關聯，避免無設備的預約案被漏掉
   - 使用 `GROUP BY` 和 `DISTINCT` 去重，確保結果準確

## 七、 事務邊界

ReviewMapper 的操作通常在 Service 層的事務內執行：

```java
@Service
public class ReviewService {
    
    @Transactional(rollbackFor = Exception.class)
    public void reviewBooking(Long bookingId) {
        // 1. 查詢預約案（複用 BookingMapper）
        Booking booking = bookingMapper.selectById(bookingId);
        
        // 2. 衝突檢查（複用 BookingMapper）
        int conflictCount = bookingMapper.countConflictingApprovedBookings(...);
        
        // 3. 狀態更新（複用 BookingMapper）
        bookingMapper.updateStatusWithVersion(...);
        
        // 4. 查詢衝突申請（使用 ReviewMapper）
        List<Booking> conflicting = reviewMapper.selectConflictingPendingBookings(...);
        
        // 5. 批量拒絕（使用 ReviewMapper）
        if (!conflicting.isEmpty()) {
            reviewMapper.batchUpdateStatus(...);
        }
        // 事務在方法結束時自動提交或回滾
    }
}
```

## 八、 擴展考量

### 8.1 未來可能的優化

1. **審核日誌表**：記錄每筆審核操作（誰、何時、做了什麼、結果如何）
   - 需新增 SQL：`insertReviewLog`
   
2. **統計查詢**：各場地的審核通過率、平均審核時間等
   - 需新增 SQL：`selectReviewStatistics`
   
3. **批量導出**：管理員可匯出待審核列表為 Excel
   - 現有 SQL 可直接復用，但需服務層支持

### 8.2 與其他模組的集成

* **BookingMapper**：提供衝突檢查、狀態更新等核心操作
* **VenueMapper**（未來）：場地資訊查詢，目前在 SQL 中 `INNER JOIN`
* **EquipmentMapper**（未來）：設備資訊查詢，目前在 SQL 中 `LEFT JOIN`

## 九、 測試建議

### 9.1 單元測試

```java
// ReviewMapperTest.java
class ReviewMapperTest {
    
    @Test
    void testSelectPendingBookingsByVenueAndDateRange() {
        // 準備測試數據
        Long venueId = 1L;
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 30);
        
        // 執行查詢
        List<BookingVO> result = reviewMapper.selectPendingBookingsByVenueAndDateRange(
            venueId, startDate, endDate);
        
        // 驗證結果
        assertNotNull(result);
        result.forEach(vo -> assertEquals(1, vo.getStatus())); // 確保都是審核中
    }
    
    @Test
    void testBatchUpdateStatus() {
        // 準備測試數據
        List<Long> bookingIds = Arrays.asList(1L, 2L, 3L);
        Integer newStatus = 3; // 拒絕
        
        // 執行批量更新
        int affectedRows = reviewMapper.batchUpdateStatus(bookingIds, newStatus);
        
        // 驗證結果
        assertEquals(3, affectedRows);
    }
}
```

### 9.2 集成測試

```java
// ReviewServiceIntegrationTest.java
class ReviewServiceIntegrationTest {
    
    @Test
    @Transactional
    void testReviewBookingWithCascadingRejection() {
        // 準備測試數據：1 筆審核中、2 筆衝突的審核中、1 筆已通過
        
        // 執行審核通過
        reviewService.reviewBooking(approvedBookingId);
        
        // 驗證結果
        // 1. 被審核的案件狀態改為已通過
        // 2. 衝突的審核中案件自動改為拒絕
        // 3. 已通過的案件不受影響
    }
}
```

## 十、 常見陷阱與解決方案

| 陷阱 | 風險 | 解決方案 |
| :--- | :--- | :--- |
| 連鎖拒絕時遺漏某筆衝突預約 | 導致時段被重複佔用 | 使用 `selectConflictingPendingBookings` 確保找到所有衝突案件，使用 `batchUpdateStatus` 批量更新 |
| 樂觀鎖版本號不匹配 | 更新失敗但未通知前端 | 檢查 UPDATE 的受影響行數，若為 0 則拋出 `OptimisticLockException` |
| 軟刪除後仍查到刪除案件 | 資料不一致 | 所有 SELECT 查詢都需加上 `status != 4` 的篩選條件 |
| GROUP_CONCAT 資料過大導致截斷 | 設備列表不完整 | 檢查 MySQL 的 `group_concat_max_len` 設置，根據需要調整 |
| 時段遮罩位元運算溢出 | 衝突判定錯誤 | 確保 `time_slots` 使用 `INT UNSIGNED`，僅使用低 24 位 |


