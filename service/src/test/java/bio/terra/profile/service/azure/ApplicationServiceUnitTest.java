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
}
