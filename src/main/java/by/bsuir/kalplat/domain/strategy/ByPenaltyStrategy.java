package by.bsuir.kalplat.domain.strategy;

import by.bsuir.kalplat.domain.model.PaymentRequest;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/** Очерёдность по размеру штрафных санкций договора за просрочку платежа. */
@Component
public class ByPenaltyStrategy implements PaymentPriorityStrategy {

    @Override
    public String code() {
        return "BY_PENALTY";
    }

    @Override
    public List<PaymentRequest> order(List<PaymentRequest> requests) {
        return requests.stream()
                .sorted(Comparator.comparingInt(
                                (PaymentRequest request) -> penaltyRate(request))
                        .reversed()
                        .thenComparing(PaymentRequest::getPlannedDate))
                .toList();
    }

    private int penaltyRate(PaymentRequest request) {
        return request.getContract() == null ? 0 : request.getContract().getPenaltyRate();
    }
}
