package by.bsuir.kalplat.service;

import by.bsuir.kalplat.domain.model.Budget;
import by.bsuir.kalplat.domain.model.Cfo;
import by.bsuir.kalplat.domain.model.Contract;
import by.bsuir.kalplat.domain.model.Contractor;
import by.bsuir.kalplat.domain.model.Payment;
import by.bsuir.kalplat.domain.model.PaymentRequest;
import by.bsuir.kalplat.domain.model.Role;
import by.bsuir.kalplat.domain.model.User;
import by.bsuir.kalplat.domain.strategy.ByCfoWeightStrategy;
import by.bsuir.kalplat.domain.strategy.ByDueDateStrategy;
import by.bsuir.kalplat.domain.strategy.ByPenaltyStrategy;
import by.bsuir.kalplat.web.dto.PaymentViewDto;
import by.bsuir.kalplat.web.dto.RequestViewDto;
import by.bsuir.kalplat.web.mapper.PaymentRequestMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Проверка правил приоритизации платежей и преобразования объектов. */
class StrategyAndMapperTest {

    private PaymentRequest request(long id, String amount, int daysAhead,
                                   int penaltyRate, int cfoWeight) {
        Cfo cfo = new Cfo();
        cfo.setName("ЦФО-" + id);
        cfo.setWeight(cfoWeight);
        Budget budget = new Budget();
        budget.setCfo(cfo);
        budget.setLimitAmount(new BigDecimal("1000000.00"));
        Contractor contractor = new Contractor();
        contractor.setName("Контрагент-" + id);
        Contract contract = new Contract();
        contract.setContractor(contractor);
        contract.setNumber("Д-" + id);
        contract.setPenaltyRate(penaltyRate);
        User author = new User();
        author.setFullName("Фурсов А. В.");
        author.setRole(Role.INITIATOR);

        PaymentRequest request = new PaymentRequest();
        request.setId(id);
        request.setAuthor(author);
        request.setBudget(budget);
        request.setContract(contract);
        request.setAmount(new BigDecimal(amount));
        request.setPurpose("Оплата по договору Д-" + id);
        request.setPlannedDate(LocalDate.now().plusDays(daysAhead));
        return request;
    }

    @Test
    @DisplayName("Правило BY_DUE_DATE упорядочивает заявки по сроку оплаты")
    void byDueDate() {
        ByDueDateStrategy strategy = new ByDueDateStrategy();
        List<PaymentRequest> ordered = strategy.order(List.of(
                request(1, "100.00", 9, 10, 1),
                request(2, "200.00", 2, 50, 9),
                request(3, "300.00", 5, 30, 5)));
        assertThat(strategy.code()).isEqualTo("BY_DUE_DATE");
        assertThat(ordered).extracting(PaymentRequest::getId)
                .containsExactly(2L, 3L, 1L);
    }

    @Test
    @DisplayName("Правило BY_PENALTY отдаёт приоритет договорам с высоким штрафом")
    void byPenalty() {
        ByPenaltyStrategy strategy = new ByPenaltyStrategy();
        List<PaymentRequest> ordered = strategy.order(List.of(
                request(1, "100.00", 2, 10, 1),
                request(2, "200.00", 9, 70, 2),
                request(3, "300.00", 5, 40, 3)));
        assertThat(strategy.code()).isEqualTo("BY_PENALTY");
        assertThat(ordered).extracting(PaymentRequest::getId)
                .containsExactly(2L, 3L, 1L);
    }

    @Test
    @DisplayName("Правило BY_PENALTY учитывает заявки без договора")
    void byPenaltyWithoutContract() {
        PaymentRequest withoutContract = request(4, "50.00", 1, 0, 0);
        withoutContract.setContract(null);
        List<PaymentRequest> ordered = new ByPenaltyStrategy().order(List.of(
                withoutContract, request(5, "60.00", 8, 25, 1)));
        assertThat(ordered).extracting(PaymentRequest::getId)
                .containsExactly(5L, 4L);
    }

    @Test
    @DisplayName("Правило BY_CFO_WEIGHT упорядочивает заявки по значимости ЦФО")
    void byCfoWeight() {
        ByCfoWeightStrategy strategy = new ByCfoWeightStrategy();
        List<PaymentRequest> ordered = strategy.order(List.of(
                request(1, "100.00", 2, 10, 3),
                request(2, "200.00", 9, 70, 10),
                request(3, "300.00", 5, 40, 6)));
        assertThat(strategy.code()).isEqualTo("BY_CFO_WEIGHT");
        assertThat(ordered).extracting(PaymentRequest::getId)
                .containsExactly(2L, 3L, 1L);
    }

    @Test
    @DisplayName("Правило BY_CFO_WEIGHT учитывает заявки без бюджета")
    void byCfoWeightWithoutBudget() {
        PaymentRequest withoutBudget = request(6, "70.00", 1, 0, 0);
        withoutBudget.setBudget(null);
        List<PaymentRequest> ordered = new ByCfoWeightStrategy().order(List.of(
                withoutBudget, request(7, "80.00", 6, 0, 4)));
        assertThat(ordered).extracting(PaymentRequest::getId)
                .containsExactly(7L, 6L);
    }

    @Test
    @DisplayName("Заявка преобразуется в объект передачи данных полностью")
    void mapRequest() {
        PaymentRequestMapper mapper = new PaymentRequestMapper();
        RequestViewDto dto = mapper.toDto(request(8, "125000.00", 4, 20, 7));

        assertThat(dto.id()).isEqualTo(8L);
        assertThat(dto.author()).isEqualTo("Фурсов А. В.");
        assertThat(dto.cfo()).isEqualTo("ЦФО-8");
        assertThat(dto.contract()).isEqualTo("Д-8");
        assertThat(dto.contractor()).isEqualTo("Контрагент-8");
        assertThat(dto.amount()).isEqualByComparingTo("125000.00");
        assertThat(dto.status()).isEqualTo("DRAFT");
        assertThat(dto.statusTitle()).isEqualTo("Черновик");
        assertThat(dto.overLimit()).isFalse();
    }

    @Test
    @DisplayName("Преобразование заявки без связанных объектов не вызывает ошибок")
    void mapRequestWithoutRelations() {
        PaymentRequest request = new PaymentRequest();
        request.setId(9L);
        request.setAmount(new BigDecimal("10.00"));
        RequestViewDto dto = new PaymentRequestMapper().toDto(request);

        assertThat(dto.author()).isNull();
        assertThat(dto.cfo()).isNull();
        assertThat(dto.contract()).isNull();
        assertThat(dto.contractor()).isNull();
    }

    @Test
    @DisplayName("Список заявок и платёж преобразуются в объекты передачи данных")
    void mapListAndPayment() {
        PaymentRequestMapper mapper = new PaymentRequestMapper();
        List<RequestViewDto> list = mapper.toDtoList(List.of(
                request(10, "10.00", 1, 0, 0), request(11, "20.00", 2, 0, 0)));
        assertThat(list).hasSize(2);

        PaymentRequest request = request(12, "500.00", 3, 0, 0);
        Payment payment = Payment.of(request, "PP-000012");
        payment.markExecuted();
        PaymentViewDto dto = mapper.toDto(payment);

        assertThat(dto.requestId()).isEqualTo(12L);
        assertThat(dto.orderNumber()).isEqualTo("PP-000012");
        assertThat(dto.amount()).isEqualByComparingTo("500.00");
        assertThat(dto.status()).isEqualTo("EXECUTED");
        assertThat(dto.statusTitle()).isEqualTo("Исполнен");
        assertThat(dto.paidAt()).isNotNull();
    }
}
