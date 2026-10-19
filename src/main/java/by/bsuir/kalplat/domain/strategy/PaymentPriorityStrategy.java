package by.bsuir.kalplat.domain.strategy;

import by.bsuir.kalplat.domain.model.PaymentRequest;

import java.util.List;

/**
 * Правило очерёдности включения заявок в реестр платежей.
 * Интерфейс обеспечивает принцип открытости/закрытости: новое правило
 * добавляется отдельной реализацией без изменения сервиса реестра.
 */
public interface PaymentPriorityStrategy {

    /** Код правила, передаваемый в запросе к API. */
    String code();

    /** Возвращает заявки, упорядоченные по убыванию приоритета оплаты. */
    List<PaymentRequest> order(List<PaymentRequest> requests);
}
