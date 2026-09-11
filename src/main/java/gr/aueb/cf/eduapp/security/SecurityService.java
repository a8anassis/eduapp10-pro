package gr.aueb.cf.eduapp.security;

import gr.aueb.cf.eduapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.eduapp.model.User;
import gr.aueb.cf.eduapp.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private final TeacherRepository  teacherRepository;

    public boolean isOwnTeacherProfile(UUID teacherUUID, Authentication authentication) {
        User principal = (User) authentication.getPrincipal();

        return teacherRepository.existsByUuidAndUser_Uuid(teacherUUID, principal.getUuid());
    }
}
