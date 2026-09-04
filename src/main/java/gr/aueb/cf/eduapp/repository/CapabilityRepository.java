package gr.aueb.cf.eduapp.repository;

import gr.aueb.cf.eduapp.model.Capability;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapabilityRepository extends JpaRepository<Capability, Long> {
}
