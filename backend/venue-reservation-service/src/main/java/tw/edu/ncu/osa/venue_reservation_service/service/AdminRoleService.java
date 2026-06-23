package tw.edu.ncu.osa.venue_reservation_service.service;

import tw.edu.ncu.osa.venue_reservation_service.model.dto.AdminRoleRequestDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.dto.AdminRoleUpdateDTO;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.AdminRoleVO;

import java.util.List;

public interface AdminRoleService {
    List<AdminRoleVO> listAdminRoles(boolean includeDeleted);

    AdminRoleVO createAdminRole(AdminRoleRequestDTO request);

    AdminRoleVO updateAdminRole(Long id, AdminRoleUpdateDTO request);

    void deleteAdminRole(Long id);
}
