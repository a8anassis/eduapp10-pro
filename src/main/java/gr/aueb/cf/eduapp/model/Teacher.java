package gr.aueb.cf.eduapp.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "teachers")
public class Teacher extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID uuid = UUID.randomUUID();

    @Column(unique = true, nullable = false)
    private String vat;

    @Column(nullable = false)
    private String firstname;

    @Column(nullable = false)
    private String lastname;

    @Setter(AccessLevel.PACKAGE)
    @OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_info_id")
    private PersonalInfo personalInfo;

    public void addUser(User user) {
        this.user = user;
        user.setTeacher(this);
    }

    public void removeUser(User user) {
        this.user = null;
        user.setTeacher(null);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Teacher teacher)) return false;
        return Objects.equals(getVat(), teacher.getVat());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getVat());
    }
}
