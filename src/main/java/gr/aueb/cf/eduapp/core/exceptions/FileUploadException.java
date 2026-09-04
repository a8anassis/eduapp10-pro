package gr.aueb.cf.eduapp.core.exceptions;

public class FileUploadException extends AppGenericException {
    private static final String DEFAULT_CODE = "FileUploadError";

    public FileUploadException(String code, String message) {
        super(code + DEFAULT_CODE, message);
    }

}
