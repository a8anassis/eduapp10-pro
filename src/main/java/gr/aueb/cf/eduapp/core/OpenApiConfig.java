package gr.aueb.cf.eduapp.core;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/*
  Swagger UI shows an "Authorize" button
 */
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenApiConfig {

    /*
      Provides metadata for Swagger UI header section.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return  new OpenAPI()
                .info(new Info()
                        .title("EduApp API")
                        .version("1.0.0")
                        .description("""
                             REST API for managing Coding Factory teachers' registry.
                             Provides endpoints for managing teachers, users, and organizational data.
                             
                             Authentication & Authorization is done via JWT Bearer tokens.    
                             Obtain a token from /api/auth/authenticate before using secured endpoints.   
                        """)
                        .contact(new Contact()
                                .name("Coding Factory @ AUEB")
                                .email("codingfactory@aueb.gr")
                                .url("https://codingfactory.aueb.gr")
                        )
                        .license(new License()
                                .name("CC0 1.0 University")
                                .url("https://creativecommons.org/publicdomain/zero/1.0/")
                        )
                );
    }

    @Bean
    public OperationCustomizer globalSecurityResponses() {

        return (operation, handlerMethod) -> {
            // Με το || το endpoint θεωρείται secured αν το annotation
            // -@SecurityRequirement(name = "bearerAuth")- υπάρχει είτε στη method είτε στην κλάση

            boolean isSecured = handlerMethod.hasMethodAnnotation(SecurityRequirement.class)
                    || handlerMethod.getBeanType().isAnnotationPresent(SecurityRequirement.class);

            if (isSecured) {
                var responses = operation.getResponses();
                responses.putIfAbsent("401", new ApiResponse().description("Unauthorized - JWT token is missing or invalid"));
                responses.putIfAbsent("403", new ApiResponse().description("Forbidden - You don't have permission to access this resource"));
            }

            return operation;
        };

    }
}
