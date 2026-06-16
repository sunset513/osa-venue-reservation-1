package tw.edu.ncu.osa.venue_reservation_service.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserVO {
    private String identifier;
    private String chineseName;
    private String email;
    private String role;
    private Boolean isReviewer;
    private String defaultRoute;
}
