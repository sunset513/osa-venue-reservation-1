package tw.edu.ncu.osa.venue_reservation_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import tw.edu.ncu.osa.venue_reservation_service.common.result.Result;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentCreateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentUpdateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentVenueRuleDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentStatusVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentVO;
import tw.edu.ncu.osa.venue_reservation_service.service.EquipmentService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/equipments")
@RequiredArgsConstructor
@Tag(name = "設備主檔管理", description = "設備主檔、總量與可使用場地規則維護")
public class EquipmentController {
    private final EquipmentService equipmentService;

    @GetMapping
    @Operation(summary = "查詢設備清單")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<List<EquipmentVO>> listEquipments(
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        return Result.success(equipmentService.listEquipments(includeDeleted));
    }

    @GetMapping("/status")
    @Operation(summary = "查詢設備目前或指定時段狀態")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<List<EquipmentStatusVO>> getEquipmentStatuses(
            @RequestParam(value = "date", required = false) LocalDate date,
            @RequestParam(value = "hour", required = false) Integer hour) {
        return Result.success(equipmentService.getEquipmentStatuses(date, hour));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查詢設備詳情")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<EquipmentVO> getEquipment(@PathVariable Long id) {
        return Result.success(equipmentService.getEquipment(id));
    }

    @PostMapping
    @Operation(summary = "新增設備")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<Long> createEquipment(@Valid @RequestBody EquipmentCreateDTO request) {
        return Result.success(equipmentService.createEquipment(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改設備")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<Void> updateEquipment(@PathVariable Long id, @Valid @RequestBody EquipmentUpdateDTO request) {
        equipmentService.updateEquipment(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "軟刪除設備")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<Void> deleteEquipment(@PathVariable Long id) {
        equipmentService.deleteEquipment(id);
        return Result.success();
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "恢復已刪除設備")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<Void> restoreEquipment(@PathVariable Long id) {
        equipmentService.restoreEquipment(id);
        return Result.success();
    }

    @PutMapping("/{id}/venue-rules")
    @Operation(summary = "更新設備允許場地規則")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<Void> updateVenueRules(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) List<EquipmentVenueRuleDTO> venueRules) {
        equipmentService.updateVenueRules(id, venueRules);
        return Result.success();
    }
}
