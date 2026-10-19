package by.bsuir.kalplat.web.mapper;

import by.bsuir.kalplat.domain.model.Payment;
import by.bsuir.kalplat.domain.model.PaymentRequest;
import by.bsuir.kalplat.web.dto.PaymentViewDto;
import by.bsuir.kalplat.web.dto.RequestViewDto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Преобразование объектов предметной области в объекты передачи данных.
 * Единственная обязанность класса – отображение модели на представление,
 * поэтому правила преобразования не дублируются в контроллерах и сервисах.
 */
@Component
public class PaymentRequestMapper {

    public RequestViewDto toDto(PaymentRequest request) {
        return new RequestViewDto(
                request.getId(),
                request.getAuthor() == null ? null : request.getAuthor().getFullName(),
                request.getBudget() == null || request.getBudget().getCfo() == null
                        ? null : request.getBudget().getCfo().getName(),
                request.getContract() == null ? null : request.getContract().getNumber(),
                request.getContract() == null || request.getContract().getContractor() == null
                        ? null : request.getContract().getContractor().getName(),
                request.getAmount(),
                request.getPurpose(),
                request.getPlannedDate(),
                request.getStatus().name(),
                request.getStatus().getTitle(),
                request.isOverLimit(),
                request.getRejectReason(),
                request.getCreatedAt());
    }

    public List<RequestViewDto> toDtoList(List<PaymentRequest> requests) {
        return requests.stream().map(this::toDto).toList();
    }

    public PaymentViewDto toDto(Payment payment) {
        return new PaymentViewDto(
                payment.getId(),
                payment.getRequest() == null ? null : payment.getRequest().getId(),
                payment.getOrderNumber(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getStatus().getTitle(),
                payment.getPaidAt());
    }
}
