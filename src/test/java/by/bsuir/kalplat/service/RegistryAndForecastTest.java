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

    @Test
    @DisplayName("Пустой реестр формируется без ошибок")
    void emptyRegistry() {
        RegistryDto registry = registryService.buildRegistry(
                LocalDate.now().minusDays(30), new BigDecimal("1000.00"), null);
        assertThat(registry.included()).isEmpty();
        assertThat(registry.includedAmount()).isEqualByComparingTo("0");
        assertThat(registry.strategy()).isEqualTo("BY_DUE_DATE");
    }

    @Test
    @DisplayName("Исполнение пустого реестра отклоняется")
    void executeEmptyRegistry() {
        assertThatThrownBy(() -> registryService.execute(
                LocalDate.now().minusDays(30), new BigDecimal("1000.00"), null))
                .isInstanceOf(by.bsuir.kalplat.web.error.BusinessRuleException.class)
                .hasMessageContaining("отсутствуют заявки");
    }

    @Test
    @DisplayName("Реестр формируется по правилу приоритизации BY_PENALTY")
    void registryWithPenaltyStrategy() {
        approved("30000.00", 6);
        RegistryDto registry = registryService.buildRegistry(
                LocalDate.now().plusDays(8), new BigDecimal("50000.00"), "BY_PENALTY");
        assertThat(registry.strategy()).isEqualTo("BY_PENALTY");
        assertThat(registry.included()).isNotEmpty();
    }

    @Test
    @DisplayName("Реестр формируется по правилу приоритизации BY_CFO_WEIGHT")
    void registryWithCfoWeightStrategy() {
        approved("25000.00", 6);
        RegistryDto registry = registryService.buildRegistry(
                LocalDate.now().plusDays(8), new BigDecimal("50000.00"),
                "BY_CFO_WEIGHT");
        assertThat(registry.strategy()).isEqualTo("BY_CFO_WEIGHT");
    }

    @Test
    @DisplayName("Прогноз без заявок не содержит кассовых разрывов")
    void forecastWithoutGaps() {
        ForecastDto forecast = forecastService.forecast(
                LocalDate.now().plusDays(200), 3, new BigDecimal("1000.00"));
        assertThat(forecast.gapCount()).isZero();
        assertThat(forecast.recommendations()).isEmpty();
        assertThat(forecast.days()).hasSize(3);
    }

    @Test
    @DisplayName("Горизонт прогнозирования свыше 180 дней отклоняется")
    void forecastHorizonUpperBound() {
        assertThatThrownBy(() -> forecastService.forecast(LocalDate.now(), 181,
                BigDecimal.TEN)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1...180");
    }

    @Test
    @DisplayName("Отклонение одобренной заявки освобождает резерв бюджета")
    void rejectApprovedReleasesReserve() {
        PaymentRequest request = approved("40000.00", 4);
        User treasurer = users.findById(3L).orElseThrow();
        BigDecimal reservedBefore = request.getBudget().getReservedAmount();

        requestService.reject(request.getId(), treasurer, "Отменена поставка");

        PaymentRequest stored = requests.findById(request.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(stored.getRejectReason()).isEqualTo("Отменена поставка");
        assertThat(stored.getBudget().getReservedAmount())
                .isLessThanOrEqualTo(reservedBefore);
    }

    @Test
    @DisplayName("Передача чужой заявки на согласование запрещена")
    void submitForeignRequest() {
        User initiator = users.findById(1L).orElseThrow();
        User treasurer = users.findById(3L).orElseThrow();
        PaymentRequest request = requestService.create(new RequestCreateDto(
                1L, 1L, new BigDecimal("1200.00"), "Оплата по договору",
                LocalDate.now().plusDays(3)), initiator);

        assertThatThrownBy(() -> requestService.submit(request.getId(), treasurer))
                .isInstanceOf(by.bsuir.kalplat.web.error.AccessDeniedException.class);
    }

    @Test
    @DisplayName("Согласование администратором запрещено")
    void approveByAdmin() {
        PaymentRequest request = approved("1500.00", 3);
        User admin = users.findById(4L).orElseThrow();
        assertThatThrownBy(() -> requestService.approve(request.getId(), admin))
                .isInstanceOf(by.bsuir.kalplat.web.error.AccessDeniedException.class);
    }

    @Test
    @DisplayName("Заявка на несуществующий бюджет отклоняется")
    void unknownBudget() {
        User initiator = users.findById(1L).orElseThrow();
        assertThatThrownBy(() -> requestService.create(new RequestCreateDto(
                99L, 1L, new BigDecimal("100.00"), "Оплата",
                LocalDate.now().plusDays(3)), initiator))
                .isInstanceOf(by.bsuir.kalplat.web.error.NotFoundException.class);
    }
}
