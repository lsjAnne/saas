package backend.common.api;

public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String traceId
) {

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>("200", "success", data, traceId);
    }
}

