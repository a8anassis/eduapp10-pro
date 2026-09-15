package gr.aueb.cf.eduapp.service;

import gr.aueb.cf.eduapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.eduapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.eduapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.eduapp.core.filters.TeacherFilters;
import gr.aueb.cf.eduapp.dto.PersonalInfoInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.eduapp.dto.TeacherUpdateDTO;
import gr.aueb.cf.eduapp.dto.UserInsertDTO;
import gr.aueb.cf.eduapp.mapper.Mapper;
import gr.aueb.cf.eduapp.model.PersonalInfo;
import gr.aueb.cf.eduapp.model.Region;
import gr.aueb.cf.eduapp.model.Role;
import gr.aueb.cf.eduapp.model.Teacher;
import gr.aueb.cf.eduapp.model.User;
import gr.aueb.cf.eduapp.repository.PersonalInfoRepository;
import gr.aueb.cf.eduapp.repository.RegionRepository;
import gr.aueb.cf.eduapp.repository.RoleRepository;
import gr.aueb.cf.eduapp.repository.TeacherRepository;
import gr.aueb.cf.eduapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PersonalInfoRepository personalInfoRepository;

    @Mock
    private Mapper mapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TeacherService teacherService;

    private Region region;
    private Role role;
    private TeacherReadOnlyDTO expectedReadOnlyDTO;

    @BeforeEach
    void setUp() {
        region = new Region();
        region.setId(1L);
        region.setName("Attica");

        role = new Role();
        role.setId(3L);
        role.setName("TEACHER");

        expectedReadOnlyDTO = new TeacherReadOnlyDTO(
                UUID.randomUUID().toString(), "First", "Last", "123456789", "Attica");
    }

    // ---------- saveTeacher ----------

    @Test
    void saveTeacher_savesSuccessfully_whenDataIsValid() throws Exception {
        TeacherInsertDTO dto = buildInsertDTO("123456789", "12345678901", "AB123456", "jsmith");

        Teacher mappedTeacher = new Teacher();
        mappedTeacher.setFirstname(dto.firstname());
        mappedTeacher.setLastname(dto.lastname());
        mappedTeacher.setVat(dto.vat());
        User user = new User();
        user.setUsername(dto.userInsertDTO().username());
        user.setPassword(dto.userInsertDTO().password());
        mappedTeacher.addUser(user);
        mappedTeacher.setPersonalInfo(new PersonalInfo());

        when(teacherRepository.findByVat(dto.vat())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByAmka(dto.personalInfoInsertDTO().amka())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByIdentityNumber(dto.personalInfoInsertDTO().identityNumber()))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsername(dto.userInsertDTO().username())).thenReturn(Optional.empty());
        when(regionRepository.findById(dto.regionId())).thenReturn(Optional.of(region));
        when(roleRepository.findById(3L)).thenReturn(Optional.of(role));
        when(mapper.mapToTeacherEntity(dto)).thenReturn(mappedTeacher);
        when(passwordEncoder.encode(dto.userInsertDTO().password())).thenReturn("hashed-password");
        when(mapper.mapToTeacherReadonlyDTO(mappedTeacher)).thenReturn(expectedReadOnlyDTO);

        TeacherReadOnlyDTO result = teacherService.saveTeacher(dto);

        assertThat(result).isEqualTo(expectedReadOnlyDTO);
        assertThat(mappedTeacher.getRegion()).isEqualTo(region);
        assertThat(mappedTeacher.getUser().getRole()).isEqualTo(role);
        assertThat(mappedTeacher.getUser().getPassword()).isEqualTo("hashed-password");
        verify(teacherRepository).save(mappedTeacher);
    }

    @Test
    void saveTeacher_throwsEntityAlreadyExistsException_whenVatAlreadyExists() {
        TeacherInsertDTO dto = buildInsertDTO("123456789", "12345678901", "AB123456", "jsmith");
        when(teacherRepository.findByVat(dto.vat())).thenReturn(Optional.of(new Teacher()));

        assertThatThrownBy(() -> teacherService.saveTeacher(dto))
                .isInstanceOf(EntityAlreadyExistsException.class);

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void saveTeacher_throwsEntityAlreadyExistsException_whenAmkaAlreadyExists() {
        TeacherInsertDTO dto = buildInsertDTO("123456789", "12345678901", "AB123456", "jsmith");
        when(teacherRepository.findByVat(dto.vat())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByAmka(dto.personalInfoInsertDTO().amka()))
                .thenReturn(Optional.of(new PersonalInfo()));

        assertThatThrownBy(() -> teacherService.saveTeacher(dto))
                .isInstanceOf(EntityAlreadyExistsException.class);

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void saveTeacher_throwsEntityAlreadyExistsException_whenIdentityNumberAlreadyExists() {
        TeacherInsertDTO dto = buildInsertDTO("123456789", "12345678901", "AB123456", "jsmith");
        when(teacherRepository.findByVat(dto.vat())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByAmka(dto.personalInfoInsertDTO().amka())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByIdentityNumber(dto.personalInfoInsertDTO().identityNumber()))
                .thenReturn(Optional.of(new PersonalInfo()));

        assertThatThrownBy(() -> teacherService.saveTeacher(dto))
                .isInstanceOf(EntityAlreadyExistsException.class);

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void saveTeacher_throwsEntityAlreadyExistsException_whenUsernameAlreadyExists() {
        TeacherInsertDTO dto = buildInsertDTO("123456789", "12345678901", "AB123456", "jsmith");
        when(teacherRepository.findByVat(dto.vat())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByAmka(dto.personalInfoInsertDTO().amka())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByIdentityNumber(dto.personalInfoInsertDTO().identityNumber()))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsername(dto.userInsertDTO().username()))
                .thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> teacherService.saveTeacher(dto))
                .isInstanceOf(EntityAlreadyExistsException.class);

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void saveTeacher_throwsEntityInvalidArgumentException_whenRegionDoesNotExist() {
        TeacherInsertDTO dto = buildInsertDTO("123456789", "12345678901", "AB123456", "jsmith");
        when(teacherRepository.findByVat(dto.vat())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByAmka(dto.personalInfoInsertDTO().amka())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByIdentityNumber(dto.personalInfoInsertDTO().identityNumber()))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsername(dto.userInsertDTO().username())).thenReturn(Optional.empty());
        when(regionRepository.findById(dto.regionId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.saveTeacher(dto))
                .isInstanceOf(EntityInvalidArgumentException.class);

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void saveTeacher_throwsEntityInvalidArgumentException_whenRoleDoesNotExist() {
        TeacherInsertDTO dto = buildInsertDTO("123456789", "12345678901", "AB123456", "jsmith");
        when(teacherRepository.findByVat(dto.vat())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByAmka(dto.personalInfoInsertDTO().amka())).thenReturn(Optional.empty());
        when(personalInfoRepository.findByIdentityNumber(dto.personalInfoInsertDTO().identityNumber()))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsername(dto.userInsertDTO().username())).thenReturn(Optional.empty());
        when(regionRepository.findById(dto.regionId())).thenReturn(Optional.of(region));
        when(roleRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.saveTeacher(dto))
                .isInstanceOf(EntityInvalidArgumentException.class);

        verify(teacherRepository, never()).save(any());
    }

    // ---------- updateTeacher ----------

    @Test
    void updateTeacher_updatesSuccessfully_whenNothingUniqueChanges() throws Exception {
        Teacher existing = buildExistingTeacher("123456789", "AB123456", "jsmith");
        TeacherUpdateDTO dto = buildUpdateDTO(existing.getUuid(), "123456789", "AB123456", "jsmith", 1L);

        when(teacherRepository.findByUuid(dto.uuid())).thenReturn(Optional.of(existing));
        when(mapper.mapToTeacherReadonlyDTO(existing)).thenReturn(expectedReadOnlyDTO);

        TeacherReadOnlyDTO result = teacherService.updateTeacher(dto);

        assertThat(result).isEqualTo(expectedReadOnlyDTO);
        assertThat(existing.getFirstname()).isEqualTo(dto.firstname());
        assertThat(existing.getLastname()).isEqualTo(dto.lastname());
        verify(teacherRepository, never()).findByVat(anyString());
        verify(personalInfoRepository, never()).findByIdentityNumber(anyString());
        verify(userRepository, never()).findByUsername(anyString());

        verify(teacherRepository).save(existing);
    }

    @Test
    void updateTeacher_throwsEntityNotFoundException_whenTeacherDoesNotExist() {
        UUID uuid = UUID.randomUUID();
        TeacherUpdateDTO dto = buildUpdateDTO(uuid, "123456789", "AB123456", "jsmith", 1L);
        when(teacherRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.updateTeacher(dto))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateTeacher_throwsEntityAlreadyExistsException_whenNewVatAlreadyExists() {
        Teacher existing = buildExistingTeacher("123456789", "AB123456", "jsmith");
        TeacherUpdateDTO dto = buildUpdateDTO(existing.getUuid(), "999999999", "AB123456", "jsmith", 1L);

        when(teacherRepository.findByUuid(dto.uuid())).thenReturn(Optional.of(existing));
        when(teacherRepository.findByVat(dto.vat())).thenReturn(Optional.of(new Teacher()));

        assertThatThrownBy(() -> teacherService.updateTeacher(dto))
                .isInstanceOf(EntityAlreadyExistsException.class);

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateTeacher_throwsEntityAlreadyExistsException_whenNewIdentityNumberAlreadyExists() {
        Teacher existing = buildExistingTeacher("123456789", "AB123456", "jsmith");
        TeacherUpdateDTO dto = buildUpdateDTO(existing.getUuid(), "123456789", "CD999999", "jsmith", 1L);

        when(teacherRepository.findByUuid(dto.uuid())).thenReturn(Optional.of(existing));
        when(personalInfoRepository.findByIdentityNumber(dto.personalInfoUpdateDTO().identityNumber()))
                .thenReturn(Optional.of(new PersonalInfo()));

        assertThatThrownBy(() -> teacherService.updateTeacher(dto))
                .isInstanceOf(EntityAlreadyExistsException.class);

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateTeacher_throwsEntityInvalidArgumentException_whenNewRegionDoesNotExist() {
        Teacher existing = buildExistingTeacher("123456789", "AB123456", "jsmith");
        TeacherUpdateDTO dto = buildUpdateDTO(existing.getUuid(), "123456789", "AB123456", "jsmith", 2L);

        when(teacherRepository.findByUuid(dto.uuid())).thenReturn(Optional.of(existing));
        when(regionRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.updateTeacher(dto))
                .isInstanceOf(EntityInvalidArgumentException.class);

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateTeacher_movesTeacherToNewRegion_whenRegionChanges() throws Exception {
        Teacher existing = buildExistingTeacher("123456789", "AB123456", "jsmith");
        Region newRegion = new Region();
        newRegion.setId(2L);
        newRegion.setName("Thessaly");
        TeacherUpdateDTO dto = buildUpdateDTO(existing.getUuid(), "123456789", "AB123456", "jsmith", 2L);

        when(teacherRepository.findByUuid(dto.uuid())).thenReturn(Optional.of(existing));
        when(regionRepository.findById(2L)).thenReturn(Optional.of(newRegion));
        when(mapper.mapToTeacherReadonlyDTO(existing)).thenReturn(expectedReadOnlyDTO);

        teacherService.updateTeacher(dto);

        assertThat(existing.getRegion()).isEqualTo(newRegion);
        assertThat(region.getAllTeachers()).doesNotContain(existing);
        assertThat(newRegion.getAllTeachers()).contains(existing);
    }

    @Test
    void updateTeacher_throwsEntityAlreadyExistsException_whenNewUsernameAlreadyExists() {
        Teacher existing = buildExistingTeacher("123456789", "AB123456", "jsmith");
        TeacherUpdateDTO dto = buildUpdateDTO(existing.getUuid(), "123456789", "AB123456", "newusername", 1L);

        when(teacherRepository.findByUuid(dto.uuid())).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("newusername")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> teacherService.updateTeacher(dto))
                .isInstanceOf(EntityAlreadyExistsException.class);

        verify(teacherRepository, never()).save(any());
    }

    // ---------- deleteTeacherByUUID ----------

    @Test
    void deleteTeacherByUUID_softDeletesTeacherPersonalInfoAndUser() throws Exception {
        Teacher existing = buildExistingTeacher("123456789", "AB123456", "jsmith");
        when(teacherRepository.findByUuidAndDeletedFalse(existing.getUuid())).thenReturn(Optional.of(existing));
        when(mapper.mapToTeacherReadonlyDTO(existing)).thenReturn(expectedReadOnlyDTO);

        TeacherReadOnlyDTO result = teacherService.deleteTeacherByUUID(existing.getUuid());

        assertThat(result).isEqualTo(expectedReadOnlyDTO);
        assertThat(existing.isDeleted()).isTrue();
        assertThat(existing.getPersonalInfo().isDeleted()).isTrue();
        assertThat(existing.getUser().isDeleted()).isTrue();
    }

    @Test
    void deleteTeacherByUUID_throwsEntityNotFoundException_whenTeacherDoesNotExist() {
        UUID uuid = UUID.randomUUID();
        when(teacherRepository.findByUuidAndDeletedFalse(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.deleteTeacherByUUID(uuid))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- getTeacherByUUID ----------

    @Test
    void getTeacherByUUID_returnsTeacher_whenExists() throws Exception {
        Teacher existing = buildExistingTeacher("123456789", "AB123456", "jsmith");
        when(teacherRepository.findByUuid(existing.getUuid())).thenReturn(Optional.of(existing));
        when(mapper.mapToTeacherReadonlyDTO(existing)).thenReturn(expectedReadOnlyDTO);

        TeacherReadOnlyDTO result = teacherService.getTeacherByUUID(existing.getUuid());

        assertThat(result).isEqualTo(expectedReadOnlyDTO);
    }

    @Test
    void getTeacherByUUID_throwsEntityNotFoundException_whenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(teacherRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.getTeacherByUUID(uuid))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- getTeacherByUUIDDeletedFalse ----------

    @Test
    void getTeacherByUUIDDeletedFalse_returnsTeacher_whenExistsAndNotDeleted() throws Exception {
        Teacher existing = buildExistingTeacher("123456789", "AB123456", "jsmith");
        when(teacherRepository.findByUuidAndDeletedFalse(existing.getUuid())).thenReturn(Optional.of(existing));
        when(mapper.mapToTeacherReadonlyDTO(existing)).thenReturn(expectedReadOnlyDTO);

        TeacherReadOnlyDTO result = teacherService.getTeacherByUUIDDeletedFalse(existing.getUuid());

        assertThat(result).isEqualTo(expectedReadOnlyDTO);
    }

    @Test
    void getTeacherByUUIDDeletedFalse_throwsEntityNotFoundException_whenNotFoundOrDeleted() {
        UUID uuid = UUID.randomUUID();
        when(teacherRepository.findByUuidAndDeletedFalse(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.getTeacherByUUIDDeletedFalse(uuid))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- getPaginatedTeachers / getPaginatedTeachersDeletedFalse ----------

    @Test
    void getPaginatedTeachers_mapsRepositoryPageToDTOPage() {
        Teacher existing = buildExistingTeacher("123456789", "AB123456", "jsmith");
        Pageable pageable = PageRequest.of(0, 10);
        Page<Teacher> teacherPage = new PageImpl<>(List.of(existing), pageable, 1);

        when(teacherRepository.findAll(pageable)).thenReturn(teacherPage);
        when(mapper.mapToTeacherReadonlyDTO(existing)).thenReturn(expectedReadOnlyDTO);

        Page<TeacherReadOnlyDTO> result = teacherService.getPaginatedTeachers(pageable);

        assertThat(result.getContent()).containsExactly(expectedReadOnlyDTO);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getPaginatedTeachersDeletedFalse_mapsRepositoryPageToDTOPage() {
        Teacher existing = buildExistingTeacher("123456789", "AB123456", "jsmith");
        Pageable pageable = PageRequest.of(0, 10);
        Page<Teacher> teacherPage = new PageImpl<>(List.of(existing), pageable, 1);

        when(teacherRepository.findAllByDeletedFalse(pageable)).thenReturn(teacherPage);
        when(mapper.mapToTeacherReadonlyDTO(existing)).thenReturn(expectedReadOnlyDTO);

        Page<TeacherReadOnlyDTO> result = teacherService.getPaginatedTeachersDeletedFalse(pageable);

        assertThat(result.getContent()).containsExactly(expectedReadOnlyDTO);
    }

    // ---------- getTeachersPaginatedFiltered ----------

    @Test
    void getTeachersPaginatedFiltered_returnsSingleResultPage_whenFilteringByUuid() throws Exception {
        Teacher existing = buildExistingTeacher("123456789", "AB123456", "jsmith");
        Pageable pageable = PageRequest.of(0, 10);
        TeacherFilters filters = TeacherFilters.builder().uuid(existing.getUuid()).build();

        when(teacherRepository.findByUuidAndDeletedFalse(existing.getUuid())).thenReturn(Optional.of(existing));
        when(mapper.mapToTeacherReadonlyDTO(existing)).thenReturn(expectedReadOnlyDTO);

        Page<TeacherReadOnlyDTO> result = teacherService.getTeachersPaginatedFiltered(pageable, filters);

        assertThat(result.getContent()).containsExactly(expectedReadOnlyDTO);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(teacherRepository, never()).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getTeachersPaginatedFiltered_throwsEntityNotFoundException_whenUuidFilterHasNoMatch() {
        Pageable pageable = PageRequest.of(0, 10);
        UUID uuid = UUID.randomUUID();
        TeacherFilters filters = TeacherFilters.builder().uuid(uuid).build();

        when(teacherRepository.findByUuidAndDeletedFalse(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.getTeachersPaginatedFiltered(pageable, filters))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getTeachersPaginatedFiltered_returnsSingleResultPage_whenFilteringByAmka() throws Exception {
        Teacher existing = buildExistingTeacher("123456789", "AB123456", "jsmith");
        Pageable pageable = PageRequest.of(0, 10);
        TeacherFilters filters = TeacherFilters.builder().amka("12345678901").build();

        when(teacherRepository.findByPersonalInfo_Amka("12345678901")).thenReturn(Optional.of(existing));
        when(mapper.mapToTeacherReadonlyDTO(existing)).thenReturn(expectedReadOnlyDTO);

        Page<TeacherReadOnlyDTO> result = teacherService.getTeachersPaginatedFiltered(pageable, filters);

        assertThat(result.getContent()).containsExactly(expectedReadOnlyDTO);
    }

    @Test
    void getTeachersPaginatedFiltered_throwsEntityNotFoundException_whenAmkaFilterHasNoMatch() {
        Pageable pageable = PageRequest.of(0, 10);
        TeacherFilters filters = TeacherFilters.builder().amka("00000000000").build();

        when(teacherRepository.findByPersonalInfo_Amka("00000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.getTeachersPaginatedFiltered(pageable, filters))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getTeachersPaginatedFiltered_returnsSingleResultPage_whenFilteringByVat() throws Exception {
        Teacher existing = buildExistingTeacher("123456789", "AB123456", "jsmith");
        Pageable pageable = PageRequest.of(0, 10);
        TeacherFilters filters = TeacherFilters.builder().vat("123456789").build();

        when(teacherRepository.findByVatAndDeletedFalse("123456789")).thenReturn(Optional.of(existing));
        when(mapper.mapToTeacherReadonlyDTO(existing)).thenReturn(expectedReadOnlyDTO);

        Page<TeacherReadOnlyDTO> result = teacherService.getTeachersPaginatedFiltered(pageable, filters);

        assertThat(result.getContent()).containsExactly(expectedReadOnlyDTO);
    }

    @Test
    void getTeachersPaginatedFiltered_throwsEntityNotFoundException_whenVatFilterHasNoMatch() {
        Pageable pageable = PageRequest.of(0, 10);
        TeacherFilters filters = TeacherFilters.builder().vat("000000000").build();

        when(teacherRepository.findByVatAndDeletedFalse("000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.getTeachersPaginatedFiltered(pageable, filters))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTeachersPaginatedFiltered_delegatesToSpecification_whenNoUniqueFilterProvided() throws Exception {
        Teacher existing = buildExistingTeacher("123456789", "AB123456", "jsmith");
        Pageable pageable = PageRequest.of(0, 10);
        TeacherFilters filters = TeacherFilters.builder().lastname("smith").build();
        Page<Teacher> teacherPage = new PageImpl<>(List.of(existing), pageable, 1);

        when(teacherRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(teacherPage);
        when(mapper.mapToTeacherReadonlyDTO(existing)).thenReturn(expectedReadOnlyDTO);

        Page<TeacherReadOnlyDTO> result = teacherService.getTeachersPaginatedFiltered(pageable, filters);

        assertThat(result.getContent()).containsExactly(expectedReadOnlyDTO);
        verify(teacherRepository, never()).findByUuidAndDeletedFalse(any());
        verify(teacherRepository, never()).findByPersonalInfo_Amka(anyString());
        verify(teacherRepository, never()).findByVatAndDeletedFalse(anyString());
    }

    // ---------- isTeacherExistsByVat ----------

    @Test
    void isTeacherExistsByVat_returnsTrue_whenTeacherExists() {
        when(teacherRepository.findByVat("123456789")).thenReturn(Optional.of(new Teacher()));

        assertThat(teacherService.isTeacherExistsByVat("123456789")).isTrue();
    }

    @Test
    void isTeacherExistsByVat_returnsFalse_whenTeacherDoesNotExist() {
        when(teacherRepository.findByVat("123456789")).thenReturn(Optional.empty());

        assertThat(teacherService.isTeacherExistsByVat("123456789")).isFalse();
    }

    // ---------- saveAmkaFile ----------

    @Test
    void saveAmkaFile_throwsEntityNotFoundException_whenTeacherDoesNotExist() {
        UUID uuid = UUID.randomUUID();
        when(teacherRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.saveAmkaFile(uuid, null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- helpers ----------

    private TeacherInsertDTO buildInsertDTO(String vat, String amka, String identityNumber, String username) {
        return TeacherInsertDTO.builder()
                .firstname("First")
                .lastname("Last")
                .vat(vat)
                .regionId(1L)
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

    private TeacherUpdateDTO buildUpdateDTO(UUID uuid, String vat, String identityNumber, String username, Long regionId) {
        return TeacherUpdateDTO.builder()
                .uuid(uuid)
                .firstname("Updated First")
                .lastname("Updated Last")
                .vat(vat)
                .regionId(regionId)
                .userUpdateDTO(UserInsertDTO.builder()
                        .username(username)
                        .password("Passw0rd!")
                        .roleId(3L)
                        .build())
                .personalInfoUpdateDTO(PersonalInfoInsertDTO.builder()
                        .amka("12345678901")
                        .identityNumber(identityNumber)
                        .placeOfBirth("Athens")
                        .municipalityOfRegistration("Athens")
                        .build())
                .build();
    }

    private Teacher buildExistingTeacher(String vat, String identityNumber, String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("hashed");
        role.addUser(user);

        PersonalInfo personalInfo = new PersonalInfo();
        personalInfo.setAmka("12345678901");
        personalInfo.setIdentityNumber(identityNumber);
        personalInfo.setPlaceOfBirth("Athens");
        personalInfo.setMunicipalityOfRegistration("Athens");

        Teacher teacher = new Teacher();
        teacher.setFirstname("First");
        teacher.setLastname("Last");
        teacher.setVat(vat);
        teacher.setPersonalInfo(personalInfo);
        teacher.addUser(user);
        region.addTeacher(teacher);

        return teacher;
    }
}
