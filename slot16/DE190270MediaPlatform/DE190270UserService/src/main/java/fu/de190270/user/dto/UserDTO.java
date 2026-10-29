package fu.de190270.user.dto;

import fu.de190270.user.common.ValidationGroups.Create;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UserDTO(
        Long userId,
        @NotNull(groups = Create.class) @Size(min = 1, max = 30)
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String username,
        @NotNull(groups = Create.class) @Size(min = 1, max = 100) @Email
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String email,
        @NotNull(groups = Create.class) @Pattern(regexp = "USER|CREATOR|ADMIN") String role,
        @NotNull(groups = Create.class) @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal ratePerPost,
        @NotNull(groups = Create.class) @Min(1) @Max(10) Integer maxDailyPosts,
        @NotNull(groups = Create.class) @Min(1) Integer level,
        @Pattern(regexp = "ACTIVE|SUSPENDED|DEACTIVATED") String status) {
}
