package by.bsuir.kalplat.service;

import by.bsuir.kalplat.domain.model.PaymentRequest;
import by.bsuir.kalplat.domain.model.RequestStatus;
import by.bsuir.kalplat.domain.model.User;
import by.bsuir.kalplat.repository.PaymentRequestRepository;
import by.bsuir.kalplat.repository.UserRepository;
import by.bsuir.kalplat.web.dto.ForecastDto;
import by.bsuir.kalplat.web.dto.RegistryDto;
import by.bsuir.kalplat.web.dto.RequestCreateDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Проверка формирования реестра платежей и прогнозирования ликвидности. */
@SpringBootTest
class RegistryAndForecastTest {

    @Autowired
    private PaymentRequestService requestService;
    @Autowired
    private PaymentRegistryService registryService;
    @Autowired
    private ForecastService forecastService;
    @Autowired
    private PaymentRequestRepository requests;
    @Autowired
    private UserRepository users;

    private PaymentRequest approved(String amount, int daysAhead) {
        User initiator = users.findById(1L).orElseThrow();
        User cfo = users.findById(2L).orElseThrow();
        User treasurer = users.findById(3L).orElseThrow();
        PaymentRequest request = requestService.create(new RequestCreateDto(
                1L, 1L, new BigDecimal(amount), "Оплата по договору",
                LocalDate.now().plusDays(daysAhead)), initiator);
        requestService.submit(request.getId(), initiator);
        requestService.approve(request.getId(), cfo);
        return requestService.approve(request.getId(), treasurer);
    }

    @Test
    @DisplayName("Заявки, не обеспеченные остатком, переносятся на следующий день")
    void registrySplitsByBalance() {
        approved("100000.00", 3);
        approved("200000.00", 5);
        RegistryDto registry = registryService.buildRegistry(
                LocalDate.now().plusDays(7), new BigDecimal("150000.00"), "BY_DUE_DATE");

        assertThat(registry.included()).hasSize(1);
        assertThat(registry.postponed()).hasSize(1);
        assertThat(registry.includedAmount()).isEqualByComparingTo("100000.00");
    }

    @Test
    @DisplayName("Неизвестное правило приоритизации отклоняется")
    void unknownStrategy() {
        assertThatThrownBy(() -> registryService.buildRegistry(
                LocalDate.now(), BigDecimal.TEN, "BY_UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Неизвестное правило");
    }

    @Test
    @DisplayName("Исполнение реестра переводит заявки в статус «Оплачена»")
    void executeMarksPaid() {
        PaymentRequest request = approved("50000.00", 2);
        registryService.execute(LocalDate.now().plusDays(4),
                new BigDecimal("500000.00"), "BY_DUE_DATE");

        assertThat(requests.findById(request.getId()).orElseThrow().getStatus())
                .isEqualTo(RequestStatus.PAID);
    }

    @Test
    @DisplayName("Прогноз выявляет кассовый разрыв при недостатке средств")
    void forecastDetectsGap() {
        approved("400000.00", 2);
        ForecastDto forecast = forecastService.forecast(
                LocalDate.now(), 5, new BigDecimal("100000.00"));

        assertThat(forecast.days()).hasSize(5);
        assertThat(forecast.gapCount()).isGreaterThan(0);
        assertThat(forecast.recommendations()).isNotEmpty();
    }

    @Test
    @DisplayName("Недопустимый горизонт прогнозирования отклоняется")
    void forecastValidatesHorizon() {
        assertThatThrownBy(() -> forecastService.forecast(LocalDate.now(), 0,
                BigDecimal.TEN)).isInstanceOf(IllegalArgumentException.class);
    }
}
