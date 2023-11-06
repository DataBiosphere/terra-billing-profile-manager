package bio.terra.profile.app.controller;

import bio.terra.common.exception.InconsistentFieldsException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.iam.AuthenticatedUserRequestFactory;
import bio.terra.profile.api.SpendReportingApi;
import bio.terra.profile.app.configuration.SpendReportingConfig;
import bio.terra.profile.model.SpendReport;
import bio.terra.profile.service.spendreporting.SpendReportingService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class SpendReportingApiController implements SpendReportingApi {

  private final HttpServletRequest request;
  private final SpendReportingService spendReportingService;
  private final AuthenticatedUserRequestFactory authenticatedUserRequestFactory;
  private final SpendReportingConfig spendReportingConfig;

  @Autowired
  public SpendReportingApiController(
      HttpServletRequest request,
      SpendReportingService spendReportingService,
      AuthenticatedUserRequestFactory authenticatedUserRequestFactory,
      SpendReportingConfig spendReportingConfig) {
    this.request = request;
    this.spendReportingService = spendReportingService;
    this.authenticatedUserRequestFactory = authenticatedUserRequestFactory;
    this.spendReportingConfig = spendReportingConfig;
  }

  @Override
  public ResponseEntity<SpendReport> getSpendReport(
      UUID id,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date spendReportStartDate,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date spendReportEndDate) {
    OffsetDateTime startDate = spendReportStartDate.toInstant().atOffset(ZoneOffset.UTC);
    OffsetDateTime endDate = spendReportEndDate.toInstant().atOffset(ZoneOffset.UTC);
    throwsIfParametersAreNotValid(startDate, endDate);

    AuthenticatedUserRequest authenticatedUserRequest =
        authenticatedUserRequestFactory.from(request);
    return new ResponseEntity<>(
        spendReportingService.getSpendReport(id, startDate, endDate, authenticatedUserRequest),
        HttpStatus.OK);
  }

  private void throwsIfParametersAreNotValid(OffsetDateTime startDate, OffsetDateTime endDate) {
    if (endDate.isBefore(startDate)) {
      throw new InconsistentFieldsException("End date should be greater than start date.");
    }
    OffsetDateTime now = OffsetDateTime.now();
    if (startDate.isAfter(now) || endDate.isAfter(now)) {
      throw new InconsistentFieldsException("Start date and end date should not be future date.");
    }
    Duration duration = Duration.between(endDate, startDate);
    if (Math.abs(duration.toDays()) > spendReportingConfig.getAzure().getMaxDateRangeDays()) {
      throw new InconsistentFieldsException(
          String.format(
              "Provided dates exceed maximum report date range of %s days.",
              spendReportingConfig.getAzure().getMaxDateRangeDays()));
    }
  }
}
