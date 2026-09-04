package gr.aueb.cf.eduapp.repository;

import gr.aueb.cf.eduapp.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {

    Optional<User> findByUuid(UUID uuid);
    Optional<User> findByUuidAndDeletedFalse(UUID uuid);

    @EntityGraph(attributePaths = {"role", "role.capabilities"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"role", "role.capabilities"})
    Optional<User> findByUsernameAndDeletedFalse(String username);


}
