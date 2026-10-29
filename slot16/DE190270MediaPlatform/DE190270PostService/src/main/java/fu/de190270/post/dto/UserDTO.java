package fu.de190270.post.dto;

import java.math.BigDecimal;

/** Local representation of the User Service response; no database association across services. */
public record UserDTO(Long userId, String username, String email, String role, BigDecimal ratePerPost,
                      Integer maxDailyPosts, Integer level, String status) {
}
