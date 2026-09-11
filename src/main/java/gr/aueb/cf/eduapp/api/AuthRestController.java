package gr.aueb.cf.eduapp.api;

import gr.aueb.cf.eduapp.authentication.AuthenticationService;
import gr.aueb.cf.eduapp.dto.AuthenticationRequestDTO;
import gr.aueb.cf.eduapp.dto.AuthenticationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final AuthenticationService authenticationService;

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponseDTO> authenticate(
            @RequestBody AuthenticationRequestDTO authenticationRequestDTO) {

        AuthenticationResponseDTO responseDTO = authenticationService
                .authenticate(authenticationRequestDTO);
        return  ResponseEntity.ok(responseDTO);
    }
}
