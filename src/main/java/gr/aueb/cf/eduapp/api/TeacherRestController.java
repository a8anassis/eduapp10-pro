package gr.aueb.cf.eduapp.api;

import gr.aueb.cf.eduapp.service.ITeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
public class TeacherRestController {

    private final ITeacherService teacherService;
}
