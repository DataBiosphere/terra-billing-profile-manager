package bio.terra.profile.pact.provider;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.iam.AuthenticatedUserRequestFactory;
import bio.terra.profile.app.Main;
import bio.terra.profile.app.controller.UnauthenticatedApiController;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.service.crl.AzureCrlService;
import bio.terra.profile.service.crl.GcpCrlService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.iam.model.SamResourceType;
import bio.terra.profile.service.job.JobService;
import bio.terra.profile.service.profile.exception.ProfileNotFoundException;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.profile.service.status.ProfileStatusService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;

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

  @MockBean UnauthenticatedApiController unauthenticatedApiController;

  @MockBean AuthenticatedUserRequestFactory authenticatedUserRequestFactory;
  @Mock AuthenticatedUserRequest userRequest;

  // These mocks are just here so that spring can instantiate beans for the ApplicationContext
  @MockBean AzureCrlService azureCrlService;
  @MockBean GcpCrlService gcpCrlService;
  @MockBean JobService jobService;
  @MockBean CacheManager cacheManager;
  @MockBean ProfileStatusService profileStatusService;

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