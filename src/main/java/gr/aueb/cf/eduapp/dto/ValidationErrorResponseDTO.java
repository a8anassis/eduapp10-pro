package gr.aueb.cf.eduapp.dto;

import java.util.Map;

public record ValidationErrorResponseDTO(String code, String message, Map<String, String> error) {
}
