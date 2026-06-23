package tw.edu.ncu.osa.venue_reservation_service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    tw.edu.ncu.osa.venue_reservation_service.model.entity.User selectByUserId(
            @Param("userId") String userId
    );

    int insertPortalUser(
            @Param("userId") String userId,
            @Param("name") String name,
            @Param("email") String email,
            @Param("role") String role
    );

    int updateLoginAt(@Param("userId") String userId);
}
