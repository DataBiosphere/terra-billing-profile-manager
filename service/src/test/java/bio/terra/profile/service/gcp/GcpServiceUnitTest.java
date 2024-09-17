package bio.terra.profile.service.gcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import bio.terra.cloudres.google.billing.CloudBillingClientCow;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.GcpConfiguration;
import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.crl.GcpCrlService;
import bio.terra.profile.service.gcp.exception.InaccessibleBillingAccountException;
import bio.terra.profile.service.profile.model.BillingProfile;
import com.google.iam.v1.TestIamPermissionsRequest;
import com.google.iam.v1.TestIamPermissionsResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class GcpServiceUnitTest extends BaseSpringUnitTest {
  @Mock private GcpCrlService crlService;
  @Mock private CloudBillingClientCow billingClientCow;

  private AuthenticatedUserRequest user;
  private BillingProfile profile;
  private GcpService gcpService;

  @BeforeEach
  public void before() {
    user =
        AuthenticatedUserRequest.builder()
            .setSubjectId("12345")
            .setEmail("profile@unit.com")
            .setToken("token")
            .build();
    profile =
        new BillingProfile(
            UUID.randomUUID(),
            "name",
            "description",
            "direct",
            CloudPlatform.GCP,
            Optional.of("billingAccount"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Instant.now(),
            Instant.now(),
            "creator");

    when(crlService.getBillingClientCow(eq(user))).thenReturn(billingClientCow);
    gcpService = new GcpService(new GcpConfiguration(), crlService);
  }

  @Test
  void verifyAccount() {
    var captor = ArgumentCaptor.forClass(TestIamPermissionsRequest.class);
    when(billingClientCow.testIamPermissions(captor.capture()))
        .thenReturn(
            TestIamPermissionsResponse.newBuilder()
                .addAllPermissions(GcpService.BILLING_ACCOUNT_PERMISSIONS_TO_TEST)
                .build());

    gcpService.verifyUserBillingAccountAccess(profile.billingAccountId(), user);

    assertEquals(
        "billingAccounts/" + profile.billingAccountId().get(), captor.getValue().getResource());
    assertEquals(
        GcpService.BILLING_ACCOUNT_PERMISSIONS_TO_TEST, captor.getValue().getPermissionsList());
  }

  @Test
  void verifyAccountNoAccess() {
    var captor = ArgumentCaptor.forClass(TestIamPermissionsRequest.class);
    when(billingClientCow.testIamPermissions(captor.capture()))
        .thenReturn(
            TestIamPermissionsResponse.newBuilder()
                .addAllPermissions(List.of("some-other-permission"))
                .build());

    assertThrows(
        InaccessibleBillingAccountException.class,
        () -> gcpService.verifyUserBillingAccountAccess(profile.billingAccountId(), user));

    assertEquals(
        "billingAccounts/" + profile.billingAccountId().get(), captor.getValue().getResource());
    assertEquals(
        GcpService.BILLING_ACCOUNT_PERMISSIONS_TO_TEST, captor.getValue().getPermissionsList());
  }
}
