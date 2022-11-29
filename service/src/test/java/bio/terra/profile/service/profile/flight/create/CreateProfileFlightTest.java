package bio.terra.profile.service.profile.flight.create;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import bio.terra.cloudres.google.billing.CloudBillingClientCow;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.sam.exception.SamExceptionFactory;
import bio.terra.common.sam.exception.SamInterruptedException;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.model.AzureManagedAppModel;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.azure.ApplicationService;
import bio.terra.profile.service.azure.AzureService;
import bio.terra.profile.service.crl.CrlService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.profile.ProfileService;
import bio.terra.profile.service.profile.exception.InaccessibleApplicationDeploymentException;
import bio.terra.profile.service.profile.exception.InaccessibleBillingAccountException;
import bio.terra.profile.service.profile.exception.MissingRequiredFieldsException;
import bio.terra.profile.service.profile.model.BillingProfile;
import com.google.iam.v1.TestIamPermissionsResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class CreateProfileFlightTest extends BaseSpringUnitTest {

  @Autowired ProfileService profileService;
  @Autowired AzureConfiguration azureConfiguration;
  @MockBean CrlService crlService;

  @MockBean SamService samService;
  @MockBean AzureService azureService;
  @MockBean ApplicationService applicationService;

  AuthenticatedUserRequest userRequest =
      AuthenticatedUserRequest.builder()
          .setToken("fake-token")
          .setSubjectId("fake-sub")
          .setEmail("example@example.com")
          .build();

  @Test
  void createGcpProfileSuccess() {
    var billingCow = mock(CloudBillingClientCow.class);
    when(crlService.getBillingClientCow(any())).thenReturn(billingCow);
    var iamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(CreateProfileVerifyAccountStep.PERMISSIONS_TO_TEST)
            .build();
    when(billingCow.testIamPermissions(any())).thenReturn(iamPermissionsResponse);
    var profile =
        new BillingProfile(
            UUID.randomUUID(),
            "fake-bp-name",
            "fake-description",
            "direct",
            CloudPlatform.GCP,
            Optional.of("ABCDEF-1234"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            null,
            null,
            null);

    var createdProfile = profileService.createProfile(profile, userRequest);

    assertEquals(createdProfile.id(), profile.id());
    assertEquals(createdProfile.biller(), profile.biller());
    assertEquals(createdProfile.cloudPlatform(), profile.cloudPlatform());
    assertEquals(createdProfile.billingAccountId().get(), profile.billingAccountId().get());
    assertEquals(createdProfile.displayName(), profile.displayName());
    assertNotNull(createdProfile.createdTime());
    assertNotNull(createdProfile.lastModified());
    assertEquals(createdProfile.createdBy(), userRequest.getEmail());
  }

  @Test
  void createGcpProfileMissingBillingAccount() {
    var billingCow = mock(CloudBillingClientCow.class);
    when(crlService.getBillingClientCow(any())).thenReturn(billingCow);
    var iamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(CreateProfileVerifyAccountStep.PERMISSIONS_TO_TEST)
            .build();
    when(billingCow.testIamPermissions(any())).thenReturn(iamPermissionsResponse);
    var profile =
        new BillingProfile(
            UUID.randomUUID(),
            "fake-bp-name",
            "fake-description",
            "direct",
            CloudPlatform.GCP,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            null,
            null,
            null);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> profileService.createProfile(profile, userRequest));
  }

  @Test
  void createGcpProfileInvalidPermissions() {
    var billingCow = mock(CloudBillingClientCow.class);
    when(crlService.getBillingClientCow(any())).thenReturn(billingCow);
    var iamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(List.of("billing:wrong", "billing:fake"))
            .build();
    when(billingCow.testIamPermissions(any())).thenReturn(iamPermissionsResponse);
    var profile =
        new BillingProfile(
            UUID.randomUUID(),
            "fake-bp-name",
            "fake-description",
            "direct",
            CloudPlatform.GCP,
            Optional.of("ABCDEF-1234"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            null,
            null,
            null);

    assertThrows(
        InaccessibleBillingAccountException.class,
        () -> profileService.createProfile(profile, userRequest));
  }

  @Test
  void createAzureProfileInaccessibleAppDeployment() {
    when(azureService.getAuthorizedManagedAppDeployments(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    var profile =
        new BillingProfile(
            UUID.randomUUID(),
            "fake-bp-name",
            "fake-description",
            "direct",
            CloudPlatform.AZURE,
            Optional.empty(),
            Optional.of(UUID.randomUUID()),
            Optional.of(UUID.randomUUID()),
            Optional.of("fake-mrg"),
            null,
            null,
            null);

    assertThrows(
        InaccessibleApplicationDeploymentException.class,
        () -> profileService.createProfile(profile, userRequest));
  }

  @Test
  void createAzureProfileSuccess() {
    var subId = UUID.randomUUID();
    var tenantId = UUID.randomUUID();
    var mrgId = "fake-mrg";

    when(azureService.getAuthorizedManagedAppDeployments(any(), any(), any()))
        .thenReturn(
            Collections.singletonList(
                new AzureManagedAppModel()
                    .tenantId(tenantId)
                    .subscriptionId(subId)
                    .managedResourceGroupId(mrgId)));
    when(azureService.getRegisteredProviderNamespacesForSubscription(any(), any()))
        .thenReturn(azureConfiguration.getRequiredProviders());

    var profile =
        new BillingProfile(
            UUID.randomUUID(),
            "fake-bp-name",
            "fake-description",
            "direct",
            CloudPlatform.AZURE,
            Optional.empty(),
            Optional.of(tenantId),
            Optional.of(subId),
            Optional.of(mrgId),
            null,
            null,
            null);

    profileService.createProfile(profile, userRequest);

    verify(applicationService)
        .addTagToMrg(tenantId, subId, mrgId, "terra.billingProfileId", profile.id().toString());
  }

  @Test
  void tagIsRemovedFromMRGOnFailure() throws Exception {
    var subId = UUID.randomUUID();
    var tenantId = UUID.randomUUID();
    var mrgId = "fake-mrg";
    var billingProfileId = UUID.randomUUID();
    when(azureService.getAuthorizedManagedAppDeployments(any(), any(), any()))
        .thenReturn(
            Collections.singletonList(
                new AzureManagedAppModel()
                    .tenantId(tenantId)
                    .subscriptionId(subId)
                    .managedResourceGroupId(mrgId)));
    when(azureService.getRegisteredProviderNamespacesForSubscription(any(), any()))
        .thenReturn(azureConfiguration.getRequiredProviders());
    doThrow(SamExceptionFactory.create("foo", new InterruptedException()))
        .when(samService).createProfileResource(any(), eq(billingProfileId));

    var profile =
        new BillingProfile(
            billingProfileId,
            "fake-bp-name",
            "fake-description",
            "direct",
            CloudPlatform.AZURE,
            Optional.empty(),
            Optional.of(tenantId),
            Optional.of(subId),
            Optional.of(mrgId),
            null,
            null,
            null);
    assertThrows(SamInterruptedException.class, () -> profileService.createProfile(profile, userRequest));

    verify(applicationService)
        .addTagToMrg(tenantId, subId, mrgId, "terra.billingProfileId", profile.id().toString());
    verify(applicationService).removeTagFromMrg(tenantId, subId, mrgId, "terra.billingProfileId");
  }

}
