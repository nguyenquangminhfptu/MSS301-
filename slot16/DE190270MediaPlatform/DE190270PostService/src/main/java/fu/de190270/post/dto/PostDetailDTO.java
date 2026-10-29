package fu.de190270.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.Date;

public record PostDetailDTO(Long postId, String advertiserName, String advertiserEmail, String advertiserPhone,
        @JsonFormat(pattern = "dd/MM/yyyy", timezone = "UTC") Date startDate,
        @JsonFormat(pattern = "dd/MM/yyyy", timezone = "UTC") Date endDate,
        Integer numberOfPosts, BigDecimal totalCost, String status, UserDTO user) {
}
