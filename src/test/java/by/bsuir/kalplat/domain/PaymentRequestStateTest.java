package by.bsuir.kalplat.domain;

import by.bsuir.kalplat.domain.model.Budget;
import by.bsuir.kalplat.domain.model.Cfo;
import by.bsuir.kalplat.domain.model.Contract;
import by.bsuir.kalplat.domain.model.Contractor;
import by.bsuir.kalplat.domain.model.Payment;
import by.bsuir.kalplat.domain.model.PaymentRequest;
import by.bsuir.kalplat.domain.model.PaymentStatus;
import by.bsuir.kalplat.domain.model.RequestStatus;
import by.bsuir.kalplat.domain.model.Role;
import by.bsuir.kalplat.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Проверка правил предметной области: переходы статусов и расчёт лимитов. */
class PaymentRequestStateTest {

    private PaymentRequest request(RequestStatus status) {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal("1000.00"));
        request.setPlannedDate(LocalDate.now().plusDays(5));
        request.setStatus(status);
        return request;
    }

    @Test
    @DisplayName("Полный маршрут согласования изменяет статусы последовательно")
    void fullRoute() {
        PaymentRequest request = request(RequestStatus.DRAFT);
        request.submit();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.ON_APPROVAL);
        request.approveByCfo();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CFO_APPROVED);
        request.approveByTreasurer();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
        request.includeInRegistry();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.IN_REGISTRY);
        request.markPaid();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PAID);
    }

    @Test
    @DisplayName("Возврат заявки из реестра в очередь платежей")
    void returnToQueue() {
        PaymentRequest request = request(RequestStatus.IN_REGISTRY);
        request.returnToQueue();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
    }

    @Test
    @DisplayName("Недопустимые переходы статусов отклоняются")
    void invalidTransitions() {
        assertThatThrownBy(() -> request(RequestStatus.APPROVED).submit())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("нельзя отправить на согласование");
        assertThatThrownBy(() -> request(RequestStatus.DRAFT).approveByCfo())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> request(RequestStatus.DRAFT).approveByTreasurer())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> request(RequestStatus.DRAFT).markPaid())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> request(RequestStatus.DRAFT).includeInRegistry())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> request(RequestStatus.DRAFT).returnToQueue())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Оплаченную и отклонённую заявку отклонить нельзя")
    void rejectNotAllowed() {
        assertThatThrownBy(() -> request(RequestStatus.PAID).reject("причина"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> request(RequestStatus.REJECTED).reject("причина"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Отклонение заявки сохраняет причину")
    void rejectKeepsReason() {
        PaymentRequest request = request(RequestStatus.ON_APPROVAL);
        request.reject("Превышен лимит бюджета");
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(request.getRejectReason()).isEqualTo("Превышен лимит бюджета");
    }

    @Test
    @DisplayName("Остаток лимита бюджета уменьшается резервом и расходом")
    void budgetRest() {
        Budget budget = new Budget();
        budget.setLimitAmount(new BigDecimal("1000.00"));
        assertThat(budget.rest()).isEqualByComparingTo("1000.00");
        budget.reserve(new BigDecimal("300.00"));
        assertThat(budget.rest()).isEqualByComparingTo("700.00");
        budget.spend(new BigDecimal("300.00"));
        assertThat(budget.rest()).isEqualByComparingTo("700.00");
        assertThat(budget.getSpentAmount()).isEqualByComparingTo("300.00");
        assertThat(budget.getReservedAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Освобождение резерва не допускает отрицательных значений")
    void releaseReserveNeverNegative() {
        Budget budget = new Budget();
        budget.setLimitAmount(new BigDecimal("500.00"));
        budget.releaseReserve(new BigDecimal("100.00"));
        assertThat(budget.getReservedAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Период бюджета определяется по дате оплаты")
    void periodOfDate() {
        assertThat(Budget.periodOf(LocalDate.of(2026, 11, 5))).isEqualTo("2026-11");
    }

    @Test
    @DisplayName("Платёж формируется по заявке и меняет статусы")
    void paymentLifecycle() {
        PaymentRequest request = request(RequestStatus.IN_REGISTRY);
        Payment payment = Payment.of(request, "PP-000001");
        assertThat(payment.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PREPARED);
        payment.markExecuted();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXECUTED);
        assertThat(payment.getPaidAt()).isNotNull();
        payment.markRejected();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REJECTED);
    }

    @Test
    @DisplayName("Роль пользователя проверяется методом hasRole")
    void userRole() {
        User user = new User();
        user.setRole(Role.TREASURER);
        user.setFullName("Сидорова А. В.");
        user.setEmail("treasurer@kalplat.by");
        assertThat(user.hasRole(Role.TREASURER)).isTrue();
        assertThat(user.hasRole(Role.INITIATOR)).isFalse();
        assertThat(Role.TREASURER.getTitle()).isEqualTo("Казначей");
        assertThat(RequestStatus.PAID.getTitle()).isEqualTo("Оплачена");
        assertThat(PaymentStatus.EXECUTED.getTitle()).isEqualTo("Исполнен");
    }

    @Test
    @DisplayName("Справочные сущности хранят переданные значения")
    void referenceEntities() {
        Cfo cfo = new Cfo();
        cfo.setName("Производство");
        cfo.setWeight(10);
        Contractor contractor = new Contractor();
        contractor.setName("ОАО «Белсталь»");
        contractor.setInn("190123456");
        contractor.setBankDetails("BY12AKBB3012");
        Contract contract = new Contract();
        contract.setContractor(contractor);
        contract.setNumber("ПС-2026/14");
        contract.setSignedAt(LocalDate.of(2026, 7, 1));
        contract.setPenaltyRate(50);

        assertThat(cfo.getName()).isEqualTo("Производство");
        assertThat(cfo.getWeight()).isEqualTo(10);
        assertThat(contract.getContractor().getInn()).isEqualTo("190123456");
        assertThat(contract.getContractor().getBankDetails()).startsWith("BY12");
        assertThat(contract.getNumber()).isEqualTo("ПС-2026/14");
        assertThat(contract.getSignedAt()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(contract.getPenaltyRate()).isEqualTo(50);
    }
}
