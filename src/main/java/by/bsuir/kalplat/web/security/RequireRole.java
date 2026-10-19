package by.bsuir.kalplat.web.security;

import by.bsuir.kalplat.domain.model.Role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Требование роли для выполнения операции программного интерфейса.
 * Проверка прав вынесена из тела методов контроллеров: обработчик запросов
 * описывает только требуемую роль, а проверку выполняет перехватчик.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** Роли, которым доступна операция; пустое значение – любая роль. */
    Role[] value() default {};
}
