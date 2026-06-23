package tw.edu.ncu.osa.venue_reservation_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.edu.ncu.osa.venue_reservation_service.mapper.AdminRoleMapper;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.AdminRoleRequestDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.AdminRoleUpdateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.AdminRole;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.AdminRoleVO;
import tw.edu.ncu.osa.venue_reservation_service.service.AdminRoleService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements AdminRoleService {
    private static final int LEVEL_ONE = 1;

    private final AdminRoleMapper adminRoleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AdminRoleVO> listAdminRoles(boolean includeDeleted) {
        log.info("【AdminRoleService】[listAdminRoles] 查詢管理員清單，includeDeleted={}", includeDeleted);
        return adminRoleMapper.selectAll(includeDeleted).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRoleVO createAdminRole(AdminRoleRequestDTO request) {
        String userId = normalizeUserId(request.getUserId());
        Integer level = request.getLevel();
        log.info("【AdminRoleService】[createAdminRole] 建立或恢復管理員，userId={}, level={}", userId, level);

        AdminRole existing = adminRoleMapper.selectByUserId(userId);
        if (existing != null) {
            guardLastLevelOne(existing, level, "不可降級最後一位 level 1 管理員");
            adminRoleMapper.restoreAndUpdateLevel(existing.getId(), level);
            return toVO(adminRoleMapper.selectById(existing.getId()));
        }

        AdminRole adminRole = new AdminRole();
        adminRole.setUserId(userId);
        adminRole.setLevel(level);
        adminRoleMapper.insertAdminRole(adminRole);
        return toVO(adminRoleMapper.selectById(adminRole.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRoleVO updateAdminRole(Long id, AdminRoleUpdateDTO request) {
        AdminRole existing = requireExisting(id);
        if (existing.getDeletedAt() != null) {
            throw new RuntimeException("管理員角色已刪除，請重新建立以恢復");
        }

        guardLastLevelOne(existing, request.getLevel(), "不可降級最後一位 level 1 管理員");
        adminRoleMapper.updateLevel(id, request.getLevel());
        return toVO(adminRoleMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAdminRole(Long id) {
        AdminRole existing = requireExisting(id);
        if (existing.getDeletedAt() != null) {
            return;
        }

        guardLastLevelOne(existing, null, "不可刪除最後一位 level 1 管理員");
        adminRoleMapper.softDelete(id);
    }

    private AdminRole requireExisting(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("管理員角色 ID 不可為空或為負數");
        }

        AdminRole existing = adminRoleMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("管理員角色不存在");
        }
        return existing;
    }

    private void guardLastLevelOne(AdminRole existing, Integer newLevel, String message) {
        boolean activeLevelOne = existing.getDeletedAt() == null && LEVEL_ONE == existing.getLevel();
        boolean remainsLevelOne = newLevel != null && LEVEL_ONE == newLevel;
        if (activeLevelOne && !remainsLevelOne && adminRoleMapper.countActiveLevelOne() <= 1) {
            throw new RuntimeException(message);
        }
    }

    private String normalizeUserId(String userId) {
        String normalized = userId == null ? null : userId.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("管理員 userId 不可為空");
        }
        return normalized;
    }

    private AdminRoleVO toVO(AdminRole adminRole) {
        return new AdminRoleVO(
                adminRole.getId(),
                adminRole.getUserId(),
                adminRole.getLevel(),
                adminRole.getDeletedAt() == null,
                adminRole.getDeletedAt(),
                adminRole.getCreatedAt(),
                adminRole.getUpdatedAt()
        );
    }
}
