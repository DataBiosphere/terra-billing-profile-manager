package bio.terra.profile.service.profile.flight.create;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.service.crl.CrlService;
import bio.terra.profile.service.profile.exception.InaccessibleApplicationDeploymentException;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import java.util.Map;
import java.util.UUID;

/** Step to verify the user has access to an Azure profile's managed resource group. */
record CreateProfileVerifyDeployedApplicationStep(
    CrlService crlService, BillingProfile profile, AuthenticatedUserRequest user) implements Step {

  private static final String DEPLOYED_APPLICATION_RESOURCE_ID =
      "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Solutions/applications/%s";

  @Override
  public StepResult doStep(FlightContext context) {
    final String applicationResourceId =
        getApplicationDeploymentId(
            profile.subscriptionId().get(),
            profile.resourceGroupName().get(),
            profile.applicationDeploymentName().get());

    final boolean canAccess;
    try {
      var resourceManager =
          crlService.getResourceManager(profile.tenantId().get(), profile.subscriptionId().get());
      var applicationDeployment = resourceManager.genericResources().getById(applicationResourceId);
      var properties =
          (Map<String, Map<String, Map<String, String>>>) applicationDeployment.properties();
      canAccess =
          properties
              .get("parameters")
              .get("authorizedTerraUser")
              .get("value")
              .equalsIgnoreCase(user.getEmail());
    } catch (Exception e) {
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }

    if (!canAccess) {
      throw new InaccessibleApplicationDeploymentException(
          String.format(
              "The user [%s] needs access to deployed application [%s] to perform the requested operation",
              user.getEmail(), applicationResourceId));
    }

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    // Verify account has no side effects to clean up
    return StepResult.getStepResultSuccess();
  }

  /**
   * Return the Azure resource ID for the application deployment associated with the specified
   * parameters.
   *
   * @param subscriptionId The ID of the subscription into which the application is deployed
   * @param resourceGroupName The name of the resource group into which the application is deployed
   * @param applicationDeploymentName The name of the application deployment
   * @return Azure resource identifier
   */
  private static String getApplicationDeploymentId(
      UUID subscriptionId, String resourceGroupName, String applicationDeploymentName) {
    return String.format(
        DEPLOYED_APPLICATION_RESOURCE_ID,
        subscriptionId,
        resourceGroupName,
        applicationDeploymentName);
  }
}
