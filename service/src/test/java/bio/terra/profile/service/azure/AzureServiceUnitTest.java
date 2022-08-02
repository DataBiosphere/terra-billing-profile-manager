package bio.terra.profile.service.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.model.AzureManagedAppModel;
import com.azure.resourcemanager.managedapplications.models.Application;
import com.azure.resourcemanager.managedapplications.models.Plan;
import java.util.List;
import java.util.Map;
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

    var authedTerraApp = mock(Application.class);
    when(authedTerraApp.plan()).thenReturn(new Plan().withProduct(offerName));
    when(authedTerraApp.parameters())
        .thenReturn(
            Map.of(
                "authorizedTerraUser",
                Map.of("value", String.format("%s,other@example.com", authedUserEmail))));
    when(authedTerraApp.managedResourceGroupId()).thenReturn("mrg_fake1");
    when(authedTerraApp.id()).thenReturn("fake/azure/api/abc/123");
    when(authedTerraApp.name()).thenReturn("fake_app_1");

    var unauthedTerraApp = mock(Application.class);
    when(unauthedTerraApp.plan()).thenReturn(new Plan().withProduct(offerName));
    when(unauthedTerraApp.parameters())
        .thenReturn(Map.of("authorizedTerraUser", Map.of("value", "other@example.com")));
    when(unauthedTerraApp.managedResourceGroupId()).thenReturn("mrg_fake2");
    when(unauthedTerraApp.id()).thenReturn("fake/azure/api/abc/123");

    when(unauthedTerraApp.name()).thenReturn("fake_app_2");

    var otherNonTerraApp = mock(Application.class);
    when(otherNonTerraApp.plan()).thenReturn(new Plan().withProduct("other_offer"));
    when(otherNonTerraApp.managedResourceGroupId()).thenReturn("mrg_fake3");
    when(otherNonTerraApp.id()).thenReturn("fake/azure/api/abc/123");
    when(otherNonTerraApp.name()).thenReturn("fake_app_3");

    var appsList = Stream.of(authedTerraApp, unauthedTerraApp, otherNonTerraApp);
    var appService = mock(ApplicationService.class);
    var tenantId = UUID.randomUUID();
    when(appService.getApplicationsForSubscription(eq(subId))).thenReturn(appsList);
    when(appService.getTenantForSubscription(subId)).thenReturn(tenantId);

    var offer = new AzureConfiguration.AzureApplicationOffer();
    offer.setName("FAKE_APP_NAME");
    offer.setAuthorizedUserKey("authorizedTerraUser");
    var offers = Map.of(offerName, offer);
    var azureService =
        new AzureService(appService, new AzureConfiguration("fake", "fake", "fake", offers));

    var result = azureService.getManagedAppDeployments(subId, user);

    var expected =
        List.of(
            new AzureManagedAppModel()
                .applicationDeploymentName("fake_app_1")
                .tenantId(tenantId)
                .managedResourceGroupId("mrg_fake1")
                .subscriptionId(subId)
                .resourceGroupName("123"));
    assertEquals(result, expected);
  }
}
