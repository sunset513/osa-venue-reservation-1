package tw.edu.ncu.osa.venue_reservation_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tw.edu.ncu.osa.venue_reservation_service.common.result.Result;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentAvailabilityQueryDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentBookingCreateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentBookingQueryDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentBookingUpdateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentAvailabilityVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBookingPageVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBookingVO;
import tw.edu.ncu.osa.venue_reservation_service.service.EquipmentBookingService;

@RestController
@RequestMapping("/api/equipment-bookings")
@RequiredArgsConstructor
@Tag(name = "設備借用申請", description = "使用者建立、查詢、修改與撤回獨立設備借用申請")
public class EquipmentBookingController {
    private final EquipmentBookingService equipmentBookingService;

    @PostMapping
    @Operation(summary = "建立設備借用申請")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<Long> createBooking(@Valid @RequestBody EquipmentBookingCreateDTO request) {
        return Result.success(equipmentBookingService.createBooking(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查詢我的設備借用詳情")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<EquipmentBookingVO> getMyBooking(@PathVariable Long id) {
        return Result.success(equipmentBookingService.getMyBooking(id));
    }

    @PostMapping("/query")
    @Operation(summary = "查詢我的設備借用列表")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<EquipmentBookingPageVO> queryMyBookings(@RequestBody(required = false) EquipmentBookingQueryDTO query) {
        return Result.success(equipmentBookingService.queryMyBookings(query));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改設備借用申請")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<Void> updateBooking(@PathVariable Long id, @Valid @RequestBody EquipmentBookingUpdateDTO request) {
        equipmentBookingService.updateBooking(id, request);
        return Result.success();
    }

    @PutMapping("/{id}/withdraw")
    @Operation(summary = "撤回設備借用申請")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<Void> withdrawBooking(@PathVariable Long id) {
        equipmentBookingService.withdrawBooking(id);
        return Result.success();
    }

    @PostMapping("/availability")
    @Operation(summary = "檢查設備可用量")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<EquipmentAvailabilityVO> checkAvailability(@Valid @RequestBody EquipmentAvailabilityQueryDTO query) {
        return Result.success(equipmentBookingService.checkAvailability(query));
    }
}
