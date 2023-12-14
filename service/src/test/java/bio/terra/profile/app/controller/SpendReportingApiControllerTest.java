package bio.terra.profile.app.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.SpendReportingConfig;
import bio.terra.profile.common.AuthRequestFixtures;
import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.spendreporting.SpendReportingService;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SpendReportingApiControllerTest extends BaseSpringUnitTest {
  @Autowired MockMvc mockMvc;
  @MockBean SamService samService;
  @MockBean SpendReportingService spendReportingService;
  @Autowired SpendReportingConfig spendReportingConfig;

  private final AuthenticatedUserRequest userRequest = AuthRequestFixtures.buildAuthRequest();

  @BeforeEach
  void setup() throws Exception {
    when(samService.getUserStatusInfo(userRequest.getToken()))
        .thenReturn(
            new UserStatusInfo()
                .userSubjectId(userRequest.getSubjectId())
                .userEmail(userRequest.getEmail())
                .enabled(true));
  }

  @Test
  void getSpendReportWithWrongDateRange_returnBadRequest() throws Exception {
    var profileId = UUID.randomUUID();
    var startDate = OffsetDateTime.now();
    var endDate = startDate.minusDays(30);
    mockMvc
        .perform(
            get("/api/profiles/v1/{profileId}/spendReport", profileId.toString())
                .queryParam("spendReportStartDate", startDate.format(DateTimeFormatter.ISO_DATE))
                .queryParam("spendReportEndDate", endDate.format(DateTimeFormatter.ISO_DATE))
                .header("Authorization", "Bearer " + userRequest.getToken()))
        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
        .andExpect(content().string(containsString("End date should be greater than start date")));
  }

  @Test
  void getSpendReportWithWrongStartDateFormat_returnBadRequest() throws Exception {
    var profileId = UUID.randomUUID();
    var now = OffsetDateTime.now();
    var startDateWithWrongFormat =
        String.format("%s.%s.%s", now.getMonth(), now.getDayOfMonth(), now.getYear());
    mockMvc
        .perform(
            get("/api/profiles/v1/{profileId}/spendReport", profileId.toString())
                .queryParam("spendReportStartDate", startDateWithWrongFormat)
                .queryParam(
                    "spendReportEndDate", now.plusDays(30).format(DateTimeFormatter.ISO_DATE))
                .header("Authorization", "Bearer " + userRequest.getToken()))
        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
        .andExpect(content().string(containsString("spendReportStartDate must be a date")));
  }

  @Test
  void getSpendReportWithWrongEndDateFormat_returnBadRequest() throws Exception {
    var profileId = UUID.randomUUID();
    var now = OffsetDateTime.now();
    var endDateWithWrongFormat =
        String.format("%s.%s.%s", now.getMonth(), now.getDayOfMonth(), now.getYear());
    mockMvc
        .perform(
            get("/api/profiles/v1/{profileId}/spendReport", profileId.toString())
                .queryParam(
                    "spendReportStartDate", now.minusDays(30).format(DateTimeFormatter.ISO_DATE))
                .queryParam("spendReportEndDate", endDateWithWrongFormat)
                .header("Authorization", "Bearer " + userRequest.getToken()))
        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
        .andExpect(content().string(containsString("spendReportEndDate must be a date")));
  }

  @Test
  void getSpendReportWithDateRangeWhichExceedsMaxValue_returnBadRequest() throws Exception {
    var profileId = UUID.randomUUID();
    var from =
        OffsetDateTime.now()
            .minusDays(spendReportingConfig.getAzure().getMaxDateRangeDays() + 30 /*some extra*/);
    var to = from.plusDays(spendReportingConfig.getAzure().getMaxDateRangeDays() + 10);
    mockMvc
        .perform(
            get("/api/profiles/v1/{profileId}/spendReport", profileId.toString())
                .queryParam("spendReportStartDate", from.format(DateTimeFormatter.ISO_DATE))
                .queryParam("spendReportEndDate", to.format(DateTimeFormatter.ISO_DATE))
                .header("Authorization", "Bearer " + userRequest.getToken()))
        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
        .andExpect(
            content().string(containsString("Provided dates exceed maximum report date range")));
  }

  @Test
  void getSpendReportWithWrongDateParameters_returnOk() throws Exception {
    var profileId = UUID.randomUUID();
    var startDate = OffsetDateTime.now().minusMonths(3);
    var endDate = startDate.plusMonths(2);
    mockMvc
        .perform(
            get("/api/profiles/v1/{profileId}/spendReport", profileId.toString())
                .queryParam("spendReportStartDate", startDate.format(DateTimeFormatter.ISO_DATE))
                .queryParam("spendReportEndDate", endDate.format(DateTimeFormatter.ISO_DATE))
                .header("Authorization", "Bearer " + userRequest.getToken()))
        .andExpect(status().is(HttpStatus.OK.value()));
  }
}
