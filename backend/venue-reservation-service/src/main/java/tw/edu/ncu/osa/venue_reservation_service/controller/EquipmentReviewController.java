package tw.edu.ncu.osa.venue_reservation_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tw.edu.ncu.osa.venue_reservation_service.common.result.Result;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentBookingQueryDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.EquipmentBookingRejectDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBookingPageVO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.EquipmentBookingVO;
import tw.edu.ncu.osa.venue_reservation_service.service.EquipmentReviewService;

@RestController
@RequestMapping("/api/equipment-reviews")
@RequiredArgsConstructor
@Tag(name = "設備借用審核", description = "管理端查詢、核准與拒絕設備借用申請")
public class EquipmentReviewController {
    private final EquipmentReviewService equipmentReviewService;

    @PostMapping("/query")
    @Operation(summary = "管理端查詢設備借用申請")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<EquipmentBookingPageVO> queryBookings(@RequestBody(required = false) EquipmentBookingQueryDTO query) {
        return Result.success(equipmentReviewService.queryBookings(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "管理端查詢設備借用詳情")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<EquipmentBookingVO> getBooking(@PathVariable Long id) {
        return Result.success(equipmentReviewService.getBooking(id));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "核准設備借用申請")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<Void> approveBooking(@PathVariable Long id) {
        equipmentReviewService.approveBooking(id);
        return Result.success();
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "拒絕設備借用申請")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<Void> rejectBooking(@PathVariable Long id, @Valid @RequestBody EquipmentBookingRejectDTO request) {
        equipmentReviewService.rejectBooking(id, request.getRejectReason());
        return Result.success();
    }
}
