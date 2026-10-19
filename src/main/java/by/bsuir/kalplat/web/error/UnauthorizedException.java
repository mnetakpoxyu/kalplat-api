package by.bsuir.kalplat.web.error;

import org.springframework.http.HttpStatus;

/** Пользователь не идентифицирован. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
