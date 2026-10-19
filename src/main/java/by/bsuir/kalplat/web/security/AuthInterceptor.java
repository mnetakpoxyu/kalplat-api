package by.bsuir.kalplat.web.security;

import by.bsuir.kalplat.domain.model.Role;
import by.bsuir.kalplat.domain.model.User;
import by.bsuir.kalplat.repository.UserRepository;
import by.bsuir.kalplat.web.error.AccessDeniedException;
import by.bsuir.kalplat.web.error.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * Перехватчик запросов, выполняющий идентификацию пользователя и проверку его
 * роли до вызова метода контроллера и до проверки тела запроса.
 * Сквозная задача разграничения доступа отделена от бизнес-логики, что
 * соответствует принципу единственной ответственности.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ATTRIBUTE = "kalplat.currentUser";

    private final UserRepository users;

    public AuthInterceptor(UserRepository users) {
        this.users = users;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        User user = resolveUser(request);
        request.setAttribute(USER_ATTRIBUTE, user);

        RequireRole requirement = method.getMethodAnnotation(RequireRole.class);
        if (requirement == null || requirement.value().length == 0) {
            return true;
        }
        Role[] allowedRoles = requirement.value();
        boolean allowed = Arrays.stream(allowedRoles).anyMatch(user::hasRole);
        if (!allowed) {
            throw new AccessDeniedException(Arrays.stream(allowedRoles)
                    .findFirst().orElseThrow());
        }
        return true;
    }

    private User resolveUser(HttpServletRequest request) {
        String header = request.getHeader(CurrentUser.HEADER);
        if (header == null || header.isBlank()) {
            throw new UnauthorizedException("Не передан заголовок " + CurrentUser.HEADER
                    + " с идентификатором пользователя");
        }
        long id;
        try {
            id = Long.parseLong(header.trim());
        } catch (NumberFormatException exception) {
            throw new UnauthorizedException(
                    "Недопустимое значение заголовка " + CurrentUser.HEADER);
        }
        return users.findById(id).orElseThrow(() ->
                new UnauthorizedException("Пользователь не зарегистрирован в системе"));
    }

    /** Роли, допущенные к операции, для описания в документации. */
    public static String describe(Role... roles) {
        return Arrays.stream(roles).map(Role::getTitle).reduce((a, b) -> a + ", " + b)
                .orElse("любая роль");
    }
}
