package bio.terra.profile.service.profile.flight.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.model.AzureManagedAppModel;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.azure.AzureService;
import bio.terra.profile.service.profile.exception.InaccessibleApplicationDeploymentException;
import bio.terra.profile.service.profile.exception.MissingRequiredProvidersException;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class CreateProfileVerifyDeployedApplicationStepTest extends BaseSpringUnitTest {

  @Mock private FlightContext flightContext;
  @Mock private AzureService azureService;

  private AuthenticatedUserRequest user;
  private BillingProfile profile;
  private CreateProfileVerifyDeployedApplicationStep step;

  public CreateProfileVerifyDeployedApplicationStepTest() {}

  @BeforeEach
  void before() {
    var providers = ImmutableSet.of("fake-namespace");
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
            "creator",
            Optional.empty());
    var azureConfiguration =
        new AzureConfiguration(
            "fake_client", "fake_secret", "fake_tenant", ImmutableSet.of(), providers);
    when(azureService.getRegisteredProviderNamespacesForSubscription(
            profile.getRequiredTenantId(), profile.getRequiredSubscriptionId()))
        .thenReturn(providers);
    step =
        new CreateProfileVerifyDeployedApplicationStep(
            azureService, profile, azureConfiguration, user);
  }

  @Test
  void verifyManagedApp() {
    when(azureService.getAuthorizedManagedAppDeployments(
            profile.subscriptionId().get(), true, user))
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
  void verifyManagedAppNoAccess() {
    assertThrows(
        InaccessibleApplicationDeploymentException.class, () -> step.doStep(flightContext));
  }

  @Test
  void missingProviders() {
    when(azureService.getAuthorizedManagedAppDeployments(
            profile.subscriptionId().get(), true, user))
        .thenReturn(
            List.of(
                new AzureManagedAppModel()
                    .subscriptionId(profile.subscriptionId().get())
                    .tenantId(profile.tenantId().get())
                    .managedResourceGroupId(profile.managedResourceGroupId().get())));
    when(azureService.getRegisteredProviderNamespacesForSubscription(
            profile.getRequiredTenantId(), profile.getRequiredSubscriptionId()))
        .thenReturn(ImmutableSet.of());
    assertThrows(MissingRequiredProvidersException.class, () -> step.doStep(flightContext));
  }
}
