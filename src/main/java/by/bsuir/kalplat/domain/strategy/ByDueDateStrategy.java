package by.bsuir.kalplat.domain.strategy;

import by.bsuir.kalplat.domain.model.PaymentRequest;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/** Очерёдность по плановой дате оплаты: раньше срок – выше приоритет. */
@Component
public class ByDueDateStrategy implements PaymentPriorityStrategy {

    @Override
    public String code() {
        return "BY_DUE_DATE";
    }

    @Override
    public List<PaymentRequest> order(List<PaymentRequest> requests) {
        return requests.stream()
                .sorted(Comparator.comparing(PaymentRequest::getPlannedDate)
                        .thenComparing(PaymentRequest::getId))
                .toList();
    }
}
