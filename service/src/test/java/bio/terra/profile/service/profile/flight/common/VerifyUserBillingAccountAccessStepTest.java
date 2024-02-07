package bio.terra.profile.service.profile.flight.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import bio.terra.cloudres.google.billing.CloudBillingClientCow;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.crl.GcpCrlService;
import bio.terra.profile.service.profile.exception.InaccessibleBillingAccountException;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
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

class VerifyUserBillingAccountAccessStepTest extends BaseSpringUnitTest {

  @Mock private FlightContext flightContext;
  @Mock private GcpCrlService crlService;
  @Mock private CloudBillingClientCow billingClientCow;

  private AuthenticatedUserRequest user;
  private BillingProfile profile;
  private VerifyUserBillingAccountAccessStep step;

  @BeforeEach
  public void before() {
    user =
        AuthenticatedUserRequest.builder()
            .setSubjectId("12345")
            .setEmail("profileId@unit.com")
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

    step = new VerifyUserBillingAccountAccessStep(crlService, profile, user);

    when(crlService.getBillingClientCow(eq(user))).thenReturn(billingClientCow);
  }

  @Test
  void verifyAccount() throws InterruptedException {
    var captor = ArgumentCaptor.forClass(TestIamPermissionsRequest.class);
    when(billingClientCow.testIamPermissions(captor.capture()))
        .thenReturn(
            TestIamPermissionsResponse.newBuilder()
                .addAllPermissions(VerifyUserBillingAccountAccessStep.PERMISSIONS_TO_TEST)
                .build());

    var result = step.doStep(flightContext);

    assertEquals(
        "billingAccounts/" + profile.billingAccountId().get(), captor.getValue().getResource());
    assertEquals(
        VerifyUserBillingAccountAccessStep.PERMISSIONS_TO_TEST,
        captor.getValue().getPermissionsList());
    assertEquals(StepResult.getStepResultSuccess(), result);
  }

  @Test
  void verifyAccountNoAccess() throws InterruptedException {
    var captor = ArgumentCaptor.forClass(TestIamPermissionsRequest.class);
    when(billingClientCow.testIamPermissions(captor.capture()))
        .thenReturn(
            TestIamPermissionsResponse.newBuilder()
                .addAllPermissions(List.of("some-other-permission"))
                .build());

    assertThrows(InaccessibleBillingAccountException.class, () -> step.doStep(flightContext));

    assertEquals(
        "billingAccounts/" + profile.billingAccountId().get(), captor.getValue().getResource());
    assertEquals(
        VerifyUserBillingAccountAccessStep.PERMISSIONS_TO_TEST,
        captor.getValue().getPermissionsList());
  }
}
