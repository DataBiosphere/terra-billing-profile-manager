package bio.terra.profile.app.common;

import bio.terra.common.exception.ErrorReportException;
import bio.terra.profile.model.ErrorReport;
import org.springframework.http.HttpStatus;

/** A common utility for building an ApiErrorReport from an exception. */
public class ErrorReportUtils {
  private ErrorReportUtils() {}

  public static ErrorReport buildApiErrorReport(Exception exception) {
    if (exception instanceof ErrorReportException errorReport) {
      return new ErrorReport()
          .message(errorReport.getMessage())
          .statusCode(errorReport.getStatusCode().value())
          .causes(errorReport.getCauses());
    } else {
      return new ErrorReport()
          .message(exception.getMessage())
          .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
          .causes(null);
    }
  }
}
