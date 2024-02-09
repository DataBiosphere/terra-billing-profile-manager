package bio.terra.profile.service.gcp;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.service.crl.GcpCrlService;
import bio.terra.profile.service.gcp.exception.InaccessibleBillingAccountException;
import bio.terra.profile.service.profile.exception.MissingRequiredFieldsException;
import com.google.cloud.billing.v1.BillingAccountName;
import com.google.iam.v1.TestIamPermissionsRequest;
import com.google.iam.v1.TestIamPermissionsResponse;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GcpService {

  private static final Logger logger =
      LoggerFactory.getLogger(bio.terra.profile.service.gcp.GcpService.class);

  public static final List<String> BILLING_ACCOUNT_PERMISSIONS_TO_TEST =
      List.of("billing.resourceAssociations.create");

  private final GcpCrlService crlService;

  @Autowired
  public GcpService(GcpCrlService crlService) {
    this.crlService = crlService;
  }

  public void verifyUserBillingAccountAccess(
      Optional<String> billingAccountIdOpt, AuthenticatedUserRequest user) {
    var billingCow = crlService.getBillingClientCow(user);
    var billingAccountId =
        billingAccountIdOpt.orElseThrow(
            () -> new MissingRequiredFieldsException("Missing billing account ID"));
    logger.info(
        String.format(
            "Checking user %s permissions on billing account %s",
            user.getEmail(), billingAccountId));

    var testPermissionsRequest =
        TestIamPermissionsRequest.newBuilder()
            .setResource(BillingAccountName.of(billingAccountId).toString())
            .addAllPermissions(BILLING_ACCOUNT_PERMISSIONS_TO_TEST)
            .build();

    final TestIamPermissionsResponse testPermissionsResponse;
    testPermissionsResponse = billingCow.testIamPermissions(testPermissionsRequest);

    var actualPermissions = testPermissionsResponse.getPermissionsList();
    if (actualPermissions == null
        || !actualPermissions.equals(BILLING_ACCOUNT_PERMISSIONS_TO_TEST)) {
      var message =
          String.format(
              "The user [%s] needs access to the billing account [%s] to perform the requested operation",
              user.getEmail(), billingAccountId);
      throw new InaccessibleBillingAccountException(message);
    }
  }
}
