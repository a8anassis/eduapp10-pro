package gr.aueb.cf.eduapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UserUpdateDTO(

        @NotNull
        @Size(min = 3, max = 30)
        String username,

        @NotNull
        @Pattern(regexp = "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&+=])^.{8,}$")
        String password
) {
}
