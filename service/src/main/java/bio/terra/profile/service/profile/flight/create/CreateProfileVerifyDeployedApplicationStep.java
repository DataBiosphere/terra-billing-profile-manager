package bio.terra.profile.service.profile.flight.create;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.service.azure.AzureService;
import bio.terra.profile.service.profile.exception.InaccessibleApplicationDeploymentException;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import java.util.Objects;

/** Step to verify the user has access to an Azure profile's managed resource group. */
record CreateProfileVerifyDeployedApplicationStep(
    AzureService azureService, BillingProfile profile, AuthenticatedUserRequest user)
    implements Step {

  @Override
  public StepResult doStep(FlightContext context) {
    final boolean canAccess;
    try {
      var apps =
          azureService.getAuthorizedManagedAppDeployments(profile.subscriptionId().get(), user);
      canAccess =
          apps.stream()
                  .filter(
                      app ->
                          Objects.equals(
                                  app.getManagedResourceGroupId(),
                                  profile.managedResourceGroupId().get())
                              && app.getSubscriptionId() == profile.subscriptionId().get())
                  .count()
              == 1;
    } catch (Exception e) {
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }

    if (!canAccess) {
      throw new InaccessibleApplicationDeploymentException(
          String.format(
              "The user [%s] needs access to deployed application [%s] to perform the requested operation",
              user.getEmail(), profile.managedResourceGroupId().get()));
    }

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    // Verify account has no side effects to clean up
    return StepResult.getStepResultSuccess();
  }
}
