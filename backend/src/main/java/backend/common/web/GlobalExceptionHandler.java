package backend.common.web;

import backend.common.api.ApiResponse;
import backend.common.exception.BusinessException;
import backend.common.trace.TraceIdHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(exception.httpStatus())
                .body(new ApiResponse<>(
                        exception.code(),
                        exception.getMessage(),
                        exception.data(),
                        TraceIdHolder.get()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldError() == null
                ? "璇锋眰鍙傛暟鏍￠獙澶辫触"
                : exception.getBindingResult().getFieldError().getDefaultMessage();
        return new ApiResponse<>("400", message, null, TraceIdHolder.get());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHandlerMethodValidation(HandlerMethodValidationException exception) {
        String message = exception.getAllErrors().isEmpty()
                ? "request validation failed"
                : exception.getAllErrors().get(0).getDefaultMessage();
        return new ApiResponse<>("400", message, null, TraceIdHolder.get());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return new ApiResponse<>("400", exception.getMessage(), null, TraceIdHolder.get());
    }
}

