package gr.aueb.cf.eduapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record TeacherInsertDTO(

        @NotNull
        @Size(min = 2)
        String firstname,

        @NotNull
        @Size(min = 2)
        String lastname,

        @Pattern(regexp = "\\d{9,}")
        String vat,

        @NotNull
        Long regionId,

        @NotNull
        @Valid
        UserInsertDTO userInsertDTO,

        @NotNull
        @Valid
        PersonalInfoInsertDTO personalInfoInsertDTO
) {}
