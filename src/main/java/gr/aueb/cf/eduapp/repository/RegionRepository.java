package gr.aueb.cf.eduapp.repository;

import gr.aueb.cf.eduapp.model.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByOrderByNameAsc();
}
