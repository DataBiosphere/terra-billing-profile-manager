package bio.terra.profile.service.profile.flight.create;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.policy.model.TpsComponent;
import bio.terra.policy.model.TpsObjectType;
import bio.terra.profile.service.policy.TpsApiDispatch;
import bio.terra.profile.service.policy.exception.PolicyServiceDuplicateException;
import bio.terra.profile.service.profile.flight.ProfileMapKeys;
import bio.terra.profile.service.profile.model.ProfileDescription;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

record CreateProfilePoliciesStep(
    TpsApiDispatch tpsApiDispatch, ProfileDescription profile, AuthenticatedUserRequest user)
    implements Step {

  private static final Logger logger = LoggerFactory.getLogger(CreateProfilePoliciesStep.class);

  @Override
  public StepResult doStep(FlightContext flightContext) throws InterruptedException {
    try {
      if (profile.policies().isPresent()) {
        tpsApiDispatch.createPao(
            profile.billingProfile().id(),
            profile.policies().get(),
            TpsComponent.BPM,
            TpsObjectType.BILLING_PROFILE);

        FlightMap workingMap = flightContext.getWorkingMap();
        workingMap.put(ProfileMapKeys.POLICIES, profile.policies().get());
      }
    } catch (PolicyServiceDuplicateException e) {
      // Before the flight we check that the profile does not exist, so it's safe to assume that
      // any policy on this profile object was created by this flight, and we can ignore
      // duplicates.
      logger.info(
          "Created duplicate policy for profile {}. This is expected for Stairway retries",
          profile.billingProfile().id());
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext flightContext) throws InterruptedException {
    tpsApiDispatch.deletePao(profile.billingProfile().id());
    return StepResult.getStepResultSuccess();
  }
}
