package bio.terra.profile.service.profile.flight.create;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.service.gcp.GcpService;
import bio.terra.profile.service.gcp.exception.InaccessibleBillingAccountException;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import java.util.Optional;

/** Step to verify the user has access to a GCP profile's billing account. */
public record CreateProfileVerifyAccountStep(
    GcpService gcpService, Optional<String> billingAccountId, AuthenticatedUserRequest user)
    implements Step {

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException {
    try {
      gcpService.verifyUserBillingAccountAccess(billingAccountId, user);
      gcpService.verifySABillingAccountAccess(billingAccountId);
    } catch (InaccessibleBillingAccountException e) {
      throw e;
    } catch (Exception e) {
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    // Verify account has no side effects to clean up
    return StepResult.getStepResultSuccess();
  }
}
