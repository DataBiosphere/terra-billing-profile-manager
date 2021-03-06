package bio.terra.profile.service.profile.flight.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import bio.terra.cloudres.google.billing.CloudBillingClientCow;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.crl.CrlService;
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

public class CreateProfileVerifyAccountStepTest extends BaseUnitTest {

  @Mock private FlightContext flightContext;
  @Mock private CrlService crlService;
  @Mock private CloudBillingClientCow billingClientCow;

  private AuthenticatedUserRequest user;
  private BillingProfile profile;
  private CreateProfileVerifyAccountStep step;

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
            Optional.empty(),
            Instant.now(),
            Instant.now(),
            "creator");

    step = new CreateProfileVerifyAccountStep(crlService, profile, user);

    when(crlService.getBillingClientCow(eq(user))).thenReturn(billingClientCow);
  }

  @Test
  public void verifyAccount() throws InterruptedException {
    var captor = ArgumentCaptor.forClass(TestIamPermissionsRequest.class);
    when(billingClientCow.testIamPermissions(captor.capture()))
        .thenReturn(
            TestIamPermissionsResponse.newBuilder()
                .addAllPermissions(List.of("billing.resourceAssociations.create"))
                .build());

    var result = step.doStep(flightContext);

    assertEquals(
        "billingAccounts/" + profile.billingAccountId().get(), captor.getValue().getResource());
    assertEquals(
        List.of("billing.resourceAssociations.create"), captor.getValue().getPermissionsList());
    assertEquals(StepResult.getStepResultSuccess(), result);
  }

  @Test
  public void verifyAccountNoAccess() throws InterruptedException {
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
        List.of("billing.resourceAssociations.create"), captor.getValue().getPermissionsList());
  }
}
