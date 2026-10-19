package by.bsuir.kalplat.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Лимит бюджета центра финансовой ответственности на период. */
@Entity
@Table(name = "budget")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Cfo cfo;

    private String period;                 // период в формате ГГГГ-ММ

    private BigDecimal limitAmount = BigDecimal.ZERO;

    private BigDecimal reservedAmount = BigDecimal.ZERO;

    private BigDecimal spentAmount = BigDecimal.ZERO;

    /** Остаток лимита: предельная сумма за вычетом резерва и расхода. */
    public BigDecimal rest() {
        return limitAmount.subtract(reservedAmount).subtract(spentAmount);
    }

    public void reserve(BigDecimal amount) {
        reservedAmount = reservedAmount.add(amount);
    }

    public void releaseReserve(BigDecimal amount) {
        reservedAmount = reservedAmount.subtract(amount).max(BigDecimal.ZERO);
    }

    public void spend(BigDecimal amount) {
        releaseReserve(amount);
        spentAmount = spentAmount.add(amount);
    }

    public static String periodOf(java.time.LocalDate date) {
        return YearMonth.from(date).toString();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cfo getCfo() { return cfo; }
    public void setCfo(Cfo cfo) { this.cfo = cfo; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal value) { this.limitAmount = value; }
    public BigDecimal getReservedAmount() { return reservedAmount; }
    public void setReservedAmount(BigDecimal value) { this.reservedAmount = value; }
    public BigDecimal getSpentAmount() { return spentAmount; }
    public void setSpentAmount(BigDecimal value) { this.spentAmount = value; }
}
