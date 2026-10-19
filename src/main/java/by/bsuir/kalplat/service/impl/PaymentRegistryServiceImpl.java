package by.bsuir.kalplat.service.impl;

import by.bsuir.kalplat.domain.model.Payment;
import by.bsuir.kalplat.domain.model.PaymentRequest;
import by.bsuir.kalplat.domain.model.RequestStatus;
import by.bsuir.kalplat.domain.strategy.PaymentPriorityStrategy;
import by.bsuir.kalplat.repository.PaymentRepository;
import by.bsuir.kalplat.repository.PaymentRequestRepository;
import by.bsuir.kalplat.service.BudgetService;
import by.bsuir.kalplat.service.PaymentRegistryService;
import by.bsuir.kalplat.web.dto.RegistryDto;
import by.bsuir.kalplat.web.error.BusinessRuleException;
import by.bsuir.kalplat.web.mapper.PaymentRequestMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Формирование и исполнение реестра платежей.
 * Правило очерёдности включения заявок выбирается во время работы из числа
 * реализаций интерфейса PaymentPriorityStrategy: добавление нового правила не
 * требует изменения данного класса (принцип открытости/закрытости).
 */
@Service
public class PaymentRegistryServiceImpl implements PaymentRegistryService {

    private final PaymentRequestRepository requests;
    private final PaymentRepository payments;
    private final BudgetService budgetService;
    private final PaymentRequestMapper mapper;
    private final Map<String, PaymentPriorityStrategy> strategies;

    public PaymentRegistryServiceImpl(PaymentRequestRepository requests,
                                      PaymentRepository payments,
                                      BudgetService budgetService,
                                      PaymentRequestMapper mapper,
                                      List<PaymentPriorityStrategy> availableStrategies) {
        this.requests = requests;
        this.payments = payments;
        this.budgetService = budgetService;
        this.mapper = mapper;
        this.strategies = availableStrategies.stream()
                .collect(Collectors.toMap(PaymentPriorityStrategy::code,
                        Function.identity()));
    }

    @Override
    public RegistryDto buildRegistry(LocalDate date, BigDecimal balance, String code) {
        PaymentPriorityStrategy strategy = strategy(code);
        List<PaymentRequest> approved =
                requests.findApprovedUntil(RequestStatus.APPROVED, date);

        List<PaymentRequest> included = new ArrayList<>();
        List<PaymentRequest> postponed = new ArrayList<>();
        BigDecimal rest = balance;
        BigDecimal includedAmount = BigDecimal.ZERO;

        for (PaymentRequest request : strategy.order(approved)) {
            if (rest.compareTo(request.getAmount()) >= 0) {
                included.add(request);
                rest = rest.subtract(request.getAmount());
                includedAmount = includedAmount.add(request.getAmount());
            } else {
                postponed.add(request);
            }
        }
        return new RegistryDto(date, strategy.code(), balance, includedAmount,
                mapper.toDtoList(included), mapper.toDtoList(postponed));
    }

    @Override
    @Transactional
    public RegistryDto execute(LocalDate date, BigDecimal balance, String code) {
        RegistryDto registry = buildRegistry(date, balance, code);
        if (registry.included().isEmpty()) {
            throw new BusinessRuleException(
                    "На указанную дату отсутствуют заявки, обеспеченные остатком средств");
        }
        for (var view : registry.included()) {
            PaymentRequest request = requests.findById(view.id()).orElseThrow();
            request.includeInRegistry();
            Payment payment = Payment.of(request, nextOrderNumber());
            payment.markExecuted();
            request.markPaid();
            budgetService.spend(request.getBudget(), request.getAmount());
            payments.save(payment);
            requests.save(request);
        }
        return buildRegistryResult(registry);
    }

    /** Повторное построение представления после исполнения платежей. */
    private RegistryDto buildRegistryResult(RegistryDto registry) {
        List<PaymentRequest> executed = registry.included().stream()
                .map(view -> requests.findById(view.id()).orElseThrow())
                .toList();
        return new RegistryDto(registry.date(), registry.strategy(),
                registry.accountBalance(), registry.includedAmount(),
                mapper.toDtoList(executed), registry.postponed());
    }

    private String nextOrderNumber() {
        return String.format("PP-%06d", payments.count() + 1);
    }

    private PaymentPriorityStrategy strategy(String code) {
        if (code == null || code.isBlank()) {
            return strategies.get("BY_DUE_DATE");
        }
        PaymentPriorityStrategy strategy = strategies.get(code);
        if (strategy == null) {
            throw new IllegalArgumentException("Неизвестное правило приоритизации: "
                    + code + ". Допустимые значения: " + strategies.keySet());
        }
        return strategy;
    }
}
