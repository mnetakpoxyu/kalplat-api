package by.bsuir.kalplat.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Договор с контрагентом – основание платежа. */
@Entity
@Table(name = "contract")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Contractor contractor;

    private String number;

    private LocalDate signedAt;

    private int penaltyRate;        // ставка штрафа за просрочку, базисных пунктов

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Contractor getContractor() { return contractor; }
    public void setContractor(Contractor contractor) { this.contractor = contractor; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public LocalDate getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDate signedAt) { this.signedAt = signedAt; }
    public int getPenaltyRate() { return penaltyRate; }
    public void setPenaltyRate(int penaltyRate) { this.penaltyRate = penaltyRate; }
}
