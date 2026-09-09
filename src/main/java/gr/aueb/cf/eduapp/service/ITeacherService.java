package gr.aueb.cf.eduapp.service;

import gr.aueb.cf.eduapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.eduapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.eduapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.eduapp.core.exceptions.FileUploadException;
import gr.aueb.cf.eduapp.core.filters.TeacherFilters;
import gr.aueb.cf.eduapp.dto.TeacherInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.eduapp.dto.TeacherUpdateDTO;
import gr.aueb.cf.eduapp.model.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ITeacherService {

    TeacherReadOnlyDTO saveTeacher(TeacherInsertDTO teacherInsertDTO)
            throws EntityAlreadyExistsException, EntityInvalidArgumentException;

    void saveAmkaFile(UUID uuid, MultipartFile file) throws FileUploadException, EntityNotFoundException;

    TeacherReadOnlyDTO updateTeacher(TeacherUpdateDTO teacherUpdateDTO)
            throws EntityNotFoundException, EntityAlreadyExistsException, EntityInvalidArgumentException;

    TeacherReadOnlyDTO deleteTeacherByUUID(UUID uuid) throws EntityNotFoundException;

    TeacherReadOnlyDTO getTeacherByUUID(UUID uuid) throws EntityNotFoundException;
    TeacherReadOnlyDTO getTeacherByUUIDDeletedFalse(UUID uuid) throws EntityNotFoundException;

    Page<TeacherReadOnlyDTO> getPaginatedTeachers(Pageable pageable);
    Page<TeacherReadOnlyDTO> getPaginatedTeachersDeletedFalse(Pageable pageable);
    Page<TeacherReadOnlyDTO> getTeachersPaginatedFiltered(Pageable pageable, TeacherFilters filters)
            throws EntityNotFoundException;
    boolean isTeacherExistsByVat(String vat);

}
