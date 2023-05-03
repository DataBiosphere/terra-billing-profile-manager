package bio.terra.profile.service.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.service.azure.exception.InaccessibleSubscriptionException;
import bio.terra.profile.service.crl.AzureCloudResources;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluent.SubscriptionClient;
import com.azure.resourcemanager.resources.fluent.SubscriptionsClient;
import com.azure.resourcemanager.resources.fluent.models.SubscriptionInner;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplicationServiceUnitTest extends BaseUnitTest {

  @Test
  void getTenantForSubscription_accessibleSubscription() {
    var expectedTenantId = UUID.randomUUID();
    var subscriptionId = UUID.randomUUID();

    // mocked chain of nested Azure objects
    var subscription = mock(SubscriptionInner.class);
    when(subscription.tenantId()).thenReturn(expectedTenantId.toString());
    var subscriptionsClient = mock(SubscriptionsClient.class);
    when(subscriptionsClient.get(subscriptionId.toString())).thenReturn(subscription);
    var subscriptionClient = mock(SubscriptionClient.class);
    when(subscriptionClient.getSubscriptions()).thenReturn(subscriptionsClient);
    var resourceManager = mock(ResourceManager.class);
    when(resourceManager.subscriptionClient()).thenReturn(subscriptionClient);

    var crlService = mock(AzureCloudResources.class);
    when(crlService.getResourceManager(subscriptionId)).thenReturn(resourceManager);
    var applicationService = new ApplicationService(crlService);

    var result = applicationService.getTenantForSubscription(subscriptionId);

    assertEquals(result, expectedTenantId);
  }

  @Test
  void getTenantForSubscription_inaccessibleSubscription() {
    var subscriptionId = UUID.randomUUID();
    var crlService = mock(AzureCloudResources.class);
    when(crlService.getResourceManager(subscriptionId))
        .thenThrow(
            new ManagementException(
                "error",
                null,
                new ManagementError(ApplicationService.AZURE_SUB_NOT_FOUND, "not found")));
    var applicationService = new ApplicationService(crlService);

    assertThrows(
        InaccessibleSubscriptionException.class,
        () -> applicationService.getTenantForSubscription(subscriptionId));
  }

  @Test
  void getTenantForSubscription_otherMgmtError() {
    var subscriptionId = UUID.randomUUID();

    var crlService = mock(AzureCloudResources.class);
    when(crlService.getResourceManager(subscriptionId))
        .thenThrow(
            new ManagementException("error", null, new ManagementError("ExampleError", "example")));
    var applicationService = new ApplicationService(crlService);
    assertThrows(
        ManagementException.class,
        () -> applicationService.getTenantForSubscription(subscriptionId));
  }
}
