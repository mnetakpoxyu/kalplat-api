package by.bsuir.kalplat.web.security;

import by.bsuir.kalplat.domain.model.User;
import by.bsuir.kalplat.web.error.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Доступ к пользователю, от имени которого выполняется текущий запрос.
 * Пользователь определяется перехватчиком {@link AuthInterceptor} и
 * сохраняется в атрибутах запроса, поэтому сервисы не обращаются к хранилищу
 * повторно.
 */
@Component
public class CurrentUser {

    public static final String HEADER = "X-User-Id";

    private final HttpServletRequest httpRequest;

    public CurrentUser(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public User get() {
        Object attribute = httpRequest.getAttribute(AuthInterceptor.USER_ATTRIBUTE);
        if (attribute instanceof User user) {
            return user;
        }
        throw new UnauthorizedException("Пользователь не определён");
    }
}
