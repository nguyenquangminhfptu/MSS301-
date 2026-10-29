package fu.de190270.post.dto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record ApiResponse<T>(int status, String message, T data, String timestamp) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(1, message, data, now());
    }

    public static ApiResponse<Void> error(int status, String message) {
        return new ApiResponse<>(status, message, null, now());
    }

    private static String now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
