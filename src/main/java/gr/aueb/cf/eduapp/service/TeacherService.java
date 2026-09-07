package gr.aueb.cf.eduapp.service;

import gr.aueb.cf.eduapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.eduapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.eduapp.core.exceptions.FileUploadException;
import gr.aueb.cf.eduapp.dto.PersonalInfoInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherInsertDTO;
import gr.aueb.cf.eduapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.eduapp.mapper.Mapper;
import gr.aueb.cf.eduapp.model.Region;
import gr.aueb.cf.eduapp.model.Role;
import gr.aueb.cf.eduapp.model.Teacher;
import gr.aueb.cf.eduapp.model.User;
import gr.aueb.cf.eduapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherService implements ITeacherService {

    private final TeacherRepository teacherRepository;
    private final RegionRepository regionRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PersonalInfoRepository personalInfoRepository;
    private final Mapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Override
    @Transactional(rollbackFor = { EntityAlreadyExistsException.class, EntityInvalidArgumentException.class })
    public TeacherReadOnlyDTO saveTeacher(TeacherInsertDTO dto)
            throws EntityAlreadyExistsException, EntityInvalidArgumentException {

        if (dto.vat() != null && teacherRepository.findByVat(dto.vat()).isPresent()) {
            throw new EntityAlreadyExistsException("Teacher", "Teacher with vat=" + dto.vat() + " already exists");
        }

        if (dto.personalInfoInsertDTO().amka() != null
                && personalInfoRepository.findByAmka(dto.personalInfoInsertDTO().amka()).isPresent()) {
            throw new EntityAlreadyExistsException("AMKA", "Teacher with amka=" + dto.personalInfoInsertDTO().amka()
                    +  " already exists");
        }

        if (dto.personalInfoInsertDTO().identityNumber() != null
                && personalInfoRepository.findByIdentityNumber(dto.personalInfoInsertDTO().identityNumber()).isPresent()) {
            throw new EntityAlreadyExistsException("IdentityNumber",  "Teacher with identityNumber="
                    + dto.personalInfoInsertDTO().identityNumber() +  " already exists");
        }

        if (dto.userInsertDTO().username() != null &&
                userRepository.findByUsername(dto.userInsertDTO().username()).isPresent()) {
            throw new EntityAlreadyExistsException("Username",  "User with username="
                    +  dto.userInsertDTO().username() +  " already exists");
        }

        Region region = regionRepository.findById(dto.regionId()).orElseThrow(() ->
                new EntityInvalidArgumentException("Region", "Region with id=" + dto.regionId() + " does not exist"));

        final Long teacherRoleId = 3L;  // TODO να αλλάξει το DTO

        Role role = roleRepository.findById(teacherRoleId).orElseThrow(()
                -> new EntityInvalidArgumentException("Role", "Role with id=" + teacherRoleId + " does not exist"));

        Teacher teacher = mapper.mapToTeacherEntity(dto);

        User user = teacher.getUser();
        user.setPassword(passwordEncoder.encode(dto.userInsertDTO().password()));

        region.addTeacher(teacher);
        role.addUser(user);

        teacherRepository.save(teacher);

        log.info("Teacher with vat={} saved successfully", dto.vat());
        return mapper.mapToTeacherReadonlyDTO(teacher);
    }

    @Override
    public void saveAmkaFile(UUID uuid, MultipartFile file)
            throws FileUploadException, EntityAlreadyExistsException {

    }
}
