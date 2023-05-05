package bio.terra.profile.service.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.AzureManagedAppModel;
import bio.terra.profile.service.azure.exception.InaccessibleSubscriptionException;
import bio.terra.profile.service.crl.AzureCrlService;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.managedapplications.ApplicationManager;
import com.azure.resourcemanager.managedapplications.models.Application;
import com.azure.resourcemanager.managedapplications.models.Plan;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluent.SubscriptionClient;
import com.azure.resourcemanager.resources.fluent.SubscriptionsClient;
import com.azure.resourcemanager.resources.fluent.models.SubscriptionInner;
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
  private static String authedUserEmail = "profile@example.com";
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

  private ApplicationManager mockApplicationManager(List<Application> apps) {
    var appsIter = mock(PagedIterable.class);
    when(appsIter.stream()).thenReturn(apps.stream());
    var applications =
        mock(com.azure.resourcemanager.managedapplications.models.Applications.class);
    when(applications.list()).thenReturn(appsIter);
    var manager = mock(ApplicationManager.class);
    when(manager.applications()).thenReturn(applications);
    return manager;
  }

  private ResourceManager mockResourceManager(UUID subscriptionId, UUID tenantId) {
    var subscription = mock(SubscriptionInner.class);
    when(subscription.tenantId()).thenReturn(tenantId.toString());
    var subscriptionsClient = mock(SubscriptionsClient.class);
    when(subscriptionsClient.get(subscriptionId.toString())).thenReturn(subscription);
    var subscriptionClient = mock(SubscriptionClient.class);
    when(subscriptionClient.getSubscriptions()).thenReturn(subscriptionsClient);
    var resourceManager = mock(ResourceManager.class);
    when(resourceManager.subscriptionClient()).thenReturn(subscriptionClient);
    return resourceManager;
  }

  private Application mockApplicationCalls(
      String offerName,
      String offerPublisher,
      Optional<String> authedUserEmail,
      String managedResourceGroupId,
      String applicationName) {
    var application = mock(Application.class);
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
    return application;
  }

  @Test
  public void getManagedApps() {
    var authedTerraApp =
        mockApplicationCalls(
            offerName, offerPublisher, Optional.of(authedUserEmail), "mrg_fake1", "fake_app_1");

    var unauthedTerraApp =
        mockApplicationCalls(
            offerName, offerPublisher, Optional.empty(), "mrg_fake2", "fake_app_2");

    var otherNonTerraApp =
        mockApplicationCalls(
            "other_offer", offerPublisher, Optional.empty(), "mrg_fake3", "fake_app3");

    var differentPublisherApp =
        mockApplicationCalls(
            offerName, "other_publisher", Optional.of(authedUserEmail), "mrg_fake1", "fake_app_1");

    var appsList =
        List.of(authedTerraApp, unauthedTerraApp, otherNonTerraApp, differentPublisherApp);

    var crlService = mock(AzureCrlService.class);
    var appManager = mockApplicationManager(appsList);
    when(crlService.getApplicationManager(subId)).thenReturn(appManager);
    var resourceManager = mockResourceManager(subId, tenantId);
    when(crlService.getResourceManager(subId)).thenReturn(resourceManager);

    var profileDao = mock(ProfileDao.class);

    var offer = new AzureConfiguration.AzureApplicationOffer();
    offer.setName(offerName);
    offer.setPublisher(offerPublisher);
    offer.setAuthorizedUserKey(authorizedUserKey);
    var offers = Set.of(offer);
    var azureService =
        new AzureService(
            crlService,
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
    var crlService = mock(AzureCrlService.class);
    var profileDao = mock(ProfileDao.class);

    var authedTerraApp =
        mockApplicationCalls(
            offerName, offerPublisher, Optional.of(authedUserEmail), "mrg_fake1", "fake_app_1");

    var appsList = List.of(authedTerraApp, authedTerraApp);

    var applicationManager = mockApplicationManager(appsList);
    when(crlService.getApplicationManager(subId)).thenReturn(applicationManager);
    var resourceManager = mockResourceManager(subId, tenantId);
    when(crlService.getResourceManager(subId)).thenReturn(resourceManager);

    var offer = new AzureConfiguration.AzureApplicationOffer();
    offer.setName(offerName);
    offer.setPublisher(offerPublisher);
    offer.setAuthorizedUserKey(authorizedUserKey);
    var offers = Set.of(offer);
    var azureService =
        new AzureService(
            crlService,
            new AzureConfiguration("fake", "fake", "fake", offers, ImmutableSet.of()),
            profileDao);

    var result = azureService.getAuthorizedManagedAppDeployments(subId, true, user);

    assertEquals(result.size(), 1, "Duplicate app instances should be removed");
  }

  @Test
  public void getManagedApps_excludingAssignedApplications() {
    var applicationName = "fake_app_1";
    var assignedTerraAppManagedResourceGroupId = "assigned_fake_mrg";
    var unassignedTerraAppManagedResourceGroupId = "unassigned_fake_mrg";

    var profileDao = mock(ProfileDao.class);
    when(profileDao.listManagedResourceGroupsInSubscription(subId))
        .thenReturn(List.of(assignedTerraAppManagedResourceGroupId));

    var assignedTerraApp =
        mockApplicationCalls(
            offerName,
            offerPublisher,
            Optional.of(authedUserEmail),
            assignedTerraAppManagedResourceGroupId,
            applicationName);

    var unassignedTerraApp =
        mockApplicationCalls(
            offerName,
            offerPublisher,
            Optional.of(authedUserEmail),
            unassignedTerraAppManagedResourceGroupId,
            applicationName);

    var offer = new AzureConfiguration.AzureApplicationOffer();
    offer.setName(offerName);
    offer.setPublisher(offerPublisher);
    offer.setAuthorizedUserKey(authorizedUserKey);
    var offers = Set.of(offer);

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

    var appsList = List.of(assignedTerraApp, unassignedTerraApp);
    var crlService = mock(AzureCrlService.class);
    var applicationManager = mockApplicationManager(appsList);
    when(crlService.getApplicationManager(subId)).thenReturn(applicationManager);
    var resourceManager = mockResourceManager(subId, tenantId);
    when(crlService.getResourceManager(subId)).thenReturn(resourceManager);

    var azureService =
        new AzureService(
            crlService,
            new AzureConfiguration("fake", "fake", "fake", offers, ImmutableSet.of()),
            profileDao);

    var result = azureService.getAuthorizedManagedAppDeployments(subId, false, user);
    assertEquals(List.of(unassignedAzureManagedAppModel), result);
  }

  @Test
  public void getManagedApps_includingAssignedApplications() {
    var applicationName = "fake_app_1";
    var assignedTerraAppManagedResourceGroupId = "assigned_fake_mrg";
    var unassignedTerraAppManagedResourceGroupId = "unassigned_fake_mrg";

    var profileDao = mock(ProfileDao.class);
    when(profileDao.listManagedResourceGroupsInSubscription(subId))
        .thenReturn(List.of(assignedTerraAppManagedResourceGroupId));

    var assignedTerraApp =
        mockApplicationCalls(
            offerName,
            offerPublisher,
            Optional.of(authedUserEmail),
            assignedTerraAppManagedResourceGroupId,
            applicationName);

    var unassignedTerraApp =
        mockApplicationCalls(
            offerName,
            offerPublisher,
            Optional.of(authedUserEmail),
            unassignedTerraAppManagedResourceGroupId,
            applicationName);

    var appsList = List.of(assignedTerraApp, unassignedTerraApp);
    var crlService = mock(AzureCrlService.class);
    var applicationManager = mockApplicationManager(appsList);
    when(crlService.getApplicationManager(subId)).thenReturn(applicationManager);
    var resourceManager = mockResourceManager(subId, tenantId);
    when(crlService.getResourceManager(subId)).thenReturn(resourceManager);

    var offer = new AzureConfiguration.AzureApplicationOffer();
    offer.setName(offerName);
    offer.setPublisher(offerPublisher);
    offer.setAuthorizedUserKey(authorizedUserKey);
    var offers = Set.of(offer);
    var azureService =
        new AzureService(
            crlService,
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

    var result = azureService.getAuthorizedManagedAppDeployments(subId, true, user);
    assertEquals(List.of(assignedAzureManagedAppModel, unassignedAzureManagedAppModel), result);
  }

  private static Stream<Arguments> getAuthorizedEmails() {
    return Stream.of(
        Arguments.of("foo@bar.com, ".concat(authedUserEmail)),
        Arguments.of(StringUtils.swapCase(authedUserEmail)));
  }

  @ParameterizedTest
  @MethodSource("getAuthorizedEmails")
  public void getManagedApps_handlesDifferentEmailFormats(String authorizedEmails) {
    var authedTerraApp =
        mockApplicationCalls(
            offerName, offerPublisher, Optional.of(authorizedEmails), "mrg_fake1", "fake_app_1");

    var appsList = List.of(authedTerraApp);
    var crlService = mock(AzureCrlService.class);
    var applicationManager = mockApplicationManager(appsList);
    when(crlService.getApplicationManager(subId)).thenReturn(applicationManager);
    var resourceManager = mockResourceManager(subId, tenantId);
    when(crlService.getResourceManager(subId)).thenReturn(resourceManager);

    var profileDao = mock(ProfileDao.class);

    var offer = new AzureConfiguration.AzureApplicationOffer();
    offer.setName(offerName);
    offer.setPublisher(offerPublisher);
    offer.setAuthorizedUserKey(authorizedUserKey);
    var offers = Set.of(offer);
    var azureService =
        new AzureService(
            crlService,
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

  @Test
  void getTenantForSubscription_inaccessibleSubscription() {
    var subscriptionId = UUID.randomUUID();
    var crlService = mock(AzureCrlService.class);
    when(crlService.getResourceManager(subscriptionId))
        .thenThrow(
            new ManagementException(
                "error", null, new ManagementError(AzureService.AZURE_SUB_NOT_FOUND, "not found")));

    var azureService =
        new AzureService(
            crlService,
            new AzureConfiguration("fake", "fake", "fake", ImmutableSet.of(), ImmutableSet.of()),
            mock(ProfileDao.class));
    assertThrows(
        InaccessibleSubscriptionException.class,
        () -> azureService.getAuthorizedManagedAppDeployments(subscriptionId, false, user));
  }

  @Test
  void getTenantForSubscription_otherMgmtError() {
    var subscriptionId = UUID.randomUUID();

    var crlService = mock(AzureCrlService.class);
    when(crlService.getResourceManager(subscriptionId))
        .thenThrow(
            new ManagementException("error", null, new ManagementError("ExampleError", "example")));
    var azureService =
        new AzureService(
            crlService,
            new AzureConfiguration("fake", "fake", "fake", ImmutableSet.of(), ImmutableSet.of()),
            mock(ProfileDao.class));

    assertThrows(
        ManagementException.class,
        () -> azureService.getAuthorizedManagedAppDeployments(subscriptionId, false, user));
  }
}
