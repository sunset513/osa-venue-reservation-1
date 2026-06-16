package tw.edu.ncu.osa.venue_reservation_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tw.edu.ncu.osa.venue_reservation_service.common.result.Result;
import tw.edu.ncu.osa.venue_reservation_service.model.entity.User;
import tw.edu.ncu.osa.venue_reservation_service.model.vo.CurrentUserVO;
import tw.edu.ncu.osa.venue_reservation_service.util.UserContext;

@RestController
@RequestMapping("/api")
@Tag(name = "登入狀態", description = "提供目前登入者資訊")
public class AuthController {
    private static final String ROLE_ADMIN = "ADMIN";

    @GetMapping("/me")
    @Operation(summary = "取得目前登入者資訊")
    @SecurityRequirement(name = "Session-Cookie")
    public Result<CurrentUserVO> getCurrentUser() {
        User user = UserContext.getUser();
        boolean isReviewer = user != null && ROLE_ADMIN.equals(user.getRole());
        String defaultRoute = isReviewer ? "/review" : "/consent-agreement";

        return Result.success(new CurrentUserVO(
                user == null ? null : user.getUserId(),
                user == null ? null : user.getName(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getRole(),
                isReviewer,
                defaultRoute
        ));
    }
}
