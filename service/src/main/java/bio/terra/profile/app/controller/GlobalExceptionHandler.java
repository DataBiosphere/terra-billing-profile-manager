package bio.terra.profile.app.controller;

import bio.terra.common.exception.AbstractGlobalExceptionHandler;
import bio.terra.profile.model.ErrorReport;
import io.sentry.Sentry;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * This module provides a top-level exception handler for controllers. All exceptions that rise
 * through the controllers are caught in this handler. It converts the exceptions into standard
 * ApiErrorReport responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler<ErrorReport> {
  @Override
  public ErrorReport generateErrorReport(Throwable ex, HttpStatus statusCode, List<String> causes) {
    if (statusCode.is5xxServerError()) {
      Sentry.captureException(ex);
    }
    return new ErrorReport().message(ex.getMessage()).statusCode(statusCode.value()).causes(causes);
  }

  @Override
  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<ErrorReport> validationExceptionHandler(Exception ex) {
    var errorReport =
        new ErrorReport()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .message("Invalid request " + ex.getClass().getSimpleName());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorReport);
  }
}
