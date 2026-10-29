package fu.de190270.user.common;

public class ApiException extends RuntimeException {
    private final int httpStatus;
    private final int status;

    public ApiException(int httpStatus, int status, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.status = status;
    }

    public int getHttpStatus() { return httpStatus; }
    public int getStatus() { return status; }

    public static ApiException invalid(String message) {
        return new ApiException(406, 2, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, 4, message);
    }
}
