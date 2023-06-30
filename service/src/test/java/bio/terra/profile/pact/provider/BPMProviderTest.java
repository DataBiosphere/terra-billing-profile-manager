package bio.terra.profile.pact.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.iam.AuthenticatedUserRequestFactory;
import bio.terra.profile.app.Main;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.AzureManagedAppModel;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.model.SamPolicyModel;
import bio.terra.profile.model.SystemStatusSystems;
import bio.terra.profile.service.azure.AzureService;
import bio.terra.profile.service.crl.AzureCrlService;
import bio.terra.profile.service.crl.GcpCrlService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.iam.model.SamAction;
import bio.terra.profile.service.iam.model.SamResourceType;
import bio.terra.profile.service.job.JobBuilder;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.profile.service.job.JobService;
import bio.terra.profile.service.profile.exception.ProfileNotFoundException;
import bio.terra.profile.service.profile.flight.ProfileMapKeys;
import bio.terra.profile.service.profile.flight.create.CreateProfileFlight;
import bio.terra.profile.service.profile.flight.delete.DeleteProfileFlight;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.profile.service.spendreporting.azure.AzureSpendReportingService;
import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
import bio.terra.profile.service.spendreporting.azure.model.SpendData;
import bio.terra.profile.service.spendreporting.azure.model.SpendDataItem;
import bio.terra.profile.service.status.ProfileStatusService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Tag("provider-test")
@Provider("bpm-provider")
@PactBroker
// for local testing, put any test pacts in the service/pacts folder.
// then comment out the above line, and uncomment the following line
// @PactFolder("pacts")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = Main.class,
    properties = {
      "profile.profile-database.initialize-on-start=false",
      "profile.profile-database.upgrade-on-start=false"
    })
@EnableAutoConfiguration(
    exclude = {DataSourceAutoConfiguration.class, JdbcRepositoriesAutoConfiguration.class})
public class BPMProviderTest {

  @LocalServerPort int port;

  @MockBean ProfileDao profileDao;
  @MockBean SamService samService;

  @MockBean AuthenticatedUserRequestFactory authenticatedUserRequestFactory;
  @Mock AuthenticatedUserRequest userRequest;

  // These mocks are just here so that spring can instantiate beans for the ApplicationContext
  @MockBean AzureCrlService azureCrlService;
  @MockBean GcpCrlService gcpCrlService;
  @MockBean JobService jobService;
  @MockBean CacheManager cacheManager;

  // jdbcTemplate beans are used for profileStatusService when checking CloudSQL status
  @MockBean NamedParameterJdbcTemplate namedParameterJdbcTemplate;
  @MockBean JdbcTemplate jdbcTemplate;
  @Autowired ProfileStatusService profileStatusService;
  @MockBean AzureSpendReportingService azureSpendReportingService;
  @MockBean AzureService azureService;

