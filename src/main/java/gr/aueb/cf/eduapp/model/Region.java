package gr.aueb.cf.eduapp.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "regions")
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Setter(AccessLevel.PRIVATE)
    @Getter(AccessLevel.PACKAGE)
    @OneToMany(mappedBy = "region", fetch = FetchType.LAZY)
    private Set<Teacher> teachers = new HashSet<>();

    public Set<Teacher> getAllTeachers() {
        return Collections.unmodifiableSet(teachers);
    }

    // Helper methods
    public void addTeacher(Teacher teacher) {
        teachers.add(teacher);
        teacher.setRegion(this);
    }

    public void removeTeacher(Teacher teacher) {
        teachers.remove(teacher);
        teacher.setRegion(null);
    }
}
