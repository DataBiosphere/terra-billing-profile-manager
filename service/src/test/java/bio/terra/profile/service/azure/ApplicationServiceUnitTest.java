package bio.terra.profile.service.azure;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.service.crl.CrlService;
import bio.terra.profile.service.profile.exception.DuplicateTagException;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class ApplicationServiceUnitTest extends BaseUnitTest {

  @Test
  public void addTagToMrg() {
    var tenantId = UUID.randomUUID();
    var subId = UUID.randomUUID();
    var mrgId = "fake_mrg_id";
    var crlService = mock(CrlService.class);
    var resourceGroup = mock(ResourceGroup.class);
    when(crlService.getResourceGroup(eq(tenantId), eq(subId), eq(mrgId))).thenReturn(resourceGroup);
    var applicationService = new ApplicationService(crlService);

    applicationService.addTagToMrg(tenantId, subId, mrgId, "fake_tag", "fake_value");

    verify(crlService)
        .updateTagsForResource(
            eq(tenantId), eq(subId), eq(mrgId), eq(Map.of("fake_tag", "fake_value")));
  }

  @Test
  public void addTagToMrg_failsWhenTagExists() {
    var tenantId = UUID.randomUUID();
    var subId = UUID.randomUUID();
    var mrgId = "fake_mrg_id";
    var crlService = mock(CrlService.class);
    var resourceGroup = mock(ResourceGroup.class);
    when(resourceGroup.tags()).thenReturn(Map.of("fake_tag", "fake_value"));
    when(crlService.getResourceGroup(eq(tenantId), eq(subId), eq(mrgId))).thenReturn(resourceGroup);
    var applicationService = new ApplicationService(crlService);

    assertThrows(
        DuplicateTagException.class,
        () -> applicationService.addTagToMrg(tenantId, subId, mrgId, "fake_tag", "fake_value"));

    verify(crlService, times(0).description("Should not attempt to add a tag if there is a dupe"))
        .updateTagsForResource(any(), any(), any(), any());
  }
}
