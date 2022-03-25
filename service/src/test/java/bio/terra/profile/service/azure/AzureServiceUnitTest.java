package bio.terra.profile.service.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.model.AzureManagedAppModel;
import bio.terra.profile.service.crl.CrlService;
import com.azure.core.http.rest.PagedIterable;
import com.azure.resourcemanager.managedapplications.ApplicationManager;
import com.azure.resourcemanager.managedapplications.models.Application;
import com.azure.resourcemanager.managedapplications.models.Applications;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class AzureServiceUnitTest extends BaseUnitTest {

  @Test
  public void getManagedApps() {
    var crlService = mock(CrlService.class);
    var appMgr = mock(ApplicationManager.class);
    var user =
        AuthenticatedUserRequest.builder()
            .setSubjectId("12345")
            .setEmail("profile@unit.com")
            .setToken("token")
            .build();
    var apps = mock(Applications.class);
    var appsList = mock(PagedIterable.class);
    var fakeApp1 = mock(Application.class);
    var fakeApp2 = mock(Application.class);
    var fakeApp3 = mock(Application.class);
    when(fakeApp1.tags()).thenReturn(Map.of("authorizedTerraUser", "profile@unit.com"));
    when(fakeApp1.name()).thenReturn("fake_app_1");
    when(fakeApp2.tags()).thenReturn(Map.of("authorizedTerraUser", "bar@bar.com"));
    when(fakeApp3.tags()).thenReturn(Map.of("unrelatedTag", "unrelated"));
    var apps2 = Arrays.asList(fakeApp1, fakeApp2, fakeApp3);
    when(appsList.stream()).thenReturn(apps2.stream());

    var azureService = new AzureService(crlService);
    when(crlService.getApplicationManager(any(UUID.class))).thenReturn(appMgr);
    when(appMgr.applications()).thenReturn(apps);

    when(apps.list()).thenReturn(appsList);

    var result = azureService.getManagedAppDeployments(UUID.randomUUID(), user);

    var expected = List.of(new AzureManagedAppModel().name("fake_app_1"));
    assertEquals(result, expected);
  }
}
