package tw.edu.ncu.osa.venue_reservation_service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.AdminRole;

import java.util.List;

@Mapper
public interface AdminRoleMapper {
    AdminRole selectActiveByUserId(@Param("userId") String userId);

    AdminRole selectByUserId(@Param("userId") String userId);

    AdminRole selectById(@Param("id") Long id);

    List<AdminRole> selectAll(@Param("includeDeleted") boolean includeDeleted);

    int insertAdminRole(AdminRole adminRole);

    int restoreAndUpdateLevel(
            @Param("id") Long id,
            @Param("level") Integer level
    );

    int updateLevel(
            @Param("id") Long id,
            @Param("level") Integer level
    );

    int softDelete(@Param("id") Long id);

    int countActiveLevelOne();
}
