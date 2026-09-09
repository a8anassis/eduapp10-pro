package gr.aueb.cf.eduapp.specification;

import gr.aueb.cf.eduapp.core.filters.TeacherFilters;
import gr.aueb.cf.eduapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.eduapp.model.Teacher;
import org.springframework.data.jpa.domain.Specification;

public class TeacherSpecification {

    public static Specification<Teacher> build(TeacherFilters filters) {
        return Specification.allOf(
                hasLastname(filters.getLastname()),
                hasRegion(filters.getRegion()),
                isDeleted(filters.isDeleted())
        );
    }

    public static Specification<Teacher> hasLastname(String lastname) {
        return ((root, query, criteriaBuilder) -> lastname == null ? criteriaBuilder.conjunction() :
                criteriaBuilder.like(criteriaBuilder.lower(root.get("lastname")), "%" + lastname.toLowerCase() + "%"));
    }

    public static Specification<Teacher> hasRegion(String region) {
        return ((root, query, criteriaBuilder) -> region == null ? criteriaBuilder.conjunction() :
                criteriaBuilder.like(criteriaBuilder.lower(root.get("region")), "%" + region.toLowerCase() + "%"));
    }

    public static Specification<Teacher> isDeleted(boolean deleted) {   // default is false
        return ((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("deleted"), deleted));
    }

}
