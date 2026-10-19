package by.bsuir.kalplat.config;

import by.bsuir.kalplat.domain.model.Budget;
import by.bsuir.kalplat.domain.model.Cfo;
import by.bsuir.kalplat.domain.model.Contract;
import by.bsuir.kalplat.domain.model.Contractor;
import by.bsuir.kalplat.domain.model.Role;
import by.bsuir.kalplat.domain.model.User;
import by.bsuir.kalplat.repository.BudgetRepository;
import by.bsuir.kalplat.repository.CfoRepository;
import by.bsuir.kalplat.repository.ContractRepository;
import by.bsuir.kalplat.repository.ContractorRepository;
import by.bsuir.kalplat.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Демонстрационные данные для проверки работы программного интерфейса. */
@Configuration
public class DataInitializer {

    @Bean
    public ApplicationRunner seed(CfoRepository cfos, BudgetRepository budgets,
                                  ContractorRepository contractors,
                                  ContractRepository contracts, UserRepository users) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            Cfo production = cfo(cfos, "Производство", 10);
            Cfo logistics = cfo(cfos, "Логистика", 6);

            String period = Budget.periodOf(LocalDate.now().plusMonths(1));
            budget(budgets, production, period, "1500000.00");
            budget(budgets, logistics, period, "600000.00");
            String current = Budget.periodOf(LocalDate.now());
            budget(budgets, production, current, "1200000.00");
            budget(budgets, logistics, current, "450000.00");

            Contractor steel = contractor(contractors, "ОАО «Белсталь»",
                    "190123456", "BY12AKBB30120000000000000001");
            Contractor trans = contractor(contractors, "ООО «ТрансЛогистик»",
                    "190765432", "BY33PJCB30120000000000000002");
            contract(contracts, steel, "ПС-2026/14", 50);
            contract(contracts, trans, "ТЛ-2026/08", 15);

            user(users, "Фурсов А. В.", "initiator@kalplat.by", Role.INITIATOR, production);
            user(users, "Ковалёв И. П.", "cfo@kalplat.by", Role.CFO_MANAGER, production);
            user(users, "Сидорова А. В.", "treasurer@kalplat.by", Role.TREASURER, null);
            user(users, "Громов П. С.", "admin@kalplat.by", Role.ADMIN, null);
        };
    }

    private Cfo cfo(CfoRepository repository, String name, int weight) {
        Cfo cfo = new Cfo();
        cfo.setName(name);
        cfo.setWeight(weight);
        return repository.save(cfo);
    }

    private void budget(BudgetRepository repository, Cfo cfo, String period,
                        String limit) {
        Budget budget = new Budget();
        budget.setCfo(cfo);
        budget.setPeriod(period);
        budget.setLimitAmount(new BigDecimal(limit));
        repository.save(budget);
    }

    private Contractor contractor(ContractorRepository repository, String name,
                                  String inn, String account) {
        Contractor contractor = new Contractor();
        contractor.setName(name);
        contractor.setInn(inn);
        contractor.setBankDetails(account);
        return repository.save(contractor);
    }

    private void contract(ContractRepository repository, Contractor contractor,
                          String number, int penaltyRate) {
        Contract contract = new Contract();
        contract.setContractor(contractor);
        contract.setNumber(number);
        contract.setSignedAt(LocalDate.now().minusMonths(3));
        contract.setPenaltyRate(penaltyRate);
        repository.save(contract);
    }

    private void user(UserRepository repository, String name, String email,
                      Role role, Cfo cfo) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setRole(role);
        user.setCfo(cfo);
        repository.save(user);
    }
}
