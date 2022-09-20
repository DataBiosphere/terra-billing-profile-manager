package bio.terra.profile.service.profile.flight.create;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.service.crl.CrlService;
import bio.terra.profile.service.profile.exception.DuplicateProfileException;
import bio.terra.profile.service.profile.exception.MissingRequiredFieldsException;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import java.util.HashMap;

/** Add a tag that associates a billing profile ID with an azure managed resource group */
public record LinkBillingProfileIdToMrgStep(
    CrlService crlService, BillingProfile profile, AuthenticatedUserRequest user) implements Step {
  private static final String BILLING_PROFILE_ID_TAG = "terra.billingProfileId";

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    try {
      validateBillingProfile();

      var resourceManager =
          crlService.getResourceManager(profile.tenantId().get(), profile.subscriptionId().get());
      var resource =
          resourceManager.resourceGroups().getByName(profile.managedResourceGroupId().get());

      var existingTags = resource.tags();
      var existing = existingTags.get(BILLING_PROFILE_ID_TAG);
      if (existing != null) {
        throw new DuplicateProfileException(
            String.format(
                "MRG has existing billing profile linked [mrg_id = %s, existing billing_profile_id = %s]",
                profile.managedResourceGroupId().get(), existing));
      }

      // dupe existing tags to a mutable hashmap
      HashMap<String, String> tags = new HashMap<>(resource.tags());
      tags.put(BILLING_PROFILE_ID_TAG, profile.id().toString());
      resourceManager.tagOperations().updateTags(resource.id(), tags);

      return StepResult.getStepResultSuccess();
    } catch (Exception ex) {
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, ex);
    }
  }

  private void validateBillingProfile() {
    if (profile.tenantId().isEmpty()) {
      throw new MissingRequiredFieldsException(
          "No tenant ID on billing profile ID " + profile.id());
    }
    if (profile.subscriptionId().isEmpty()) {
      throw new MissingRequiredFieldsException(
          "No subscription ID on billing profile ID " + profile.id());
    }
    if (profile.managedResourceGroupId().isEmpty()) {
      throw new MissingRequiredFieldsException("No MRG ID on billing profile ID " + profile.id());
    }
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
