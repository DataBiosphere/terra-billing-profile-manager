package bio.terra.profile.service.spendreporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.common.exception.ForbiddenException;
import bio.terra.profile.common.AuthRequestFixtures;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.common.ProfileFixtures;
import bio.terra.profile.common.SpendDataFixtures;
import bio.terra.profile.common.SpendReportFixtures;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.iam.model.SamAction;
import bio.terra.profile.service.iam.model.SamResourceType;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.profile.service.spendreporting.azure.AzureSpendReportingService;
import bio.terra.profile.service.spendreporting.azure.model.SpendData;
import bio.terra.profile.service.spendreporting.azure.model.mapper.SpendDataMapper;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class SpendReportingServiceUnitTest extends BaseUnitTest {
  private SpendReportingService spendReportingService;

  @Mock ProfileDao mockProfileDao;
  @Mock SamService mockSamService;
  @Mock AzureSpendReportingService mockAzureSpendReportingService;
  @Mock SpendDataMapper mockSpendDataMapper;
  @Captor ArgumentCaptor<OffsetDateTime> fromCaptor;
  @Captor ArgumentCaptor<OffsetDateTime> toCaptor;
  @Captor ArgumentCaptor<BillingProfile> billingProfileCaptor;
  @Captor ArgumentCaptor<SpendData> spendDataCaptor;

  @BeforeEach
  public void setup() {
    spendReportingService =
        new SpendReportingService(
            mockProfileDao, mockSamService, mockAzureSpendReportingService, mockSpendDataMapper);
  }

  @Test
  public void testGetSpendReportForGcpThrowsException() throws InterruptedException {
    var resourceId = UUID.randomUUID();
    var billingAccountId = UUID.randomUUID().toString();
    var gcpBillingProfile = ProfileFixtures.createGcpBillingProfile(billingAccountId);
    var authenticatedUserRequest = AuthRequestFixtures.buildAuthRequest();

    when(mockProfileDao.getBillingProfileById(any(UUID.class))).thenReturn(gcpBillingProfile);
    when(mockSamService.isAuthorized(
            authenticatedUserRequest,
            SamResourceType.PROFILE,
            resourceId,
            SamAction.READ_SPEND_REPORT))
        .thenReturn(true);

    var from = OffsetDateTime.now();
    var to = from.plusDays(30);
    assertThrows(
        NotImplementedException.class,
        () -> spendReportingService.getSpendReport(resourceId, from, to, authenticatedUserRequest));
  }

  @Test
  public void testGetSpendReportUserIsNotAuthorizedThrowsException() throws InterruptedException {
    UUID billingProfileId = UUID.randomUUID();
    var authenticatedUserRequest = AuthRequestFixtures.buildAuthRequest();

    doThrow(ForbiddenException.class)
        .when(mockSamService)
        .verifyAuthorization(
            authenticatedUserRequest,
            SamResourceType.PROFILE,
            billingProfileId,
            SamAction.READ_SPEND_REPORT);

    var from = OffsetDateTime.now();
    var to = from.plusDays(30);
    assertThrows(
        ForbiddenException.class,
        () ->
            spendReportingService.getSpendReport(
                billingProfileId, from, to, authenticatedUserRequest));
  }

  @Test
  public void testGetSpendReportUserIsAuthorizedSuccess() throws InterruptedException {
    String billingAccountId = UUID.randomUUID().toString();
    UUID billingProfileId = UUID.randomUUID();
    var authenticatedUserRequest = AuthRequestFixtures.buildAuthRequest();
    var azureBillingProfile =
        ProfileFixtures.createAzureBillingProfile(UUID.randomUUID(), UUID.randomUUID(), "mrgName");

    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = from.plusDays(30);
    when(mockProfileDao.getBillingProfileById(any(UUID.class))).thenReturn(azureBillingProfile);
    var spendData = SpendDataFixtures.buildDefaultSpendData();
    when(mockAzureSpendReportingService.getBillingProjectSpendData(azureBillingProfile, from, to))
        .thenReturn(spendData);
    var spendReport = SpendReportFixtures.buildDefaultSpendReport();
    when(mockSpendDataMapper.mapSpendData(spendData)).thenReturn(spendReport);

    doNothing()
        .when(mockSamService)
        .verifyAuthorization(
            authenticatedUserRequest,
            SamResourceType.PROFILE,
            billingProfileId,
            SamAction.READ_SPEND_REPORT);

    spendReportingService.getSpendReport(billingProfileId, from, to, authenticatedUserRequest);

    verify(mockAzureSpendReportingService, times(1))
        .getBillingProjectSpendData(
            billingProfileCaptor.capture(), fromCaptor.capture(), toCaptor.capture());
    assertThat(billingProfileCaptor.getValue(), equalTo(azureBillingProfile));
    assertThat(fromCaptor.getValue(), equalTo(from));
    assertThat(toCaptor.getValue(), equalTo(to));

    verify(mockSpendDataMapper, times(1)).mapSpendData(spendDataCaptor.capture());
    assertThat(spendDataCaptor.getValue(), equalTo(spendData));
  }
}
