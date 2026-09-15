package gr.aueb.cf.eduapp.api;

import gr.aueb.cf.eduapp.authentication.JwtService;
import gr.aueb.cf.eduapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.eduapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.eduapp.dto.PersonalInfoInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.eduapp.dto.TeacherUpdateDTO;
import gr.aueb.cf.eduapp.dto.UserInsertDTO;
import gr.aueb.cf.eduapp.service.ITeacherService;
import gr.aueb.cf.eduapp.validator.TeacherInsertValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer unit tests for {@link TeacherRestController}: request mapping, request-body
 * validation, and exception-to-HTTP-status translation (via the real {@link
 * gr.aueb.cf.eduapp.core.ErrorHandler}). Collaborators are Mockito beans, and security filters
 * are disabled so the tests stay focused on controller behaviour rather than authentication/
 * authorization (that is covered separately, e.g. in the service-layer integration tests).
 */
@WebMvcTest(TeacherRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ITeacherService teacherService;

    @MockitoBean
    private TeacherInsertValidator teacherInsertValidator;

    // JwtAuthenticationFilter is a servlet Filter @Component, so it is instantiated even in
    // this MVC slice (security processing itself is disabled below via addFilters = false).
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final TeacherReadOnlyDTO sampleDTO =
            new TeacherReadOnlyDTO(UUID.randomUUID().toString(), "First", "Last", "123456789", "Attica");

    // ---------- POST /api/v1/teachers ----------

    @Test
    void insertTeacher_returnsCreatedWithLocationHeader_whenDataIsValid() throws Exception {
        TeacherInsertDTO dto = buildInsertDTO("123456789", "12345678901", "AA111111", "jsmith");
        when(teacherService.saveTeacher(any(TeacherInsertDTO.class))).thenReturn(sampleDTO);

        mockMvc.perform(post("/api/v1/teachers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(sampleDTO.uuid())))
                .andExpect(jsonPath("$.uuid").value(sampleDTO.uuid()))
                .andExpect(jsonPath("$.vat").value(sampleDTO.vat()));

        verify(teacherService).saveTeacher(any(TeacherInsertDTO.class));
    }

    @Test
    void insertTeacher_returnsBadRequest_whenBeanValidationFails() throws Exception {
        TeacherInsertDTO invalidDto = buildInsertDTO("abc", "12345678901", "AA111111", "jsmith"); // vat must be digits

        mockMvc.perform(post("/api/v1/teachers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(teacherService, never()).saveTeacher(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void insertTeacher_returnsBadRequestWithFieldErrors_whenCustomValidatorRejectsVat() throws Exception {
        TeacherInsertDTO dto = buildInsertDTO("123456789", "12345678901", "AA111111", "jsmith");

        doAnswer(invocation -> {
            Errors errors = invocation.getArgument(1);
            errors.rejectValue("vat", "teacher.vat.exists", "Teacher with vat=123456789 already exists.");
            return null;
        }).when(teacherInsertValidator).validate(any(), any());

        mockMvc.perform(post("/api/v1/teachers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.vat").value("Teacher with vat=123456789 already exists."));

        verify(teacherService, never()).saveTeacher(any());
    }

    @Test
    void insertTeacher_returnsConflict_whenServiceDetectsDuplicateAmka() throws Exception {
        TeacherInsertDTO dto = buildInsertDTO("123456789", "12345678901", "AA111111", "jsmith");
        when(teacherService.saveTeacher(any(TeacherInsertDTO.class)))
                .thenThrow(new EntityAlreadyExistsException("AMKA", "Teacher with amka=12345678901 already exists"));

        mockMvc.perform(post("/api/v1/teachers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AMKAAlreadyExists"));
    }

    // ---------- POST /api/v1/teachers/{uuid}/amka-file ----------

    @Test
    void uploadAmkaFile_returnsNoContent_whenUploadSucceeds() throws Exception {
        UUID uuid = UUID.randomUUID();
        MockMultipartFile file =
                new MockMultipartFile("amkaFile", "amka.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/v1/teachers/{uuid}/amka-file", uuid).file(file))
                .andExpect(status().isNoContent());

        verify(teacherService).saveAmkaFile(eq(uuid), any());
    }

    @Test
    void uploadAmkaFile_returnsNotFound_whenTeacherDoesNotExist() throws Exception {
        UUID uuid = UUID.randomUUID();
        MockMultipartFile file =
                new MockMultipartFile("amkaFile", "amka.pdf", "application/pdf", "content".getBytes());

        doAnswer(invocation -> {
            throw new EntityNotFoundException("Teacher", "Teacher with uuid=" + uuid);
        }).when(teacherService).saveAmkaFile(eq(uuid), any());

        mockMvc.perform(multipart("/api/v1/teachers/{uuid}/amka-file", uuid).file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TeacherNotFound"));
    }

    // ---------- PUT /api/v1/teachers/{uuid} ----------

    @Test
    void updateTeacher_returnsOk_whenDataIsValid() throws Exception {
        UUID uuid = UUID.randomUUID();
        TeacherUpdateDTO dto = buildUpdateDTO(uuid, "123456789", "AA111111", "jsmith", 1L);
        when(teacherService.updateTeacher(any(TeacherUpdateDTO.class))).thenReturn(sampleDTO);

        mockMvc.perform(put("/api/v1/teachers/{uuid}", uuid)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vat").value(sampleDTO.vat()));

        verify(teacherService).updateTeacher(any(TeacherUpdateDTO.class));
    }

    @Test
    void updateTeacher_returnsNotFound_whenTeacherDoesNotExist() throws Exception {
        UUID uuid = UUID.randomUUID();
        TeacherUpdateDTO dto = buildUpdateDTO(uuid, "123456789", "AA111111", "jsmith", 1L);
        when(teacherService.updateTeacher(any(TeacherUpdateDTO.class)))
                .thenThrow(new EntityNotFoundException("Teacher", "Teacher with uuid=" + uuid + " does not exist"));

        mockMvc.perform(put("/api/v1/teachers/{uuid}", uuid)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TeacherNotFound"));
    }

    // ---------- DELETE /api/v1/teachers/{uuid} ----------

    @Test
    void deleteTeacherByUUID_returnsOk_whenTeacherExists() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(teacherService.deleteTeacherByUUID(uuid)).thenReturn(sampleDTO);

        mockMvc.perform(delete("/api/v1/teachers/{uuid}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(sampleDTO.uuid()));
    }

    @Test
    void deleteTeacherByUUID_returnsNotFound_whenTeacherDoesNotExist() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(teacherService.deleteTeacherByUUID(uuid))
                .thenThrow(new EntityNotFoundException("Teacher", "Teacher with uuid=" + uuid + "not found"));

        mockMvc.perform(delete("/api/v1/teachers/{uuid}", uuid))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TeacherNotFound"));
    }

    // ---------- GET /api/v1/teachers/{uuid} ----------

    @Test
    void getTeacherByUUID_returnsOk_whenTeacherExists() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(teacherService.getTeacherByUUIDDeletedFalse(uuid)).thenReturn(sampleDTO);

        mockMvc.perform(get("/api/v1/teachers/{uuid}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstname").value(sampleDTO.firstname()));

        verify(teacherService).getTeacherByUUIDDeletedFalse(uuid);
        verify(teacherService, never()).getTeacherByUUID(any());
    }

    @Test
    void getTeacherByUUID_returnsNotFound_whenTeacherDoesNotExist() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(teacherService.getTeacherByUUIDDeletedFalse(uuid))
                .thenThrow(new EntityNotFoundException("Teacher", "Teacher with uuid=" + uuid));

        mockMvc.perform(get("/api/v1/teachers/{uuid}", uuid))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TeacherNotFound"));
    }

    // ---------- GET /api/v1/teachers ----------

    @Test
    @SuppressWarnings("unchecked")
    void getFilteredAndPaginatedTeachers_returnsPageOfTeachers() throws Exception {
        Page<TeacherReadOnlyDTO> page = new PageImpl<>(List.of(sampleDTO), PageRequest.of(0, 5), 1);
        when(teacherService.getTeachersPaginatedFiltered(any(Pageable.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/teachers")
                        .param("page", "0")
                        .param("size", "5")
                        .param("lastname", "Last"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].vat").value(sampleDTO.vat()))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(teacherService).getTeachersPaginatedFiltered(any(Pageable.class), any());
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
                .firstname("First")
                .lastname("Last")
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
}
