package fu.de190270.user.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record PageDTO<T>(int size, int page, int totalPages, long totalElements,
                         boolean first, boolean last, List<T> content) {
    public static <T> PageDTO<T> from(Page<T> result) {
        return new PageDTO<>(result.getSize(), result.getNumber(), result.getTotalPages(),
                result.getTotalElements(), result.isFirst(), result.isLast(), result.getContent());
    }
}
