package RMC_Booking_Engine.rmc.repository;

import RMC_Booking_Engine.rmc.domain.entity.StaffUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {

    Optional<StaffUser> findByEmailIgnoreCase(String email);

    List<StaffUser> findAllByOrderByEmailAsc();
}
