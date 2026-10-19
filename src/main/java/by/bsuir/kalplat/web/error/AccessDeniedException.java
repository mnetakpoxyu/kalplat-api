package by.bsuir.kalplat.web.error;

import by.bsuir.kalplat.domain.model.Role;
import org.springframework.http.HttpStatus;

/** У пользователя отсутствуют права на выполнение операции. */
public class AccessDeniedException extends ApiException {

    public AccessDeniedException(Role required) {
        super(HttpStatus.FORBIDDEN,
                "Операция доступна только роли «" + required.getTitle() + "»");
    }
}
