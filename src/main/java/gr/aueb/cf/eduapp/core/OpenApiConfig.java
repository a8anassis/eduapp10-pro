package gr.aueb.cf.eduapp.core;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
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
}
