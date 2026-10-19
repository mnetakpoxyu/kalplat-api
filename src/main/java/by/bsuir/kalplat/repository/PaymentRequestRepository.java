package by.bsuir.kalplat.repository;

import by.bsuir.kalplat.domain.model.PaymentRequest;
import by.bsuir.kalplat.domain.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {

    List<PaymentRequest> findByStatusOrderByPlannedDate(RequestStatus status);

    @Query("select r from PaymentRequest r where r.status = :status "
            + "and r.plannedDate <= :date order by r.plannedDate")
    List<PaymentRequest> findApprovedUntil(RequestStatus status, LocalDate date);

    @Query("select coalesce(sum(r.amount), 0) from PaymentRequest r "
            + "where r.plannedDate = :date and r.status in "
            + "(by.bsuir.kalplat.domain.model.RequestStatus.APPROVED, "
            + " by.bsuir.kalplat.domain.model.RequestStatus.IN_REGISTRY)")
    java.math.BigDecimal plannedOutflow(LocalDate date);
}
