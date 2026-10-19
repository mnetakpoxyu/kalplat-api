package by.bsuir.kalplat.repository;

import by.bsuir.kalplat.domain.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByCfoIdAndPeriod(Long cfoId, String period);
}
