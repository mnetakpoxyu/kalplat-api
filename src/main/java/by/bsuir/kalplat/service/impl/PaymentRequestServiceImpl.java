package by.bsuir.kalplat.service.impl;

import by.bsuir.kalplat.domain.model.Budget;
import by.bsuir.kalplat.domain.model.Contract;
import by.bsuir.kalplat.domain.model.PaymentRequest;
import by.bsuir.kalplat.domain.model.RequestStatus;
import by.bsuir.kalplat.domain.model.Role;
import by.bsuir.kalplat.domain.model.User;
import by.bsuir.kalplat.repository.ContractRepository;
import by.bsuir.kalplat.repository.PaymentRequestRepository;
import by.bsuir.kalplat.service.BudgetService;
import by.bsuir.kalplat.service.PaymentRequestService;
import by.bsuir.kalplat.web.dto.RequestCreateDto;
import by.bsuir.kalplat.web.error.AccessDeniedException;
import by.bsuir.kalplat.web.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Реализация обработки заявок на оплату.
 * Класс зависит от абстракций – интерфейсов репозиториев и сервиса бюджетов,
 * которые передаются через конструктор (внедрение зависимости). Это
 * соответствует принципу инверсии зависимостей и позволяет подменять
 * реализации при тестировании.
 */
@Service
public class PaymentRequestServiceImpl implements PaymentRequestService {

    private final PaymentRequestRepository requests;
    private final ContractRepository contracts;
    private final BudgetService budgetService;

    public PaymentRequestServiceImpl(PaymentRequestRepository requests,
                                     ContractRepository contracts,
                                     BudgetService budgetService) {
        this.requests = requests;
        this.contracts = contracts;
        this.budgetService = budgetService;
    }

    @Override
    @Transactional
    public PaymentRequest create(RequestCreateDto dto, User author) {
        Contract contract = contracts.findById(dto.contractId())
                .orElseThrow(() -> new NotFoundException("Договор", dto.contractId()));
        Budget budget = budgetService.findBudget(dto.cfoId(), dto.plannedDate());

        PaymentRequest request = new PaymentRequest();
        request.setAuthor(author);
        request.setBudget(budget);
        request.setContract(contract);
        request.setAmount(dto.amount());
        request.setPurpose(dto.purpose());
        request.setPlannedDate(dto.plannedDate());
        request.setOverLimit(dto.amount().compareTo(budgetService.availableLimit(budget)) > 0);
        return requests.save(request);
    }

    @Override
    @Transactional
    public PaymentRequest submit(Long id, User author) {
        PaymentRequest request = findById(id);
        if (!request.getAuthor().getId().equals(author.getId())) {
            throw new AccessDeniedException(Role.INITIATOR);
        }
        request.submit();
        return requests.save(request);
    }

    @Override
    @Transactional
    public PaymentRequest approve(Long id, User user) {
        PaymentRequest request = findById(id);
        if (user.hasRole(Role.CFO_MANAGER)) {
            request.approveByCfo();
        } else if (user.hasRole(Role.TREASURER)) {
            request.approveByTreasurer();
            budgetService.reserve(request.getBudget(), request.getAmount());
        } else {
            throw new AccessDeniedException(Role.CFO_MANAGER);
        }
        return requests.save(request);
    }

    @Override
    @Transactional
    public PaymentRequest reject(Long id, User user, String reason) {
        if (!user.hasRole(Role.CFO_MANAGER) && !user.hasRole(Role.TREASURER)) {
            throw new AccessDeniedException(Role.CFO_MANAGER);
        }
        PaymentRequest request = findById(id);
        boolean reserved = request.getStatus() == RequestStatus.APPROVED;
        request.reject(reason);
        if (reserved) {
            budgetService.releaseReserve(request.getBudget(), request.getAmount());
        }
        return requests.save(request);
    }

    @Override
    public PaymentRequest findById(Long id) {
        return requests.findById(id)
                .orElseThrow(() -> new NotFoundException("Заявка на оплату", id));
    }

    @Override
    public List<PaymentRequest> findAll(RequestStatus status) {
        return status == null
                ? requests.findAll()
                : requests.findByStatusOrderByPlannedDate(status);
    }
}
