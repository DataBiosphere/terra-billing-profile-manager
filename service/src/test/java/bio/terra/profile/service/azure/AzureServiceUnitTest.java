package bio.terra.profile.service.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.AzureManagedAppModel;
import bio.terra.profile.service.crl.CrlService;
import com.azure.resourcemanager.managedapplications.models.Application;
import com.azure.resourcemanager.managedapplications.models.Plan;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class AzureServiceUnitTest extends BaseUnitTest {

  UUID subId = UUID.randomUUID();
  UUID tenantId = UUID.randomUUID();
  static String authedUserEmail = "profile@example.com";
  AuthenticatedUserRequest user =
      AuthenticatedUserRequest.builder()
          .setSubjectId("12345")
          .setEmail(authedUserEmail)
          .setToken("token")
          .build();
  String offerName = "known_terra_offer";
  String offerPublisher = "known_terra_publisher";
  String authorizedUserKey = "authorizedTerraUser";
  String regionName = "application-region";

  @Test
  public void getManagedApps() {
    var authedTerraApp = mock(Application.class);
    mockApplicationCalls(
        authedTerraApp,
        offerName,
        offerPublisher,
        Optional.of(authedUserEmail),
        "mrg_fake1",
        "fake_app_1");

    var unauthedTerraApp = mock(Application.class);
    mockApplicationCalls(
        unauthedTerraApp, offerName, offerPublisher, Optional.empty(), "mrg_fake2", "fake_app_2");

    var otherNonTerraApp = mock(Application.class);
    mockApplicationCalls(
        otherNonTerraApp,
        "other_offer",
        offerPublisher,
        Optional.empty(),
        "mrg_fake3",
        "fake_app3");

    var differentPublisherApp = mock(Application.class);
    mockApplicationCalls(
        differentPublisherApp,
        offerName,
        "other_publisher",
        Optional.of(authedUserEmail),
        "mrg_fake1",
        "fake_app_1");

    var appsList =
        Stream.of(authedTerraApp, unauthedTerraApp, otherNonTerraApp, differentPublisherApp);
    var appService = mock(ApplicationService.class);
    when(appService.getApplicationsForSubscription(eq(subId))).thenReturn(appsList);
    when(appService.getTenantForSubscription(subId)).thenReturn(tenantId);

    var crlService = mock(CrlService.class);
    var profileDao = mock(ProfileDao.class);

    var offer = new AzureConfiguration.AzureApplicationOffer();
    offer.setName(offerName);
    offer.setPublisher(offerPublisher);
    offer.setAuthorizedUserKey(authorizedUserKey);
    var offers = Set.of(offer);
    var azureService =
        new AzureService(
            crlService,
            appService,
            new AzureConfiguration("fake", "fake", "fake", offers, ImmutableSet.of()),
            profileDao);

    var result = azureService.getAuthorizedManagedAppDeployments(subId, true, user);

    var expected =
        List.of(
            new AzureManagedAppModel()
                .applicationDeploymentName("fake_app_1")
                .tenantId(tenantId)
                .managedResourceGroupId("mrg_fake1")
                .subscriptionId(subId)
                .assigned(false)
                .region(regionName));
    assertEquals(result, expected);
  }

  @Test
  public void getManagedApps_dedupesApps() {
    var crlService = mock(CrlService.class);
    var profileDao = mock(ProfileDao.class);

    var authedTerraApp = mock(Application.class);
    mockApplicationCalls(
        authedTerraApp,
        offerName,
        offerPublisher,
        Optional.of(authedUserEmail),
        "mrg_fake1",
        "fake_app_1");

    var appsList = Stream.of(authedTerraApp, authedTerraApp);
    var appService = mock(ApplicationService.class);
    when(appService.getApplicationsForSubscription(eq(subId))).thenReturn(appsList);
    when(appService.getTenantForSubscription(subId)).thenReturn(tenantId);

    var offer = new AzureConfiguration.AzureApplicationOffer();
    offer.setName(offerName);
    offer.setPublisher(offerPublisher);
    offer.setAuthorizedUserKey(authorizedUserKey);
    var offers = Set.of(offer);
    var azureService =
        new AzureService(
            crlService,
            appService,
            new AzureConfiguration("fake", "fake", "fake", offers, ImmutableSet.of()),
            profileDao);

    var result = azureService.getAuthorizedManagedAppDeployments(subId, true, user);

    assertEquals(result.size(), 1, "Duplicate app instances should be removed");
  }

  @Test
  public void getManagedApps_includeAssignedApplications() {
    var applicationName = "fake_app_1";
    var assignedTerraAppManagedResourceGroupId = "assigned_fake_mrg";
    var unassignedTerraAppManagedResourceGroupId = "unassigned_fake_mrg";

    var crlService = mock(CrlService.class);
    var profileDao = mock(ProfileDao.class);
    when(profileDao.listManagedResourceGroupsInSubscription(subId))
        .thenReturn(List.of(assignedTerraAppManagedResourceGroupId));

    var assignedTerraApp = mock(Application.class);
    mockApplicationCalls(
        assignedTerraApp,
        offerName,
        offerPublisher,
        Optional.of(authedUserEmail),
        assignedTerraAppManagedResourceGroupId,
        applicationName);

    var unassignedTerraApp = mock(Application.class);
    mockApplicationCalls(
        unassignedTerraApp,
        offerName,
        offerPublisher,
        Optional.of(authedUserEmail),
        unassignedTerraAppManagedResourceGroupId,
        applicationName);

    var appsList = Stream.of(assignedTerraApp, unassignedTerraApp);
    var appService = mock(ApplicationService.class);
    when(appService.getApplicationsForSubscription(eq(subId))).thenReturn(appsList);
    when(appService.getTenantForSubscription(subId)).thenReturn(tenantId);

    var offer = new AzureConfiguration.AzureApplicationOffer();
    offer.setName(offerName);
    offer.setPublisher(offerPublisher);
    offer.setAuthorizedUserKey(authorizedUserKey);
    var offers = Set.of(offer);
    var azureService =
        new AzureService(
            crlService,
            appService,
            new AzureConfiguration("fake", "fake", "fake", offers, ImmutableSet.of()),
            profileDao);

    AzureManagedAppModel assignedAzureManagedAppModel =
        new AzureManagedAppModel()
            .tenantId(tenantId)
            .subscriptionId(subId)
            .managedResourceGroupId(assignedTerraAppManagedResourceGroupId)
            .applicationDeploymentName(applicationName)
            .assigned(true)
            .region(regionName);

    AzureManagedAppModel unassignedAzureManagedAppModel =
        new AzureManagedAppModel()
            .tenantId(tenantId)
            .subscriptionId(subId)
            .managedResourceGroupId(unassignedTerraAppManagedResourceGroupId)
            .applicationDeploymentName(applicationName)
            .assigned(false)
            .region(regionName);

    var includeAssignedResult = azureService.getAuthorizedManagedAppDeployments(subId, true, user);
    assertEquals(
        List.of(assignedAzureManagedAppModel, unassignedAzureManagedAppModel),
        includeAssignedResult);

    appsList = Stream.of(assignedTerraApp, unassignedTerraApp);
    when(appService.getApplicationsForSubscription(eq(subId))).thenReturn(appsList);

    var excludeAssignedResult = azureService.getAuthorizedManagedAppDeployments(subId, false, user);
    assertEquals(List.of(unassignedAzureManagedAppModel), excludeAssignedResult);
  }

  private static Stream<Arguments> getAuthorizedEmails() {
    return Stream.of(
            Arguments.of("foo@bar.com, " + authedUserEmail),
            Arguments.of(StringUtils.swapCase(authedUserEmail)));
  }
  @ParameterizedTest
  @MethodSource("getAuthorizedEmails")
  public void getManagedApps_handlesDifferentEmailFormats(String authorizedEmails) {
    var authedTerraApp = mock(Application.class);
    mockApplicationCalls(
            authedTerraApp,
            offerName,
            offerPublisher,
            Optional.of(authorizedEmails),
            "mrg_fake1",
            "fake_app_1");

    var appsList = Stream.of(authedTerraApp);
    var appService = mock(ApplicationService.class);
    when(appService.getApplicationsForSubscription(eq(subId))).thenReturn(appsList);
    when(appService.getTenantForSubscription(subId)).thenReturn(tenantId);

    var crlService = mock(CrlService.class);
    var profileDao = mock(ProfileDao.class);

    var offer = new AzureConfiguration.AzureApplicationOffer();
    offer.setName(offerName);
    offer.setPublisher(offerPublisher);
    offer.setAuthorizedUserKey(authorizedUserKey);
    var offers = Set.of(offer);
    var azureService =
            new AzureService(
                    crlService,
                    appService,
                    new AzureConfiguration("fake", "fake", "fake", offers, ImmutableSet.of()),
                    profileDao);

    var result = azureService.getAuthorizedManagedAppDeployments(subId, true, user);

    var expected =
            List.of(
                    new AzureManagedAppModel()
                            .applicationDeploymentName("fake_app_1")
                            .tenantId(tenantId)
                            .managedResourceGroupId("mrg_fake1")
                            .subscriptionId(subId)
                            .assigned(false)
                            .region(regionName));
    assertEquals(expected, result);
  }

  private void mockApplicationCalls(
      Application application,
      String offerName,
      String offerPublisher,
      Optional<String> authedUserEmail,
      String managedResourceGroupId,
      String applicationName) {
    when(application.plan())
        .thenReturn(new Plan().withProduct(offerName).withPublisher(offerPublisher));

    String authorizedUsers;
    if (authedUserEmail.isPresent()) {
      authorizedUsers = String.format("%s,other@example.com", authedUserEmail.get());
    } else {
      authorizedUsers = "other@example.com";
    }
    when(application.parameters())
        .thenReturn(Map.of(authorizedUserKey, Map.of("value", authorizedUsers)));
    when(application.managedResourceGroupId()).thenReturn(managedResourceGroupId);
    when(application.name()).thenReturn(applicationName);
    when(application.regionName()).thenReturn(regionName);
  }
}
