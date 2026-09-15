package gr.aueb.cf.eduapp.service;

import gr.aueb.cf.eduapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.eduapp.core.filters.TeacherFilters;
import gr.aueb.cf.eduapp.dto.PersonalInfoInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.eduapp.dto.TeacherUpdateDTO;
import gr.aueb.cf.eduapp.dto.UserInsertDTO;
import gr.aueb.cf.eduapp.model.Teacher;
import gr.aueb.cf.eduapp.repository.TeacherRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises {@link TeacherService} against a real MySQL instance (Testcontainers) with the
 * actual Spring context, so DB unique constraints, {@code @Transactional} propagation and
 * method-level {@code @PreAuthorize} security are all genuinely enforced (unlike the pure
 * Mockito unit tests in {@link TeacherServiceTest}). Each test runs in its own transaction
 * that is rolled back afterwards, so tests don't need to clean up after themselves.
 */
@SpringBootTest
@Testcontainers
@Transactional
class TeacherServiceIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL_CONTAINER =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));

    @Autowired
    private ITeacherService teacherService;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private static final Long ATTICA_REGION_ID = 2L;
    private static final Long AEGEAN_REGION_ID = 3L;

    @Test
    void saveTeacher_persistsTeacherWithHashedPasswordAndAssignedRoleAndRegion() throws Exception {
        TeacherInsertDTO dto = buildInsertDTO("111111111", "11111111111", "AA111111", "user111", ATTICA_REGION_ID);

        TeacherReadOnlyDTO result = teacherService.saveTeacher(dto);
        entityManager.flush();
        entityManager.clear();

        assertThat(result.vat()).isEqualTo("111111111");
        assertThat(result.region()).isEqualTo("ΑΤΤΙΚΗΣ");

        Teacher persisted = teacherRepository.findByVat("111111111").orElseThrow();
        assertThat(persisted.isDeleted()).isFalse();
        assertThat(persisted.getRegion().getName()).isEqualTo("ΑΤΤΙΚΗΣ");
        assertThat(persisted.getPersonalInfo().getAmka()).isEqualTo("11111111111");
        assertThat(persisted.getUser().getUsername()).isEqualTo("user111");
        assertThat(persisted.getUser().getRole().getName()).isEqualTo("TEACHER");
        assertThat(passwordEncoder.matches("Passw0rd!", persisted.getUser().getPassword())).isTrue();
    }

    @Test
    void saveTeacher_throwsEntityAlreadyExistsException_whenVatAlreadyExistsInDatabase() throws Exception {
        teacherService.saveTeacher(buildInsertDTO("222222222", "22222222222", "AA222222", "user222", ATTICA_REGION_ID));
        entityManager.flush();

        TeacherInsertDTO duplicateVat =
                buildInsertDTO("222222222", "33333333333", "AA333333", "user333", ATTICA_REGION_ID);

        assertThatThrownBy(() -> teacherService.saveTeacher(duplicateVat))
                .isInstanceOf(EntityAlreadyExistsException.class);

        assertThat(teacherRepository.findByVat("222222222")).isPresent();
        assertThat(teacherRepository.findAll()).hasSize(1);
    }

    @Test
    @WithMockUser(authorities = "EDIT_TEACHER")
    void updateTeacher_persistsChangesAndMovesTeacherToNewRegion() throws Exception {
        TeacherReadOnlyDTO saved = teacherService.saveTeacher(
                buildInsertDTO("444444444", "44444444444", "AA444444", "user444", ATTICA_REGION_ID));
        entityManager.flush();
        entityManager.clear();

        UUID uuid = UUID.fromString(saved.uuid());
        TeacherUpdateDTO updateDTO = TeacherUpdateDTO.builder()
                .uuid(uuid)
                .firstname("UpdatedFirst")
                .lastname("UpdatedLast")
                .vat("444444444")
                .regionId(AEGEAN_REGION_ID)
                .userUpdateDTO(UserInsertDTO.builder().username("user444").password("Passw0rd!").roleId(3L).build())
                .personalInfoUpdateDTO(PersonalInfoInsertDTO.builder()
                        .amka("44444444444")
                        .identityNumber("AA444444")
                        .placeOfBirth("Athens")
                        .municipalityOfRegistration("Athens")
                        .build())
                .build();

        teacherService.updateTeacher(updateDTO);
        entityManager.flush();
        entityManager.clear();

        Teacher persisted = teacherRepository.findByUuid(uuid).orElseThrow();
        assertThat(persisted.getFirstname()).isEqualTo("UpdatedFirst");
        assertThat(persisted.getLastname()).isEqualTo("UpdatedLast");
        assertThat(persisted.getRegion().getName()).isEqualTo("ΒΟΡΕΙΟΥ ΑΙΓΑΙΟΥ");
    }

    @Test
    @WithMockUser(authorities = "DELETE_TEACHER")
    void deleteTeacherByUUID_softDeletesTeacherPersonalInfoAndUser() throws Exception {
        TeacherReadOnlyDTO saved = teacherService.saveTeacher(
                buildInsertDTO("555555555", "55555555555", "AA555555", "user555", ATTICA_REGION_ID));
        entityManager.flush();
        entityManager.clear();

        UUID uuid = UUID.fromString(saved.uuid());
        teacherService.deleteTeacherByUUID(uuid);
        entityManager.flush();
        entityManager.clear();

        assertThat(teacherRepository.findByUuidAndDeletedFalse(uuid)).isEmpty();

        Teacher persisted = teacherRepository.findByUuid(uuid).orElseThrow();
        assertThat(persisted.isDeleted()).isTrue();
        assertThat(persisted.getPersonalInfo().isDeleted()).isTrue();
        assertThat(persisted.getUser().isDeleted()).isTrue();
    }

    @Test
    @WithMockUser(authorities = "VIEW_TEACHERS")
    void getTeacherByUUID_throwsAccessDeniedException_whenCallerLacksViewTeacherAuthority() throws Exception {
        TeacherReadOnlyDTO saved = teacherService.saveTeacher(
                buildInsertDTO("666666666", "66666666666", "AA666666", "user666", ATTICA_REGION_ID));
        entityManager.flush();

        UUID uuid = UUID.fromString(saved.uuid());

        assertThatThrownBy(() -> teacherService.getTeacherByUUID(uuid))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = "VIEW_TEACHERS")
    void getTeachersPaginatedFiltered_returnsOnlyMatchingLastname() throws Exception {
        teacherService.saveTeacher(
                buildInsertDTO("777777777", "77777777771", "AA777771", "user7771", ATTICA_REGION_ID, "Papadopoulos"));
        teacherService.saveTeacher(
                buildInsertDTO("777777778", "77777777772", "AA777772", "user7772", ATTICA_REGION_ID, "Ioannou"));
        entityManager.flush();
        entityManager.clear();

        TeacherFilters filters = TeacherFilters.builder().lastname("papado").build();
        Page<TeacherReadOnlyDTO> result =
                teacherService.getTeachersPaginatedFiltered(PageRequest.of(0, 10), filters);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().lastname()).isEqualTo("Papadopoulos");
    }

    private TeacherInsertDTO buildInsertDTO(String vat, String amka, String identityNumber, String username, Long regionId) {
        return buildInsertDTO(vat, amka, identityNumber, username, regionId, "Last");
    }

    private TeacherInsertDTO buildInsertDTO(String vat, String amka, String identityNumber, String username,
                                             Long regionId, String lastname) {
        return TeacherInsertDTO.builder()
                .firstname("First")
                .lastname(lastname)
                .vat(vat)
                .regionId(regionId)
                .userInsertDTO(UserInsertDTO.builder()
                        .username(username)
                        .password("Passw0rd!")
                        .roleId(3L)
                        .build())
                .personalInfoInsertDTO(PersonalInfoInsertDTO.builder()
                        .amka(amka)
                        .identityNumber(identityNumber)
                        .placeOfBirth("Athens")
                        .municipalityOfRegistration("Athens")
                        .build())
                .build();
    }
}
