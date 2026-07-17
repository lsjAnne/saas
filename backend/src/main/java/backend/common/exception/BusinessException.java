package backend.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final String code;
    private final Object data;
    private final HttpStatus httpStatus;

    public BusinessException(String code, String message, HttpStatus httpStatus) {
        this(code, message, null, httpStatus);
    }

    public BusinessException(String code, String message, Object data, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.data = data;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public Object data() {
        return data;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}

