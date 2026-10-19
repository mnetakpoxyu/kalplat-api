package by.bsuir.kalplat.repository;

import by.bsuir.kalplat.domain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
