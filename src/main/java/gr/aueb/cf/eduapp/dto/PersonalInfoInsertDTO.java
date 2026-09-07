package gr.aueb.cf.eduapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record PersonalInfoInsertDTO(
        @NotNull
        @Pattern(regexp = "\\d{11}")
        String amka,

        @NotBlank
        String identityNumber,

        @NotBlank
        String placeOfBirth,

        @NotBlank
        String municipalityOfRegistration
) {}
