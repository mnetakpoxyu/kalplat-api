package by.bsuir.kalplat.domain.strategy;

import by.bsuir.kalplat.domain.model.PaymentRequest;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/** Очерёдность по значимости центра финансовой ответственности. */
@Component
public class ByCfoWeightStrategy implements PaymentPriorityStrategy {

    @Override
    public String code() {
        return "BY_CFO_WEIGHT";
    }

    @Override
    public List<PaymentRequest> order(List<PaymentRequest> requests) {
        return requests.stream()
                .sorted(Comparator.comparingInt(
                                (PaymentRequest request) -> weight(request))
                        .reversed()
                        .thenComparing(PaymentRequest::getPlannedDate))
                .toList();
    }

    private int weight(PaymentRequest request) {
        return request.getBudget() == null || request.getBudget().getCfo() == null
                ? 0 : request.getBudget().getCfo().getWeight();
    }
}
