package bio.terra.profile.app.controller;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.iam.AuthenticatedUserRequestFactory;
import bio.terra.profile.api.SpendReportingApi;
import bio.terra.profile.model.SpendReport;
import bio.terra.profile.service.spendreporting.SpendReportingService;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class SpendReportingApiController implements SpendReportingApi {

  private final HttpServletRequest request;
  private final SpendReportingService spendReportingService;
  private final AuthenticatedUserRequestFactory authenticatedUserRequestFactory;

  @Autowired
  public SpendReportingApiController(
      HttpServletRequest request,
      SpendReportingService spendReportingService,
      AuthenticatedUserRequestFactory authenticatedUserRequestFactory) {
    this.request = request;
    this.spendReportingService = spendReportingService;
    this.authenticatedUserRequestFactory = authenticatedUserRequestFactory;
  }

  @Override
  public ResponseEntity<SpendReport> getSpendReport(
      UUID id, Date spendReportStartDate, Date spendReportEndDate
      /*, Temporarily commented to follow the swagger specification
      List<String> spendReportAggregationKey*/ ) {
    AuthenticatedUserRequest authenticatedUserRequest =
        authenticatedUserRequestFactory.from(request);
    return new ResponseEntity<SpendReport>(
        spendReportingService.getSpendReport(
            id,
            // assumption that incoming date is in UTC
            spendReportStartDate.toInstant().atOffset(ZoneOffset.UTC),
            spendReportEndDate.toInstant().atOffset(ZoneOffset.UTC),
            List.of("spendReportAggregationKey"), // update once aggregationKey is a valid parameter
            authenticatedUserRequest),
        HttpStatus.OK);
  }
}
