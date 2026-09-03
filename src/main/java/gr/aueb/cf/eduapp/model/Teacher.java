package gr.aueb.cf.eduapp.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_info_id")
    private PersonalInfo personalInfo;



}
