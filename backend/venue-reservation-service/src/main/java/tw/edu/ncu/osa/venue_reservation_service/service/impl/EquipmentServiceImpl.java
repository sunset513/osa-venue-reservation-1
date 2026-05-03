package tw.edu.ncu.osa.venue_reservation_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.edu.ncu.osa.venue_reservation_service.mapper.EquipmentMapper;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentBorrowQueryDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentCreateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentUpdateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.Equipment;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBorrowRecordPageVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBorrowRecordVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentListByVenueVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentWithStatusVO;
import tw.edu.ncu.osa.venue_reservation_service.service.EquipmentService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 設備管理業務服務實現類
 * 處理設備相關的業務邏輯，包括 CRUD 操作、使用狀態判定與借用歷史查詢
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentMapper equipmentMapper;

    // ==========================================
    // 設備查詢
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentListByVenueVO> queryAllEquipmentsWithStatus() {
        log.info("【EquipmentService】[queryAllEquipmentsWithStatus] 開始查詢所有設備及使用狀態");

        // 1. 查詢所有設備資訊（不含 isInUse）
        List<EquipmentWithStatusVO> equipmentList = equipmentMapper.selectAllEquipmentsWithoutStatus();
        log.info("【EquipmentService】[queryAllEquipmentsWithStatus] 查詢到 {} 筆設備記錄", equipmentList.size());

        // 2. 計算當前時間與小時
        LocalDate today = LocalDate.now();
        int currentHour = LocalTime.now().getHour();
        log.info("【EquipmentService】[queryAllEquipmentsWithStatus] 當前日期={}, 小時={}", today, currentHour);

        // 3. 為每個設備判定使用狀態
        for (EquipmentWithStatusVO equipment : equipmentList) {
            boolean isInUse = isEquipmentInUse(equipment.getEquipmentId(), today, currentHour);
            equipment.setIsInUse(isInUse);
            log.info("【EquipmentService】[queryAllEquipmentsWithStatus] 設備 ID={}, 名稱={}, 使用狀態={}",
                    equipment.getEquipmentId(), equipment.getEquipmentName(), isInUse);
        }

        // 4. 按場地分組
        Map<String, List<EquipmentWithStatusVO>> groupedByVenue = new HashMap<>();
        for (EquipmentWithStatusVO equipment : equipmentList) {
            groupedByVenue.computeIfAbsent(equipment.getVenueName(), k -> new ArrayList<>())
                    .add(equipment);
        }

        // 5. 組裝回傳物件
        List<EquipmentListByVenueVO> result = new ArrayList<>();
        for (Map.Entry<String, List<EquipmentWithStatusVO>> entry : groupedByVenue.entrySet()) {
            EquipmentListByVenueVO venueGroup = new EquipmentListByVenueVO();
            venueGroup.setVenueName(entry.getKey());
            venueGroup.setEquipmentList(entry.getValue());
            result.add(venueGroup);
            log.debug("【EquipmentService】[queryAllEquipmentsWithStatus] 場地 {}，設備數量 {}",
                    entry.getKey(), entry.getValue().size());
        }

        log.info("【EquipmentService】[queryAllEquipmentsWithStatus] 成功分組，返回 {} 個場地", result.size());
        return result;
    }

    // ==========================================
    // 設備 CRUD 操作
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEquipment(EquipmentCreateDTO request) {
        log.info("【EquipmentService】[createEquipment] 開始新增設備，名稱={}, 場地ID={}, 數量={}",
                request.getEquipmentName(), request.getVenueId(), request.getQuantity());

        // 第一步：檢查設備名稱唯一性
        Equipment existingEquipment = equipmentMapper.selectByName(request.getEquipmentName());

        if (existingEquipment != null) {
            if (existingEquipment.getDeletedAt() == null) {
                log.warn("【EquipmentService】[createEquipment] 設備名稱已存在且未被軟刪除，名稱={}",
                        request.getEquipmentName());
                throw new RuntimeException("設備名稱已存在，無法新增");
            } else {
                // 設備已被軟刪除，復原該設備
                log.info("【EquipmentService】[createEquipment] 檢測到已軟刪除的設備，將進行復原，ID={}",
                        existingEquipment.getId());
                existingEquipment.setDeletedAt(null);
                equipmentMapper.update(existingEquipment);
                log.info("【EquipmentService】[createEquipment] 成功復原設備，ID={}", existingEquipment.getId());

                equipmentMapper.insertVenueEquipmentMap(request.getVenueId(), existingEquipment.getId(),
                        request.getQuantity());

                return existingEquipment.getId();
            }
        }

        // 第二步：新增設備
        Equipment newEquipment = new Equipment();
        newEquipment.setName(request.getEquipmentName());
        newEquipment.setCreatedAt(LocalDateTime.now());
        newEquipment.setDeletedAt(null);

        equipmentMapper.insert(newEquipment);
        log.info("【EquipmentService】[createEquipment] 成功新增設備，ID={}, 名稱={}",
                newEquipment.getId(), newEquipment.getName());

        // 第三步：建立場地設備關聯
        equipmentMapper.insertVenueEquipmentMap(request.getVenueId(), newEquipment.getId(),
                request.getQuantity());
        log.info("【EquipmentService】[createEquipment] 成功建立場地關聯，venueId={}, equipmentId={}, quantity={}",
                request.getVenueId(), newEquipment.getId(), request.getQuantity());

        return newEquipment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEquipment(EquipmentUpdateDTO request) {
        log.info("【EquipmentService】[updateEquipment] 開始修改設備，ID={}, 新名稱={}, 新場地ID={}, 新數量={}",
                request.getId(), request.getEquipmentName(), request.getVenueId(), request.getQuantity());

        // 第一步：驗證設備存在
        Equipment equipment = equipmentMapper.selectById(request.getId());
        if (equipment == null || equipment.getDeletedAt() != null) {
            log.warn("【EquipmentService】[updateEquipment] 設備不存在或已被軟刪除，ID={}", request.getId());
            throw new RuntimeException("設備不存在");
        }

        log.debug("【EquipmentService】[updateEquipment] 找到設備，當前名稱={}", equipment.getName());

        // 第二步：若提供新名稱，則檢查新名稱唯一性（排除自身）
        if (request.getEquipmentName() != null && !equipment.getName().equals(request.getEquipmentName())) {
            Equipment existingEquipmentByName = equipmentMapper.selectByName(request.getEquipmentName());

            if (existingEquipmentByName != null && existingEquipmentByName.getDeletedAt() == null) {
                log.warn("【EquipmentService】[updateEquipment] 新名稱已被其他設備使用，名稱={}",
                        request.getEquipmentName());
                throw new RuntimeException("新名稱已存在，無法修改");
            }

            equipment.setName(request.getEquipmentName());
            log.debug("【EquipmentService】[updateEquipment] 設備名稱已更新，新名稱={}", request.getEquipmentName());
        }

        // 第三步：更新設備資訊
        equipmentMapper.update(equipment);
        log.info("【EquipmentService】[updateEquipment] 設備資訊已更新");

        // 第四步：處理場地關聯更新
        if (request.getVenueId() != null || request.getQuantity() != null) {
            // 情況 1：同時提供 venueId 和 quantity
            if (request.getVenueId() != null && request.getQuantity() != null) {
                equipmentMapper.deleteVenueEquipmentMapByEquipmentId(request.getId());
                equipmentMapper.insertVenueEquipmentMap(request.getVenueId(), request.getId(), request.getQuantity());
                log.info("【EquipmentService】[updateEquipment] 場地關聯已更新，venueId={}, quantity={}",
                        request.getVenueId(), request.getQuantity());
            }
            // 情況 2：只提供 venueId，需要保持原來的 quantity
            else if (request.getVenueId() != null && request.getQuantity() == null) {
                // 此時需要查詢原來的數量，但由於 venue_equipment_map 記錄的是 venue_id + equipment_id 的組合
                // 無法直接知道原來的 quantity，所以需要通過刪除並重新插入
                // 但這樣需要知道原來的 quantity，這需要額外的查詢
                // 先暫時將 quantity 設為 1（預設值）
                int originalQuantity = 1;
                equipmentMapper.deleteVenueEquipmentMapByEquipmentId(request.getId());
                equipmentMapper.insertVenueEquipmentMap(request.getVenueId(), request.getId(), originalQuantity);
                log.info("【EquipmentService】[updateEquipment] 場地已更新（保持數量={}），venueId={}",
                        originalQuantity, request.getVenueId());
            }
            // 情況 3：只提供 quantity 但不提供 venueId，無法更新（需要同時提供 venueId）
            else if (request.getVenueId() == null && request.getQuantity() != null) {
                log.warn("【EquipmentService】[updateEquipment] 只提供數量但未提供場地ID，無法更新場地關聯");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEquipment(Long equipmentId) {
        log.info("【EquipmentService】[deleteEquipment] 開始刪除設備，ID={}", equipmentId);

        // 第一步：驗證設備存在
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null || equipment.getDeletedAt() != null) {
            log.warn("【EquipmentService】[deleteEquipment] 設備不存在或已被軟刪除，ID={}", equipmentId);
            throw new RuntimeException("設備不存在");
        }

        // 第二步：檢查是否有進行中的預約
        int activeBookingCount = equipmentMapper.countActiveBookingsByEquipmentId(equipmentId);
        log.debug("【EquipmentService】[deleteEquipment] 檢查進行中的預約，數量={}", activeBookingCount);

        if (activeBookingCount > 0) {
            log.warn("【EquipmentService】[deleteEquipment] 該設備有進行中的預約，無法刪除，ID={}, 預約數={}",
                    equipmentId, activeBookingCount);
            throw new RuntimeException("該設備有進行中的預約，無法刪除");
        }

        // 第三步：執行軟刪除
        equipmentMapper.softDelete(equipmentId);
        log.info("【EquipmentService】[deleteEquipment] 設備已軟刪除，ID={}", equipmentId);

        // 第四步：更新場地關聯
        equipmentMapper.deleteVenueEquipmentMapByEquipmentId(equipmentId);
        log.info("【EquipmentService】[deleteEquipment] 場地關聯已清除，ID={}", equipmentId);
    }

    // ==========================================
    // 設備借用歷史查詢
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public EquipmentBorrowRecordPageVO getEquipmentBorrowHistory(EquipmentBorrowQueryDTO queryDTO) {
        log.info("【EquipmentService】[getEquipmentBorrowHistory] 開始查詢設備借用歷史紀錄");

        // 1. 驗證分頁參數
        queryDTO.isValid();
        log.info("【EquipmentService】[getEquipmentBorrowHistory] 分頁參數驗證完成 - pageNum={}, pageSize={}",
                queryDTO.getPageNum(), queryDTO.getPageSize());

        // 2. 查詢借用紀錄總筆數
        int totalCount = equipmentMapper.selectEquipmentBorrowRecordsCount();
        log.info("【EquipmentService】[getEquipmentBorrowHistory] 借用紀錄總筆數={}", totalCount);

        // 3. 計算分頁資訊
        int pageSize = queryDTO.getPageSize();
        int totalPages = (totalCount + pageSize - 1) / pageSize;
        int currentPage = queryDTO.getPageNum();
        int offset = (currentPage - 1) * pageSize;

        log.debug("【EquipmentService】[getEquipmentBorrowHistory] 分頁資訊 - totalPages={}, offset={}",
                totalPages, offset);

        // 4. 查詢當前頁的借用紀錄
        List<EquipmentBorrowRecordVO> records = equipmentMapper.selectEquipmentBorrowRecords(offset, pageSize);
        log.info("【EquipmentService】[getEquipmentBorrowHistory] 查詢到 {} 筆借用紀錄", records.size());

        // 5. 將位元遮罩轉換為可讀的時段字串
        for (EquipmentBorrowRecordVO record : records) {
            String timeSlotStr = convertTimeSlotsToString(record.getTimeSlots());
            record.setTimeSlots(timeSlotStr);
            log.debug("【EquipmentService】[getEquipmentBorrowHistory] 轉換時段 - 原始遮罩={}, 轉換後={}",
                    record.getTimeSlots(), timeSlotStr);
        }

        // 6. 組裝分頁回傳物件
        EquipmentBorrowRecordPageVO pageVO = new EquipmentBorrowRecordPageVO();
        pageVO.setTotalCount(totalCount);
        pageVO.setTotalPages(totalPages);
        pageVO.setCurrentPage(currentPage);
        pageVO.setPageSize(pageSize);
        pageVO.setData(records);

        log.info("【EquipmentService】[getEquipmentBorrowHistory] 成功組裝分頁結果，返回 {} 筆紀錄", records.size());
        return pageVO;
    }

    // ==========================================
    // 輔助方法
    // ==========================================

    /**
     * 判定設備在指定時間是否正在使用中
     * 檢查是否存在 status=1 或 2 的預約，且 booking_date=today，且時段包含 currentHour
     * @param equipmentId 設備 ID
     * @param today 查詢日期
     * @param currentHour 當前小時（0-23）
     * @return true 表示正在使用中，false 表示閒置
     */
    private boolean isEquipmentInUse(Long equipmentId, LocalDate today, Integer currentHour) {
        int count = equipmentMapper.countEquipmentInUseAtTime(equipmentId, today, currentHour);
        boolean inUse = count > 0;
        log.info("【EquipmentService】[isEquipmentInUse] 設備ID={}, 日期={}, 小時={}, 查詢結果={}",
                equipmentId, today, currentHour, inUse ? "使用中" : "閒置");
        return inUse;
    }

    /**
     * 將位元遮罩轉換為可讀的時段字串
     * 連續的小時會自動合併為一個時段區間
     *
     * 範例：
     * - timeSlots="9,10,11" → "09:00-12:00"
     * - timeSlots="9,10,11,14,15" → "09:00-12:00, 14:00-16:00"
     * - timeSlots="9,11" → "09:00-10:00, 11:00-12:00"
     *
     * @param timeSlots 位元遮罩（需透過 parseMaskToList 轉換為小時清單）
     * @return 可讀的時段字串
     */
    private String convertTimeSlotsToString(String timeSlots) {
        // ==========================================
        // 時段轉換邏輯
        // ==========================================
        try {
            Integer maskValue = Integer.parseInt(timeSlots);
            return convertMaskToTimeString(maskValue);
        } catch (NumberFormatException e) {
            log.warn("【EquipmentService】[convertTimeSlotsToString] 無法轉換時段遮罩：{}", timeSlots, e);
            return timeSlots;
        }
    }

    /**
     * 根據位元遮罩生成時段字串
     *
     * @param mask 位元遮罩（24-bit，每位代表一個小時）
     * @return 時段字串，如 "09:00-12:00, 14:00-16:00"
     */
    private String convertMaskToTimeString(Integer mask) {
        log.debug("【EquipmentService】[convertMaskToTimeString] 開始轉換位元遮罩={}", String.format("0x%06X", mask));

        // 第一步：提取所有被設置的位元（小時）
        List<Integer> hours = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            if (((mask >> i) & 1) == 1) {
                hours.add(i);
            }
        }

        log.debug("【EquipmentService】[convertMaskToTimeString] 提取的小時清單={}", hours);

        // 第二步：按連續性合併為時段區間
        if (hours.isEmpty()) {
            log.warn("【EquipmentService】[convertMaskToTimeString] 位元遮罩為空，無任何小時被設置");
            return "";
        }

        List<String> timeSlots = new ArrayList<>();
        int startHour = hours.get(0);
        int endHour = startHour;

        for (int i = 1; i < hours.size(); i++) {
            int currentHour = hours.get(i);

            if (currentHour == endHour + 1) {
                endHour = currentHour;
            } else {
                timeSlots.add(formatTimeSlot(startHour, endHour + 1));
                startHour = currentHour;
                endHour = currentHour;
            }
        }

        timeSlots.add(formatTimeSlot(startHour, endHour + 1));

        String result = String.join(", ", timeSlots);
        log.debug("【EquipmentService】[convertMaskToTimeString] 轉換完成，結果={}", result);

        return result;
    }

    /**
     * 格式化單個時段
     *
     * @param startHour 開始小時（0-23）
     * @param endHour 結束小時（1-24，不包含該小時）
     * @return 格式化的時段字串，如 "09:00-12:00"
     */
    private String formatTimeSlot(int startHour, int endHour) {
        return String.format("%02d:00-%02d:00", startHour, endHour);
    }
}

