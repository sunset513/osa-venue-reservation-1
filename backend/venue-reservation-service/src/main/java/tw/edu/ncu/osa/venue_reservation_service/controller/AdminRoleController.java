package tw.edu.ncu.osa.venue_reservation_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tw.edu.ncu.osa.venue_reservation_service.common.result.Result;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.AdminRoleRequestDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.AdminRoleUpdateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.AdminRoleVO;
import tw.edu.ncu.osa.venue_reservation_service.service.AdminRoleService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin-roles")
@RequiredArgsConstructor
@Tag(name = "管理員角色", description = "提供 level 1 管理員維護 admin role 的 API")
public class AdminRoleController {
    private final AdminRoleService adminRoleService;

    @GetMapping
    @Operation(summary = "查詢管理員角色清單")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<List<AdminRoleVO>> listAdminRoles(
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        return Result.success(adminRoleService.listAdminRoles(includeDeleted));
    }

    @PostMapping
    @Operation(summary = "新增或恢復管理員角色")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<AdminRoleVO> createAdminRole(@Valid @RequestBody AdminRoleRequestDTO request) {
        return Result.success(adminRoleService.createAdminRole(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新管理員等級")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<AdminRoleVO> updateAdminRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminRoleUpdateDTO request) {
        return Result.success(adminRoleService.updateAdminRole(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除管理員角色")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<Void> deleteAdminRole(@PathVariable Long id) {
        adminRoleService.deleteAdminRole(id);
        return Result.success(null);
    }
}
