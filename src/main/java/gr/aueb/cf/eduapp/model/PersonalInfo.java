package gr.aueb.cf.eduapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "personal_information")
public class PersonalInfo extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String amka;

    @Column(name = "identity_number", unique = true, nullable = false)
    private String identityNumber;

    @Column(name = "place_of_birth", nullable = false)
    private String placeOfBirth;

    @Column(name = "municipality_of_registration", nullable = false)
    private String municipalityOfRegistration;

    @Setter(AccessLevel.PACKAGE)
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "amka_file_id")
    private Attachment amkaFile;

    public void addAmkaFile(Attachment amkaFile) {
        this.amkaFile = amkaFile;
    }

    public void removeAmkaFile(Attachment amkaFile) {
        this.amkaFile = null;
    }
}
