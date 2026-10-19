package by.bsuir.kalplat.web.error;

import org.springframework.http.HttpStatus;

/** Базовая ошибка API с кодом состояния HTTP. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
