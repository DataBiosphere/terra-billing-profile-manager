package bio.terra.profile.app.controller;

import bio.terra.common.exception.AbstractGlobalExceptionHandler;
import bio.terra.common.exception.BadRequestException;
import bio.terra.common.sam.exception.SamUnauthorizedException;
import bio.terra.profile.model.ErrorReport;
import io.sentry.Sentry;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
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
  @ExceptionHandler({MissingServletRequestParameterException.class, BadRequestException.class})
  public ResponseEntity<ErrorReport> validationExceptionHandler(Exception ex) {
    var errorReport =
        new ErrorReport()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .message("Invalid request: " + ex.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorReport);
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
  })
  public ResponseEntity<ErrorReport> mismatchedArgsHandler(MethodArgumentTypeMismatchException ex) {
    var errorReport =
        new ErrorReport()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .message(
                "Invalid request: "
                    + ex.getParameter().getParameterName()
                    + " must be a "
                    + Objects.requireNonNull(ex.getRequiredType()).getSimpleName().toLowerCase());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorReport);
  }

  @ExceptionHandler({DataAccessException.class})
  public ResponseEntity<ErrorReport> constraintViolationExceptionHandler(
      ConstraintViolationException ex) {
    // Handle these exceptions here to avoid leaking SQL error messages to the client.
    ErrorReport errorReport =
        new ErrorReport().message(ex.getMessage()).statusCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorReport, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({SamUnauthorizedException.class})
  public ResponseEntity<ErrorReport> samUnauthorizedExceptionHandler(SamUnauthorizedException ex) {
    ErrorReport errorReport =
        new ErrorReport().message("Unauthorized").statusCode(HttpStatus.UNAUTHORIZED.value());
    return new ResponseEntity<>(errorReport, HttpStatus.UNAUTHORIZED);
  }
}
