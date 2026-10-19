package by.bsuir.kalplat.repository;

import by.bsuir.kalplat.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
