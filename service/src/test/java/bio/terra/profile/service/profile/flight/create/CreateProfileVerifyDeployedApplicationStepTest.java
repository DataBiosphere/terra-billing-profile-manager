package bio.terra.profile.service.profile.flight.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.crl.CrlService;
import bio.terra.profile.service.profile.exception.InaccessibleApplicationDeploymentException;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.models.GenericResource;
import com.azure.resourcemanager.resources.models.GenericResources;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class CreateProfileVerifyDeployedApplicationStepTest extends BaseUnitTest {

  @Mock private FlightContext flightContext;
  @Mock private CrlService crlService;
  @Mock private ResourceManager resourceManager;
  @Mock private GenericResources genericResources;
  @Mock private GenericResource genericResource;

  private AuthenticatedUserRequest user;
  private BillingProfile profile;
  private CreateProfileVerifyDeployedApplicationStep step;

  @BeforeEach
  public void before() {
    user =
        AuthenticatedUserRequest.builder()
            .setSubjectId("12345")
            .setEmail("profile@unit.com")
            .setToken("token")
            .build();
    profile =
        new BillingProfile(
            UUID.randomUUID(),
            "name",
            "description",
            "direct",
            CloudPlatform.AZURE,
            Optional.empty(),
            Optional.of(UUID.randomUUID()),
            Optional.of(UUID.randomUUID()),
            Optional.of("resourceGroup"),
            Optional.of("applicationDeployment"),
            Instant.now(),
            Instant.now(),
            "creator");
    step = new CreateProfileVerifyDeployedApplicationStep(crlService, profile, user);

    when(crlService.getResourceManager(
            eq(profile.tenantId().get()), eq(profile.subscriptionId().get())))
        .thenReturn(resourceManager);
    when(resourceManager.genericResources()).thenReturn(genericResources);
    when(genericResources.getById(anyString())).thenReturn(genericResource);
  }

  @Test
  public void verifyManagedApp() {
    when(genericResource.properties())
        .thenReturn(
            Map.of("parameters", Map.of("authorizedTerraUser", Map.of("value", user.getEmail()))));

    var result = step.doStep(flightContext);

    assertEquals(StepResult.getStepResultSuccess(), result);
  }

  @Test
  public void verifyManagedAppNoAccess() {
    when(genericResource.properties())
        .thenReturn(
            Map.of(
                "parameters",
                Map.of("authorizedTerraUser", Map.of("value", "somebody@gmail.com"))));

    assertThrows(
        InaccessibleApplicationDeploymentException.class, () -> step.doStep(flightContext));
  }
}
