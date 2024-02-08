package bio.terra.profile.service.profile.flight.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bio.terra.cloudres.google.billing.CloudBillingClientCow;
import bio.terra.common.exception.ForbiddenException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.common.ProfileFixtures;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.UpdateProfileRequest;
import bio.terra.profile.service.azure.AzureService;
import bio.terra.profile.service.crl.GcpCrlService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.iam.model.SamAction;
import bio.terra.profile.service.iam.model.SamResourceType;
import bio.terra.profile.service.policy.TpsApiDispatch;
import bio.terra.profile.service.profile.ProfileService;
import bio.terra.profile.service.profile.flight.common.VerifyUserBillingAccountAccessStep;
import bio.terra.profile.service.profile.model.BillingProfile;
import com.google.iam.v1.TestIamPermissionsResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class UpdateProfileFlightTest extends BaseSpringUnitTest {

  @Autowired ProfileService profileService;
  @Autowired AzureConfiguration azureConfiguration;
  @MockBean GcpCrlService crlService;

  @MockBean SamService samService;
  @MockBean AzureService azureService;
  @MockBean TpsApiDispatch tpsApiDispatch;
  @MockBean ProfileDao profileDao;

  AuthenticatedUserRequest userRequest =
      AuthenticatedUserRequest.builder()
          .setToken("fake-token")
          .setSubjectId("fake-sub")
          .setEmail("example@example.com")
          .build();

  @Test
  void updateProfileBillingAccountAndDescriptionSuccess() throws InterruptedException {
    String newBillingAccount = "newBillingAccount";
    String newDescription = "newDescription";

    var billingCow = mock(CloudBillingClientCow.class);
    when(crlService.getBillingClientCow(any())).thenReturn(billingCow);
    var iamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(VerifyUserBillingAccountAccessStep.PERMISSIONS_TO_TEST)
            .build();
    when(billingCow.testIamPermissions(any())).thenReturn(iamPermissionsResponse);
    var profile = ProfileFixtures.createGcpBillingProfile("ABCD1234");
    var updatedProfile =
        new BillingProfile(
            profile.id(),
            profile.displayName(),
            newDescription,
            profile.biller(),
            profile.cloudPlatform(),
            Optional.of(newBillingAccount),
            profile.tenantId(),
            profile.subscriptionId(),
            profile.managedResourceGroupId(),
            profile.createdTime(),
            profile.lastModified(),
            profile.createdBy());

    when(samService.hasActions(userRequest, SamResourceType.PROFILE, profile.id()))
        .thenReturn(true);
    when(profileDao.getBillingProfileById(profile.id()))
        .thenReturn(profile); // first call occurs pre-flight
    when(profileDao.getBillingProfileById(profile.id()))
        .thenReturn(updatedProfile); // second call occurs post-update
    when(profileDao.updateProfile(profile.id(), newDescription, newBillingAccount))
        .thenReturn(true);

    var result =
        profileService.updateProfile(
            profile.id(),
            new UpdateProfileRequest()
                .billingAccountId(newBillingAccount)
                .description(newDescription),
            userRequest);
    assertEquals(profile.id(), result.getId());
    assertEquals(newBillingAccount, result.getBillingAccountId());
    assertEquals(newDescription, result.getDescription());

    verify(samService)
        .verifyAuthorization(
            userRequest, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_BILLING_ACCOUNT);
    verify(samService)
        .verifyAuthorization(
            userRequest, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_METADATA);
    verify(billingCow).testIamPermissions(any());
  }

  @Test
  void updateProfileBillingAccountSuccess() throws InterruptedException {
    String newBillingAccount = "newBillingAccount";

    var billingCow = mock(CloudBillingClientCow.class);
    when(crlService.getBillingClientCow(any())).thenReturn(billingCow);
    var iamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(VerifyUserBillingAccountAccessStep.PERMISSIONS_TO_TEST)
            .build();
    when(billingCow.testIamPermissions(any())).thenReturn(iamPermissionsResponse);
    var profile = ProfileFixtures.createGcpBillingProfile("ABCD1234");
    var updatedProfile =
        new BillingProfile(
            profile.id(),
            profile.displayName(),
            profile.description(),
            profile.biller(),
            profile.cloudPlatform(),
            Optional.of(newBillingAccount),
            profile.tenantId(),
            profile.subscriptionId(),
            profile.managedResourceGroupId(),
            profile.createdTime(),
            profile.lastModified(),
            profile.createdBy());

    when(samService.hasActions(userRequest, SamResourceType.PROFILE, profile.id()))
        .thenReturn(true);
    when(profileDao.getBillingProfileById(profile.id()))
        .thenReturn(profile); // first call occurs pre-flight
    when(profileDao.getBillingProfileById(profile.id()))
        .thenReturn(updatedProfile); // second call occurs post-update
    when(profileDao.updateProfile(profile.id(), null, newBillingAccount)).thenReturn(true);

    var result =
        profileService.updateProfile(
            profile.id(),
            new UpdateProfileRequest().billingAccountId(newBillingAccount),
            userRequest);
    assertEquals(profile.id(), result.getId());
    assertEquals(newBillingAccount, result.getBillingAccountId());

    verify(samService)
        .verifyAuthorization(
            userRequest, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_BILLING_ACCOUNT);
    verify(samService, times(0))
        .verifyAuthorization(
            userRequest, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_METADATA);
    verify(billingCow).testIamPermissions(any());
  }

  @Test
  void updateProfileDescriptionSuccess() throws InterruptedException {
    String newDescription = "newDescription";

    var profile = ProfileFixtures.createGcpBillingProfile("ABCD1234");
    var updatedProfile =
        new BillingProfile(
            profile.id(),
            profile.displayName(),
            newDescription,
            profile.biller(),
            profile.cloudPlatform(),
            profile.billingAccountId(),
            profile.tenantId(),
            profile.subscriptionId(),
            profile.managedResourceGroupId(),
            profile.createdTime(),
            profile.lastModified(),
            profile.createdBy());

    when(samService.hasActions(userRequest, SamResourceType.PROFILE, profile.id()))
        .thenReturn(true);
    when(profileDao.getBillingProfileById(profile.id()))
        .thenReturn(profile); // first call occurs pre-flight
    when(profileDao.getBillingProfileById(profile.id()))
        .thenReturn(updatedProfile); // second call occurs post-update
    when(profileDao.updateProfile(profile.id(), newDescription, null)).thenReturn(true);

    var result =
        profileService.updateProfile(
            profile.id(), new UpdateProfileRequest().description(newDescription), userRequest);
    assertEquals(profile.id(), result.getId());
    assertEquals(newDescription, result.getDescription());

    verify(samService)
        .verifyAuthorization(
            userRequest, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_METADATA);
    verify(samService, times(0))
        .verifyAuthorization(
            userRequest, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_BILLING_ACCOUNT);
    verifyNoInteractions(crlService);
  }

  @Test
  void updateProfileUnauthorizedFailure() throws InterruptedException {
    String newBillingAccount = "newBillingAccount";

    var profile = ProfileFixtures.createGcpBillingProfile("ABCD1234");

    when(samService.hasActions(userRequest, SamResourceType.PROFILE, profile.id()))
        .thenReturn(true); // pre-flight access check
    doThrow(new ForbiddenException("forbidden"))
        .when(samService)
        .verifyAuthorization(
            userRequest, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_BILLING_ACCOUNT);
    when(profileDao.getBillingProfileById(profile.id()))
        .thenReturn(profile); // first call occurs pre-flight

    assertThrows(
        ForbiddenException.class,
        () ->
            profileService.updateProfile(
                profile.id(),
                new UpdateProfileRequest().billingAccountId(newBillingAccount),
                userRequest));

    verify(profileDao, times(0)).updateProfile(any(), any(), any());
  }
}
