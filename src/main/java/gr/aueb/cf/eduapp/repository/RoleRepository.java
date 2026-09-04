package gr.aueb.cf.eduapp.repository;

import gr.aueb.cf.eduapp.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findAllByOrderByNameAsc();
}
