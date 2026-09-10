package gr.aueb.cf.eduapp.security;

import gr.aueb.cf.eduapp.dto.ErrorResponseDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException e) throws IOException, ServletException {
        Object jwtErrorCode = request.getAttribute("auth_error_code");
        Object jwtErrorMessage = request.getAttribute("auth_error");

        String errorCode;
        String message;

        if (jwtErrorCode != null) {
            errorCode = (String) jwtErrorCode;
            message = (String) jwtErrorMessage;
        } else {
            errorCode = switch (e.getClass().getSimpleName()) {
                case "BadCredentialsException" -> "BAD_CREDENTIALS";
                case "DisabledException" -> "ACCOUNT_DISABLED";
                case "LockedException" -> "ACCOUNT_LOCKED";
                case "AccountExpiredException" -> "ACCOUNT_EXPIRED";
                case "CredentialsExpiredException" -> "CREDENTIALS_EXPIRED";
                default -> "UNAUTHORIZED";
            };
            message = e.getMessage();
        }

        log.warn("User not authenticated, with message={}", message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=UTF-8");

        response.getWriter().write(
                objectMapper.writeValueAsString(
                        new ErrorResponseDTO(errorCode, message)
                )
        );
    }
}
