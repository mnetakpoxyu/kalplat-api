package by.bsuir.kalplat.web.error;

import org.springframework.http.HttpStatus;

/** Нарушено правило предметной области. */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
