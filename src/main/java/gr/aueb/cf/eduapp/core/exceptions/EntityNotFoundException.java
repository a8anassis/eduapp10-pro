package gr.aueb.cf.eduapp.core.exceptions;

public class EntityNotFoundException extends AppGenericException {
    private static final String DEFAULT_CODE = "NotFound";

    public EntityNotFoundException(String code, String message) {
        super(code + DEFAULT_CODE, message);
    }
}
