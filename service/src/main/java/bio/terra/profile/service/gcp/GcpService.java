package bio.terra.profile.service.gcp;

import bio.terra.cloudres.google.billing.CloudBillingClientCow;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.GcpConfiguration;
import bio.terra.profile.service.crl.GcpCrlService;
import bio.terra.profile.service.gcp.exception.InaccessibleBillingAccountException;
import bio.terra.profile.service.profile.exception.MissingRequiredFieldsException;
import com.google.cloud.billing.v1.BillingAccountName;
import com.google.iam.v1.TestIamPermissionsRequest;
import com.google.iam.v1.TestIamPermissionsResponse;
import java.io.IOException;
import java.util.Collections;
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
      Collections.singletonList("billing.resourceAssociations.create");

  private final GcpCrlService crlService;
  private final GcpConfiguration configuration;

  @Autowired
  public GcpService(GcpConfiguration configuration, GcpCrlService crlService) {
    this.crlService = crlService;
    this.configuration = configuration;
  }

  public void verifyTerraBillingAccountAccess(Optional<String> billingAccountIdOpt) {
    try {
      // The BPM SA is a member of the Google group terra-billing@firecloud.org, so we can use it to
      // verify that we have the necessary permissions to access a billing account.
      var billingCow = crlService.getBillingClientCow(configuration.getSaCredentials());
      verifyBillingAccountAccess(billingAccountIdOpt, billingCow, "BPM service account");
    } catch (IOException ex) {
      throw new RuntimeException("Failed to get service account credentials", ex);
    }
  }

  public void verifyUserBillingAccountAccess(
      Optional<String> billingAccountIdOpt, AuthenticatedUserRequest user) {
    var billingCow = crlService.getBillingClientCow(user);
    verifyBillingAccountAccess(billingAccountIdOpt, billingCow, user.getEmail());
  }

  public void verifyBillingAccountAccess(
      Optional<String> billingAccountIdOpt,
      CloudBillingClientCow billingCow,
      String userIdentifier) {
    var billingAccountId =
        billingAccountIdOpt.orElseThrow(
            () -> new MissingRequiredFieldsException("Missing billing account ID"));
    logger.info(
        String.format(
            "Checking user %s permissions on billing account %s",
            userIdentifier, billingAccountId));

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
              userIdentifier, billingAccountId);
      throw new InaccessibleBillingAccountException(message);
    }
  }
}
