package by.bsuir.kalplat.service.impl;

import by.bsuir.kalplat.domain.model.Budget;
import by.bsuir.kalplat.repository.BudgetRepository;
import by.bsuir.kalplat.service.BudgetService;
import by.bsuir.kalplat.web.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Реализация операций контроля лимитов бюджета. */
@Service
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgets;

    public BudgetServiceImpl(BudgetRepository budgets) {
        this.budgets = budgets;
    }

    @Override
    public Budget findBudget(Long cfoId, LocalDate date) {
        String period = Budget.periodOf(date);
        return budgets.findByCfoIdAndPeriod(cfoId, period).orElseThrow(() ->
                new NotFoundException("Бюджет ЦФО на период " + period, cfoId));
    }

    @Override
    public BigDecimal availableLimit(Budget budget) {
        return budget.rest();
    }

    @Override
    @Transactional
    public void reserve(Budget budget, BigDecimal amount) {
        budget.reserve(amount);
        budgets.save(budget);
    }

    @Override
    @Transactional
    public void releaseReserve(Budget budget, BigDecimal amount) {
        budget.releaseReserve(amount);
        budgets.save(budget);
    }

    @Override
    @Transactional
    public void spend(Budget budget, BigDecimal amount) {
        budget.spend(amount);
        budgets.save(budget);
    }
}
