package fu.de190270.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import fu.de190270.post.common.ValidationGroups.Create;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;

public record PostDTO(
        Long postId,
        @NotNull(groups = Create.class) @Size(min = 1, max = 100)
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String advertiserName,
        @NotNull(groups = Create.class) @Size(min = 1, max = 100) @Email
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String advertiserEmail,
        @NotNull(groups = Create.class) @Size(min = 1, max = 20)
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String advertiserPhone,
        @NotNull(groups = Create.class)
        @JsonFormat(pattern = "dd/MM/yyyy", timezone = "UTC", lenient = OptBoolean.FALSE) Date startDate,
        @NotNull(groups = Create.class)
        @JsonFormat(pattern = "dd/MM/yyyy", timezone = "UTC", lenient = OptBoolean.FALSE) Date endDate,
        @NotNull(groups = Create.class) @Min(1) Integer numberOfPosts,
        BigDecimal totalCost,
        @Pattern(regexp = "DRAFT|SCHEDULED|PUBLISHED|CANCELLED") String status,
        @NotNull(groups = Create.class) @Min(1) Long userId) {
}
