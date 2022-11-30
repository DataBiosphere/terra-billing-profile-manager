package bio.terra.profile.service.azure;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.service.crl.CrlService;
import bio.terra.profile.service.profile.exception.DuplicateTagException;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplicationServiceUnitTest extends BaseUnitTest {

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

  @Test
  void removeTagFromMrg() {
    var tenantId = UUID.randomUUID();
    var subId = UUID.randomUUID();
    var mrgId = "fake_mrg_id";
    var crlService = mock(CrlService.class);
    var resourceGroup = mock(ResourceGroup.class);
    var resourceGroupId = UUID.randomUUID().toString();
    when(resourceGroup.id()).thenReturn(resourceGroupId);
    when(resourceGroup.tags()).thenReturn(Map.of("fake_tag", "fake_value"));
    when(crlService.getResourceGroup(tenantId, subId, mrgId)).thenReturn(resourceGroup);
    var applicationService = new ApplicationService(crlService);

    applicationService.removeTagFromMrg(tenantId, subId, mrgId, "fake_tag");

    verify(crlService).updateTagsForResource(tenantId, subId, resourceGroupId, Map.of());
  }

  @Test
  void removeTagFromMrg_doesNothingWhenTagIsMissing() {
    var tenantId = UUID.randomUUID();
    var subId = UUID.randomUUID();
    var mrgId = "fake_mrg_id";
    var crlService = mock(CrlService.class);
    var resourceGroup = mock(ResourceGroup.class);
    var resourceGroupId = UUID.randomUUID().toString();
    when(resourceGroup.id()).thenReturn(resourceGroupId);
    when(resourceGroup.tags()).thenReturn(Map.of());
    when(crlService.getResourceGroup(tenantId, subId, mrgId)).thenReturn(resourceGroup);
    var applicationService = new ApplicationService(crlService);

    applicationService.removeTagFromMrg(tenantId, subId, mrgId, "fake_tag");

    verify(crlService, never().description("Should not attempt to add a tag if there is a dupe"))
        .updateTagsForResource(any(), any(), any(), any());
  }
}
