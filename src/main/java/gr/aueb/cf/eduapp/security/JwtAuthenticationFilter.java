package gr.aueb.cf.eduapp.security;

import gr.aueb.cf.eduapp.authentication.CustomUserDetailsService;
import gr.aueb.cf.eduapp.authentication.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");
        final String jwt;

        String username;

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authorizationHeader.substring(7);

        try {
            username = jwtService.extractSubject(jwt);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (!jwtService.isTokenValid(jwt, userDetails)) {
                    throw new BadCredentialsException("Invalid token");
                }

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            request.setAttribute("auth_error_code", "TOKEN_EXPIRED");
            request.setAttribute("auth_error", "Token has expired");
        } catch (JwtException | IllegalArgumentException | BadCredentialsException e) {
            log.warn("Invalid token: {}", e.getMessage());
            request.setAttribute("auth_error_code", "INVALID_TOKEN");
            request.setAttribute("auth_error", "Invalid token");
        } catch (UsernameNotFoundException e) {
            log.warn("User not found: {}", e.getMessage());
            request.setAttribute("auth_error_code", "INVALID_TOKEN");
            request.setAttribute("auth_error", "Invalid token");
        } catch (Exception e) {
            log.error("Unexpected error during token validation", e);
            request.setAttribute("auth_error_code", "TOKEN_VALIDATION_FAILED");
            request.setAttribute("auth_error", "Token validation failed");
        }

        filterChain.doFilter(request, response);
    }
}
