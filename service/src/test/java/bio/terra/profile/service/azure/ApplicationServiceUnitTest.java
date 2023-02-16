package bio.terra.profile.service.azure;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.service.azure.exception.InaccessibleSubscriptionException;
import bio.terra.profile.service.crl.CrlService;
import bio.terra.profile.service.profile.exception.DuplicateTagException;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplicationServiceUnitTest extends BaseUnitTest {
  @Test
  void getTenantForSubscription_accessibleSubscription() {
    var crlService = mock(CrlService.class);
    var expectedTenantId = UUID.randomUUID();
    when(crlService.getTenantForSubscription(any(UUID.class))).thenReturn(expectedTenantId);
    var applicationService = new ApplicationService(crlService);

    var result = applicationService.getTenantForSubscription(UUID.randomUUID());

    assert (result.equals(expectedTenantId));
  }

  @Test
  void getTenantForSubscription_inaccessibleSubscription() {
    var crlService = mock(CrlService.class);
    when(crlService.getTenantForSubscription(any(UUID.class)))
        .thenThrow(
            new ManagementException(
                "error",
                null,
                new ManagementError(ApplicationService.AZURE_SUB_NOT_FOUND, "not found")));
    var applicationService = new ApplicationService(crlService);

    assertThrows(
        InaccessibleSubscriptionException.class,
        () -> applicationService.getTenantForSubscription(UUID.randomUUID()));
  }

  @Test
  void getTenantForSubscription_otherMgmtError() {
    var crlService = mock(CrlService.class);
    when(crlService.getTenantForSubscription(any(UUID.class)))
        .thenThrow(
            new ManagementException("error", null, new ManagementError("ExampleError", "example")));
    var applicationService = new ApplicationService(crlService);

    assertThrows(
        ManagementException.class,
        () -> applicationService.getTenantForSubscription(UUID.randomUUID()));
  }

  @Test
  void addTagToMrg() {
    var tenantId = UUID.randomUUID();
    var subId = UUID.randomUUID();
    var mrgId = "fake_mrg_id";
    var crlService = mock(CrlService.class);
    var resourceGroup = mock(ResourceGroup.class);
    var resourceGroupId = UUID.randomUUID().toString();
    when(resourceGroup.id()).thenReturn(resourceGroupId);
    when(crlService.getResourceGroup(tenantId, subId, mrgId)).thenReturn(resourceGroup);
    var applicationService = new ApplicationService(crlService);

    applicationService.addTagToMrg(tenantId, subId, mrgId, "fake_tag", "fake_value");

    verify(crlService)
        .updateTagsForResource(tenantId, subId, resourceGroupId, Map.of("fake_tag", "fake_value"));
  }

  @Test
  void addTagToMrg_failsWhenTagExists() {
    var tenantId = UUID.randomUUID();
    var subId = UUID.randomUUID();
    var mrgId = "fake_mrg_id";
    var crlService = mock(CrlService.class);
    var resourceGroup = mock(ResourceGroup.class);
    when(resourceGroup.tags()).thenReturn(Map.of("fake_tag", "fake_value"));
    when(crlService.getResourceGroup(tenantId, subId, mrgId)).thenReturn(resourceGroup);
    var applicationService = new ApplicationService(crlService);

    assertThrows(
        DuplicateTagException.class,
        () -> applicationService.addTagToMrg(tenantId, subId, mrgId, "fake_tag", "fake_value"));

    verify(crlService, times(0).description("Should not attempt to add a tag if there is a dupe"))
        .updateTagsForResource(any(), any(), any(), any());
  }
}
