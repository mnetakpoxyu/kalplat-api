package by.bsuir.kalplat.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Платёж, исполняющий заявку на оплату. */
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private PaymentRequest request;

    private String orderNumber;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PREPARED;

    private LocalDateTime paidAt;

    public static Payment of(PaymentRequest request, String orderNumber) {
        Payment payment = new Payment();
        payment.request = request;
        payment.amount = request.getAmount();
        payment.orderNumber = orderNumber;
        return payment;
    }

    public void markExecuted() {
        status = PaymentStatus.EXECUTED;
        paidAt = LocalDateTime.now();
    }

    public void markRejected() {
        status = PaymentStatus.REJECTED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PaymentRequest getRequest() { return request; }
    public void setRequest(PaymentRequest request) { this.request = request; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
