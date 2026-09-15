package gr.aueb.cf.eduapp.repository;

import gr.aueb.cf.eduapp.model.PersonalInfo;
import gr.aueb.cf.eduapp.model.Region;
import gr.aueb.cf.eduapp.model.Role;
import gr.aueb.cf.eduapp.model.Teacher;
import gr.aueb.cf.eduapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TeacherRepositoryTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL_CONTAINER =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Role role;
    private Region region;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setName("TEACHER_" + UUID.randomUUID());
        entityManager.persistAndFlush(role);

        region = new Region();
        region.setName("Attica_" + UUID.randomUUID());
        entityManager.persistAndFlush(region);
    }

    @Test
    void findByUuid_returnsTeacher_whenExists() {
        Teacher saved = createDummyData("uuid-hit", false);

        Optional<Teacher> found = teacherRepository.findByUuid(saved.getUuid());

        assertThat(found).isPresent();
        assertThat(found.get().getVat()).isEqualTo("vat_uuid-hit");
    }

    @Test
    void findByUuid_returnsEmpty_whenNotExists() {
        Optional<Teacher> found = teacherRepository.findByUuid(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void findByUuidAndDeletedFalse_excludesSoftDeletedTeacher() {
        Teacher deleted = createDummyData("soft-deleted", true);

        Optional<Teacher> found = teacherRepository.findByUuidAndDeletedFalse(deleted.getUuid());

        assertThat(found).isEmpty();
    }

    @Test
    void findByUuidAndDeletedFalse_returnsTeacher_whenNotDeleted() {
        Teacher active = createDummyData("active", false);

        Optional<Teacher> found = teacherRepository.findByUuidAndDeletedFalse(active.getUuid());

        assertThat(found).isPresent();
        assertThat(found.get().getUuid()).isEqualTo(active.getUuid());
    }

    @Test
    void findByVat_returnsTeacher_whenExists() {
        createDummyData("vat-lookup", false);

        Optional<Teacher> found = teacherRepository.findByVat("vat_vat-lookup");

        assertThat(found).isPresent();
        assertThat(found.get().getLastname()).isEqualTo("Last_vat-lookup");
    }

    @Test
    void findByVatAndDeletedFalse_excludesSoftDeletedTeacher() {
        createDummyData("vat-deleted", true);

        Optional<Teacher> found = teacherRepository.findByVatAndDeletedFalse("vat_vat-deleted");

        assertThat(found).isEmpty();
    }

    @Test
    void findByPersonalInfo_Amka_returnsTeacher_whenExists() {
        createDummyData("amka-lookup", false);

        Optional<Teacher> found = teacherRepository.findByPersonalInfo_Amka("amka_amka-lookup");

        assertThat(found).isPresent();
        assertThat(found.get().getPersonalInfo().getAmka()).isEqualTo("amka_amka-lookup");
    }

    @Test
    void findByPersonalInfo_Amka_returnsEmpty_whenNotExists() {
        Optional<Teacher> found = teacherRepository.findByPersonalInfo_Amka("does-not-exist");

        assertThat(found).isEmpty();
    }

    @Test
    void findAllByDeletedFalse_returnsOnlyNonDeletedTeachers() {
        Teacher active = createDummyData("page-active", false);
        createDummyData("page-deleted", true);

        Page<Teacher> page = teacherRepository.findAllByDeletedFalse(PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(Teacher::getUuid)
                .containsExactly(active.getUuid());
        assertThat(page.getContent().getFirst().getPersonalInfo()).isNotNull();
        assertThat(page.getContent().getFirst().getRegion()).isNotNull();
    }

    @Test
    void existsByUuidAndUser_Uuid_returnsTrue_forMatchingPair() {
        Teacher teacher = createDummyData("owner-match", false);

        boolean exists = teacherRepository.existsByUuidAndUser_Uuid(teacher.getUuid(), teacher.getUser().getUuid());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUuidAndUser_Uuid_returnsFalse_forMismatchedPair() {
        Teacher teacher = createDummyData("owner-a", false);
        Teacher other = createDummyData("owner-b", false);

        boolean exists = teacherRepository.existsByUuidAndUser_Uuid(teacher.getUuid(), other.getUser().getUuid());

        assertThat(exists).isFalse();
    }

    private Teacher createDummyData(String suffix, boolean deleted) {
        User user = new User();
        user.setUsername("user_" + suffix);
        user.setPassword("secret");
        role.addUser(user);

        PersonalInfo personalInfo = new PersonalInfo();
        personalInfo.setAmka("amka_" + suffix);
        personalInfo.setIdentityNumber("id_" + suffix);
        personalInfo.setPlaceOfBirth("Athens");
        personalInfo.setMunicipalityOfRegistration("Athens");

        Teacher teacher = new Teacher();
        teacher.setFirstname("First_" + suffix);
        teacher.setLastname("Last_" + suffix);
        teacher.setVat("vat_" + suffix);
        teacher.setPersonalInfo(personalInfo);
        teacher.addUser(user);
        region.addTeacher(teacher);

        if (deleted) {
            teacher.softDelete();
        }

        teacherRepository.save(teacher);
        entityManager.flush();
        entityManager.clear();

        return teacher;
    }
}
