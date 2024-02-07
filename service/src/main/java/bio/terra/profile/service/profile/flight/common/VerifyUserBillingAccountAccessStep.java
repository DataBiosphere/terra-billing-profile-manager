package bio.terra.profile.service.profile.flight.common;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.service.crl.GcpCrlService;
import bio.terra.profile.service.profile.exception.InaccessibleBillingAccountException;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.google.cloud.billing.v1.BillingAccountName;
import com.google.iam.v1.TestIamPermissionsRequest;
import com.google.iam.v1.TestIamPermissionsResponse;
import java.util.List;

/** Step to verify the user has access to a GCP profile's billing account. */
public record VerifyUserBillingAccountAccessStep(
    GcpCrlService crlService, BillingProfile profile, AuthenticatedUserRequest user)
    implements Step {

  public static final List<String> PERMISSIONS_TO_TEST =
      List.of("billing.resourceAssociations.create");

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException {
    var billingCow = crlService.getBillingClientCow(user);

    var testPermissionsRequest =
        TestIamPermissionsRequest.newBuilder()
            .setResource(BillingAccountName.of(profile.getRequiredBillingAccountId()).toString())
            .addAllPermissions(PERMISSIONS_TO_TEST)
            .build();

    final TestIamPermissionsResponse testPermissionsResponse;
    try {
      testPermissionsResponse = billingCow.testIamPermissions(testPermissionsRequest);
    } catch (Exception e) {
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }

    var actualPermissions = testPermissionsResponse.getPermissionsList();
    if (actualPermissions == null || !actualPermissions.equals(PERMISSIONS_TO_TEST)) {
      var message =
          String.format(
              "The user [%s] needs access to the billing account [%s] to perform the requested operation",
              user.getEmail(), profile.getRequiredBillingAccountId());
      throw new InaccessibleBillingAccountException(message);
    }

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    // Verify account has no side effects to clean up
    return StepResult.getStepResultSuccess();
  }
}
