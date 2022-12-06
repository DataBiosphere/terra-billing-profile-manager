package bio.terra.profile.service.profile.flight.delete;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;

public class UnlinkBillingProfileIdFromMrgStep implements Step {

  private final SamService samService;
  private final BillingProfile profile;
  private final AuthenticatedUserRequest userRequest;

  public UnlinkBillingProfileIdFromMrgStep(
      SamService samService, BillingProfile profile, AuthenticatedUserRequest userRequest) {
    this.samService = samService;
    this.profile = profile;
    this.userRequest = userRequest;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException {
    samService.deleteManagedResourceGroup(profile.id(), userRequest);
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    samService.createManagedResourceGroup(profile, userRequest);
    return StepResult.getStepResultSuccess();
  }
}
