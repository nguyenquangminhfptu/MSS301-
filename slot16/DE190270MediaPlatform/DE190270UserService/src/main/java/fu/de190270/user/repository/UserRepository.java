package fu.de190270.user.repository;

import fu.de190270.user.entity.Role;
import fu.de190270.user.entity.User;
import fu.de190270.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByUsernameAndUserIdNot(String username, Long userId);

    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (:status is null or u.status = :status)
            """)
    Page<User> search(@Param("role") Role role, @Param("status") UserStatus status, Pageable pageable);
}