  @BeforeEach
  void setUp(PactVerificationContext context) {
    when(authenticatedUserRequestFactory.from(any())).thenReturn(userRequest);
    context.setTarget(new HttpTestTarget("localhost", port, "/"));
  }

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider.class)
  void verifyPact(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @State("BPM is ok")
  void getBpmStatus() {
    when(samService.status()).thenReturn(new SystemStatusSystems().ok(true));
    // For CloudSQL return value
    doReturn(jdbcTemplate).when(namedParameterJdbcTemplate).getJdbcTemplate();
    doReturn(true).when(jdbcTemplate).execute(any(ConnectionCallback.class));
    // Checks status of services and stores result for the next time status endpoint is called
    profileStatusService.checkStatus();
  }

  @State("a GCP billing profile")
  Map<String, Object> gcpBillingProfileState() throws InterruptedException {
    var profile = ProviderStateData.gcpBillingProfile;
    setUpProfileDaoGets(List.of(profile));
    when(samService.hasActions(any(), eq(SamResourceType.PROFILE), eq(profile.id())))
        .thenReturn(true);
    return Map.of(
        "gcpProfileId", profile.id().toString(),
        "billingAccountId", profile.billingAccountId().get());
  }

  @State("an Azure billing profile")
  Map<String, Object> azureBillingProfileState() throws InterruptedException {
    var profile = ProviderStateData.azureBillingProfile;
    setUpProfileDaoGets(List.of(profile));
    when(samService.hasActions(any(), eq(SamResourceType.PROFILE), eq(profile.id())))
        .thenReturn(true);
    return Map.of(
        "azureProfileId", profile.id().toString(),
        "tenantId", profile.tenantId().get().toString(),
        "subscriptionId", profile.subscriptionId().get().toString(),
        "managedResourceGroupId", profile.managedResourceGroupId().get());
  }

  @State("two billing profiles exist")
  void twoProfilesExistState() throws InterruptedException {
    var profilesIds =
        List.of(
            ProviderStateData.azureBillingProfile.id(), ProviderStateData.gcpBillingProfile.id());
    when(samService.listProfileIds(any())).thenReturn(profilesIds);
    when(profileDao.listBillingProfiles(anyInt(), anyInt(), eq(profilesIds)))
        .thenReturn(
            List.of(ProviderStateData.azureBillingProfile, ProviderStateData.gcpBillingProfile));
  }

  @State("a managed app exists")
  Map<String, Object> managedAppExistsState() throws InterruptedException {
    var profile = ProviderStateData.azureBillingProfile;
    var subscriptionId = profile.subscriptionId().get();
    when(azureService.getAuthorizedManagedAppDeployments(eq(subscriptionId), eq(false), any()))
        .thenReturn(
            List.of(
                new AzureManagedAppModel()
                    .subscriptionId(subscriptionId)
                    .applicationDeploymentName("appDeploymentName")
                    .region("dummyRegion")
                    .assigned(false)
                    .managedResourceGroupId(profile.managedResourceGroupId().get())
                    .tenantId(profile.tenantId().get())));
    return Map.of("subscriptionId", subscriptionId.toString());
  }

  @State("a JobService that supports profile creation")
  Map<String, Object> creationJobServiceExistsState() {
    var jobBuilder = mock(JobBuilder.class);

    var profile = ProviderStateData.azureBillingProfile;

    when(jobService.newJob()).thenReturn(jobBuilder);
    when(jobBuilder.description(anyString())).thenReturn(jobBuilder);
    when(jobBuilder.flightClass(CreateProfileFlight.class)).thenReturn(jobBuilder);
    when(jobBuilder.request(any())).thenReturn(jobBuilder);
    when(jobBuilder.userRequest(any())).thenReturn(jobBuilder);
    when(jobBuilder.submitAndWait(BillingProfile.class)).thenReturn(profile);

    return Map.of(
        "subscriptionId", profile.subscriptionId().get(),
        "tenantId", profile.tenantId().get(),
        "managedResourceGroupId", profile.managedResourceGroupId());
  }

  @State("a JobService that supports profile deletion")
  void deletionJobServiceExistsState() {
    var jobBuilder = mock(JobBuilder.class);
    String jobId = "jobId";

    var profile = ProviderStateData.azureBillingProfile;

    when(jobService.newJob()).thenReturn(jobBuilder);
    when(jobBuilder.submit()).thenReturn(jobId);
    when(jobBuilder.description(anyString())).thenReturn(jobBuilder);
    when(jobBuilder.flightClass(DeleteProfileFlight.class)).thenReturn(jobBuilder);
    when(jobBuilder.userRequest(any())).thenReturn(jobBuilder);
    when(jobBuilder.addParameter(ProfileMapKeys.PROFILE, profile)).thenReturn(jobBuilder);
    when(jobBuilder.addParameter(
            eq(JobMapKeys.CLOUD_PLATFORM.getKeyName()), eq(CloudPlatform.AZURE.name())))
        .thenReturn(jobBuilder);
  }

  @State("a Sam service that supports profile policy member addition")
  Map<String, Object> userPolicyAdditionState() throws InterruptedException {
    var profile = ProviderStateData.azureBillingProfile;
    var userEmail = "userEmail@foo.bar";
    var policyName = "user";
    when(samService.addProfilePolicyMember(any(), eq(profile.id()), eq(policyName), any()))
        .thenReturn(new SamPolicyModel().name(policyName).members(List.of(userEmail)));
    return Map.of(
        "userEmail", userEmail,
        "policyName", policyName);
  }

  @State("a Sam service that supports profile policy member deletion")
  Map<String, Object> userPolicyDeletionState() throws InterruptedException {
    var profile = ProviderStateData.azureBillingProfile;
    var policyName = "user";
    when(samService.deleteProfilePolicyMember(any(), eq(profile.id()), eq(policyName), any()))
        .thenReturn(new SamPolicyModel().name("user").members(List.of()));
    return Map.of("userEmail", "userEmail@foo.bar", "policyName", policyName);
  }

  @State("an Azure spend report service exists")
  Map<String, Object> azureSpendReportServiceState() throws InterruptedException {
    var profile = ProviderStateData.azureBillingProfile;
    doNothing()
        .when(samService)
        .verifyAuthorization(
            any(), eq(SamResourceType.PROFILE), eq(profile.id()), eq(SamAction.READ_SPEND_REPORT));
    var startTimeString = "2023-04-30T16:58:56.389Z";
    var endTimeString = "2023-05-30T16:58:56.389Z";
    var startTime = OffsetDateTime.parse(startTimeString);
    var endTime = OffsetDateTime.parse(endTimeString);
    var spendDataItem =
        new SpendDataItem(
            "microsoft.storage", new BigDecimal("10.55"), "USD", SpendCategoryType.COMPUTE);
    when(azureSpendReportingService.getBillingProfileSpendData(eq(profile), any(), any()))
        .thenReturn(new SpendData(List.of(spendDataItem), startTime, endTime));
    return Map.of("startTime", startTimeString, "endTime", endTimeString);
  }

  /**
   * Set up the mock profile dao so that it returns any profile in existingProfiles when queried by
   * its id. Note: this won't work for combining states - we'll need a different solution for that.
   */
  void setUpProfileDaoGets(List<BillingProfile> existingProfiles) {
    when(profileDao.getBillingProfileById(
            argThat(argument -> existingProfiles.stream().noneMatch(p -> p.id().equals(argument)))))
        .thenAnswer(
            args -> {
              throw new ProfileNotFoundException(
                  "Profile not found for id: " + args.getArgument(0).toString());
            });
    existingProfiles.forEach(p -> when(profileDao.getBillingProfileById(p.id())).thenReturn(p));
  }
}
