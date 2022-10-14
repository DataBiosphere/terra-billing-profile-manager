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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class AzureServiceUnitTest extends BaseUnitTest {

  @Test
  public void getManagedApps() {
    var subId = UUID.randomUUID();
    var authedUserEmail = "profile@example.com";
    var user =
        AuthenticatedUserRequest.builder()
            .setSubjectId("12345")
            .setEmail(authedUserEmail)
            .setToken("token")
            .build();
    var offerName = "known_terra_offer";
    var offerPublisher = "known_terra_publisher";

    var authedTerraApp = mock(Application.class);
    when(authedTerraApp.plan())
        .thenReturn(new Plan().withProduct(offerName).withPublisher(offerPublisher));
    when(authedTerraApp.parameters())
        .thenReturn(
            Map.of(
                "authorizedTerraUser",
                Map.of("value", String.format("%s,other@example.com", authedUserEmail))));
    when(authedTerraApp.managedResourceGroupId()).thenReturn("mrg_fake1");
    when(authedTerraApp.name()).thenReturn("fake_app_1");

    var unauthedTerraApp = mock(Application.class);
    when(unauthedTerraApp.plan())
        .thenReturn(new Plan().withProduct(offerName).withPublisher(offerPublisher));
    when(unauthedTerraApp.parameters())
        .thenReturn(Map.of("authorizedTerraUser", Map.of("value", "other@example.com")));
    when(unauthedTerraApp.managedResourceGroupId()).thenReturn("mrg_fake2");
    when(unauthedTerraApp.name()).thenReturn("fake_app_2");

    var otherNonTerraApp = mock(Application.class);
    when(otherNonTerraApp.plan())
        .thenReturn(new Plan().withProduct("other_offer").withPublisher(offerPublisher));
    when(otherNonTerraApp.managedResourceGroupId()).thenReturn("mrg_fake3");
    when(otherNonTerraApp.name()).thenReturn("fake_app_3");

    var differentPublisherApp = mock(Application.class);
    when(differentPublisherApp.plan())
        .thenReturn(new Plan().withProduct(offerName).withPublisher("other_publisher"));
    when(differentPublisherApp.parameters())
        .thenReturn(
            Map.of(
                "authorizedTerraUser",
                Map.of("value", String.format("%s,other@example.com", authedUserEmail))));
    when(differentPublisherApp.managedResourceGroupId()).thenReturn("mrg_fake1");
    when(differentPublisherApp.name()).thenReturn("fake_app_1");

    var appsList =
        Stream.of(authedTerraApp, unauthedTerraApp, otherNonTerraApp, differentPublisherApp);
    var appService = mock(ApplicationService.class);
    var tenantId = UUID.randomUUID();
    when(appService.getApplicationsForSubscription(eq(subId))).thenReturn(appsList);
    when(appService.getTenantForSubscription(subId)).thenReturn(tenantId);

    var crlService = mock(CrlService.class);
    var profileDao = mock(ProfileDao.class);

    var offer = new AzureConfiguration.AzureApplicationOffer();
    offer.setName(offerName);
    offer.setPublisher(offerPublisher);
    offer.setAuthorizedUserKey("authorizedTerraUser");
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
                .subscriptionId(subId));
    assertEquals(result, expected);
  }

  @Test
  public void getManagedApps_dedupesApps() {
    var subId = UUID.randomUUID();
    var authedUserEmail = "profile@example.com";
    var user =
        AuthenticatedUserRequest.builder()
            .setSubjectId("12345")
            .setEmail(authedUserEmail)
            .setToken("token")
            .build();
    var offerName = "known_terra_offer";
    var offerPublisher = "known_terra_publisher";

    var crlService = mock(CrlService.class);
    var profileDao = mock(ProfileDao.class);

    var authedTerraApp = mock(Application.class);
    when(authedTerraApp.plan())
        .thenReturn(new Plan().withProduct(offerName).withPublisher(offerPublisher));
    when(authedTerraApp.parameters())
        .thenReturn(
            Map.of(
                "authorizedTerraUser",
                Map.of("value", String.format("%s,other@example.com", authedUserEmail))));
    when(authedTerraApp.managedResourceGroupId()).thenReturn("mrg_fake1");
    when(authedTerraApp.name()).thenReturn("fake_app_1");
    var appsList = Stream.of(authedTerraApp, authedTerraApp);
    var appService = mock(ApplicationService.class);
    var tenantId = UUID.randomUUID();
    when(appService.getApplicationsForSubscription(eq(subId))).thenReturn(appsList);
    when(appService.getTenantForSubscription(subId)).thenReturn(tenantId);

    var offer = new AzureConfiguration.AzureApplicationOffer();
    offer.setName(offerName);
    offer.setPublisher(offerPublisher);
    offer.setAuthorizedUserKey("authorizedTerraUser");
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
}
