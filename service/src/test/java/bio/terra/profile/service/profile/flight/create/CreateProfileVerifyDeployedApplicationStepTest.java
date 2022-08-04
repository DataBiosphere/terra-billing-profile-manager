package bio.terra.profile.service.profile.flight.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.model.AzureManagedAppModel;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.azure.AzureService;
import bio.terra.profile.service.profile.exception.InaccessibleApplicationDeploymentException;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class CreateProfileVerifyDeployedApplicationStepTest extends BaseSpringUnitTest {

  @Mock private FlightContext flightContext;
  @Mock private AzureService azureService;

  private AuthenticatedUserRequest user;
  private BillingProfile profile;
  private CreateProfileVerifyDeployedApplicationStep step;

  public CreateProfileVerifyDeployedApplicationStepTest() {}

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
            Optional.of("managedResourceGroupId"),
            Instant.now(),
            Instant.now(),
            "creator");
    step = new CreateProfileVerifyDeployedApplicationStep(azureService, profile, user);
  }

  @Test
  public void verifyManagedApp() {
    when(azureService.getAuthorizedManagedAppDeployments(profile.subscriptionId().get(), user))
        .thenReturn(
            List.of(
                new AzureManagedAppModel()
                    .subscriptionId(profile.subscriptionId().get())
                    .tenantId(profile.tenantId().get())
                    .managedResourceGroupId(profile.managedResourceGroupId().get())));
    var result = step.doStep(flightContext);

    assertEquals(StepResult.getStepResultSuccess(), result);
  }

  @Test
  public void verifyManagedAppNoAccess() {

    assertThrows(
        InaccessibleApplicationDeploymentException.class, () -> step.doStep(flightContext));
  }
}
