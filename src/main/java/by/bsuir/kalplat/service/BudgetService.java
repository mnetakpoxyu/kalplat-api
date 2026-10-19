package by.bsuir.kalplat.service;

import by.bsuir.kalplat.domain.model.Budget;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Операции контроля лимитов бюджета центра финансовой ответственности. */
public interface BudgetService {

    Budget findBudget(Long cfoId, LocalDate date);

    BigDecimal availableLimit(Budget budget);

    void reserve(Budget budget, BigDecimal amount);

    void releaseReserve(Budget budget, BigDecimal amount);

    void spend(Budget budget, BigDecimal amount);
}
