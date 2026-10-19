package by.bsuir.kalplat.service;

import by.bsuir.kalplat.web.dto.RegistryDto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Формирование реестра платежей и исполнение платёжных поручений. */
public interface PaymentRegistryService {

    RegistryDto buildRegistry(LocalDate date, BigDecimal accountBalance,
                              String strategyCode);

    RegistryDto execute(LocalDate date, BigDecimal accountBalance,
                        String strategyCode);
}
