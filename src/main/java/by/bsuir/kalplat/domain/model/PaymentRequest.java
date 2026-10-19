package by.bsuir.kalplat.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Заявка на оплату – центральный объект предметной области.
 * Правила изменения статуса заявки реализованы в самом классе: сервисный слой
 * не изменяет статус напрямую, что соответствует принципу единственной
 * ответственности.
 */
@Entity
@Table(name = "payment_request")
public class PaymentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User author;

    @ManyToOne
    private Budget budget;

    @ManyToOne
    private Contract contract;

    private BigDecimal amount;

    private String purpose;

    private LocalDate plannedDate;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.DRAFT;

    private boolean overLimit;

    private String rejectReason;

    private LocalDateTime createdAt = LocalDateTime.now();

    // ------------------------------------------------- правила предметной области
    public void submit() {
        requireStatus(RequestStatus.DRAFT, "отправить на согласование");
        status = RequestStatus.ON_APPROVAL;
    }

    public void approveByCfo() {
        requireStatus(RequestStatus.ON_APPROVAL, "согласовать");
        status = RequestStatus.CFO_APPROVED;
    }

    public void approveByTreasurer() {
        requireStatus(RequestStatus.CFO_APPROVED, "одобрить");
        status = RequestStatus.APPROVED;
    }

    public void reject(String reason) {
        if (status == RequestStatus.PAID || status == RequestStatus.REJECTED) {
            throw new IllegalStateException(
                    "Заявку в статусе «" + status.getTitle() + "» нельзя отклонить");
        }
        this.rejectReason = reason;
        this.status = RequestStatus.REJECTED;
    }

    public void includeInRegistry() {
        requireStatus(RequestStatus.APPROVED, "включить в реестр платежей");
        status = RequestStatus.IN_REGISTRY;
    }

    public void markPaid() {
        requireStatus(RequestStatus.IN_REGISTRY, "отметить оплаченной");
        status = RequestStatus.PAID;
    }

    public void returnToQueue() {
        requireStatus(RequestStatus.IN_REGISTRY, "вернуть в очередь платежей");
        status = RequestStatus.APPROVED;
    }

    private void requireStatus(RequestStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException("Заявку в статусе «" + status.getTitle()
                    + "» нельзя " + action);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public Budget getBudget() { return budget; }
    public void setBudget(Budget budget) { this.budget = budget; }
    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public LocalDate getPlannedDate() { return plannedDate; }
    public void setPlannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public boolean isOverLimit() { return overLimit; }
    public void setOverLimit(boolean overLimit) { this.overLimit = overLimit; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
