package by.bsuir.kalplat.web.error;

import org.springframework.http.HttpStatus;

/** Запрошенный ресурс не найден. */
public class NotFoundException extends ApiException {

    public NotFoundException(String entity, Object id) {
        super(HttpStatus.NOT_FOUND, "Ресурс не найден: " + entity
                + ", идентификатор " + id);
    }
}
