package bio.terra.profile.service.profile.flight.create;

import bio.terra.policy.model.TpsPolicyInputs;
import bio.terra.profile.model.Organization;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.profile.service.profile.exception.MissingRequiredFieldsException;
import bio.terra.profile.service.profile.flight.ProfileMapKeys;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.profile.service.profile.model.ProfileDescription;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import java.util.Optional;
import org.springframework.http.HttpStatus;

public class CreateProfileFinishStep implements Step {

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException {
    FlightMap workingMap = context.getWorkingMap();
    BillingProfile createdProfile = workingMap.get(ProfileMapKeys.PROFILE, BillingProfile.class);
    if (createdProfile == null) {
      throw new MissingRequiredFieldsException(
          "Missing required flight map key: " + ProfileMapKeys.PROFILE);
    }

    Optional<TpsPolicyInputs> policies =
        Optional.ofNullable(workingMap.get(ProfileMapKeys.POLICIES, TpsPolicyInputs.class));
    Optional<Organization> organization =
        Optional.ofNullable(
            context.getInputParameters().get(ProfileMapKeys.ORGANIZATION, Organization.class));

    workingMap.put(
        JobMapKeys.RESPONSE.getKeyName(),
        new ProfileDescription(createdProfile, policies, organization));
    workingMap.put(JobMapKeys.STATUS_CODE.getKeyName(), HttpStatus.CREATED);
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
