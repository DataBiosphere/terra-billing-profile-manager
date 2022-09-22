package bio.terra.profile.service.profile.exception;

import bio.terra.common.exception.ErrorReportException;
import java.util.List;
import org.springframework.http.HttpStatus;

public class MissingRequiredProvidersException extends ErrorReportException {
  public MissingRequiredProvidersException(String message, List<String> missingProviders) {
    super(message, missingProviders, HttpStatus.CONFLICT);
  }
}
