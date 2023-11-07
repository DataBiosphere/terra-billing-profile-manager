package bio.terra.profile.service.profile.flight.create;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import bio.terra.cloudres.google.billing.CloudBillingClientCow;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.sam.exception.SamExceptionFactory;
import bio.terra.common.sam.exception.SamInterruptedException;
import bio.terra.policy.model.TpsComponent;
import bio.terra.policy.model.TpsObjectType;
import bio.terra.policy.model.TpsPolicyInput;
import bio.terra.policy.model.TpsPolicyInputs;
import bio.terra.profile.app.common.MetricUtils;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.common.ProfileFixtures;
import bio.terra.profile.model.AzureManagedAppModel;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.azure.AzureService;
import bio.terra.profile.service.crl.GcpCrlService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.policy.TpsApiDispatch;
import bio.terra.profile.service.policy.exception.PolicyServiceAPIException;
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
  @MockBean GcpCrlService crlService;

  @MockBean SamService samService;
  @MockBean AzureService azureService;
  @MockBean TpsApiDispatch tpsApiDispatch;

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
    var profile = ProfileFixtures.createGcpBillingProfile("ABCD1234");

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
            null,
            Optional.empty());

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
    var profile = ProfileFixtures.createGcpBillingProfile("ABCDEF-1234");

    assertThrows(
        InaccessibleBillingAccountException.class,
        () -> profileService.createProfile(profile, userRequest));
  }

  @Test
  void createGcpProfile_withPolicy() throws InterruptedException {
    var billingCow = mock(CloudBillingClientCow.class);
    when(crlService.getBillingClientCow(any())).thenReturn(billingCow);
    var iamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(CreateProfileVerifyAccountStep.PERMISSIONS_TO_TEST)
            .build();
    when(billingCow.testIamPermissions(any())).thenReturn(iamPermissionsResponse);
    var policies =
        new TpsPolicyInputs()
            .addInputsItem(new TpsPolicyInput().namespace("terra").name("protected-data"));
    var profile = ProfileFixtures.createGcpBillingProfile("ABCD1234", Optional.of(policies));

    profileService.createProfile(profile, userRequest);

    verify(tpsApiDispatch)
        .createPao(profile.id(), policies, TpsComponent.BPM, TpsObjectType.BILLING_PROFILE);
  }

  @Test
  void createGcpProfile_deletePolicyOnFailure() throws InterruptedException {
    var billingCow = mock(CloudBillingClientCow.class);
    when(crlService.getBillingClientCow(any())).thenReturn(billingCow);
    var iamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(CreateProfileVerifyAccountStep.PERMISSIONS_TO_TEST)
            .build();
    when(billingCow.testIamPermissions(any())).thenReturn(iamPermissionsResponse);
    doThrow(new PolicyServiceAPIException("foo"))
        .when(tpsApiDispatch)
        .createPao(any(), any(), any(), any());
    var policies =
        new TpsPolicyInputs()
            .addInputsItem(new TpsPolicyInput().namespace("terra").name("protected-data"));
    var profile = ProfileFixtures.createGcpBillingProfile("ABCD1234", Optional.of(policies));

    assertThrows(
        PolicyServiceAPIException.class, () -> profileService.createProfile(profile, userRequest));

    verify(tpsApiDispatch)
        .createPao(profile.id(), policies, TpsComponent.BPM, TpsObjectType.BILLING_PROFILE);
    verify(tpsApiDispatch).deletePao(profile.id());
  }

  @Test
  void createAzureProfileInaccessibleAppDeployment() {
    when(azureService.getAuthorizedManagedAppDeployments(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    var profile =
        ProfileFixtures.createAzureBillingProfile(UUID.randomUUID(), UUID.randomUUID(), "fake");

    assertThrows(
        InaccessibleApplicationDeploymentException.class,
        () -> profileService.createProfile(profile, userRequest));
  }

  @Test
  void createAzureProfileSuccess() throws InterruptedException {
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
    var profile = ProfileFixtures.createAzureBillingProfile(tenantId, subId, mrgId);

    profileService.createProfile(profile, userRequest);

    verify(samService).createManagedResourceGroup(profile, userRequest);
  }

  @Test
  void createAzureProfile_removeSamMrgOnFailure() throws InterruptedException {
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
    doThrow(SamExceptionFactory.create("foo", new InterruptedException()))
        .when(samService)
        .createManagedResourceGroup(any(), eq(userRequest));
    var profile = ProfileFixtures.createAzureBillingProfile(tenantId, subId, mrgId);

    assertThrows(
        SamInterruptedException.class, () -> profileService.createProfile(profile, userRequest));

    verify(samService).deleteManagedResourceGroup(profile.id(), userRequest);
  }

  @Test
  void createAzureProfile_withPolicy() throws InterruptedException {
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
    var policies =
        new TpsPolicyInputs()
            .addInputsItem(new TpsPolicyInput().namespace("terra").name("protected-data"));
    var profile =
        ProfileFixtures.createAzureBillingProfile(tenantId, subId, mrgId, Optional.of(policies));

    profileService.createProfile(profile, userRequest);

    verify(tpsApiDispatch)
        .createPao(profile.id(), policies, TpsComponent.BPM, TpsObjectType.BILLING_PROFILE);
  }

  @Test
  void createAzureProfile_deletePolicyOnFailure() throws InterruptedException {
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
    doThrow(new PolicyServiceAPIException("foo"))
        .when(tpsApiDispatch)
        .createPao(any(), any(), any(), any());
    var policies =
        new TpsPolicyInputs()
            .addInputsItem(new TpsPolicyInput().namespace("terra").name("protected-data"));
    var profile =
        ProfileFixtures.createAzureBillingProfile(tenantId, subId, mrgId, Optional.of(policies));

    assertThrows(
        PolicyServiceAPIException.class, () -> profileService.createProfile(profile, userRequest));

    verify(tpsApiDispatch)
        .createPao(profile.id(), policies, TpsComponent.BPM, TpsObjectType.BILLING_PROFILE);
    verify(tpsApiDispatch).deletePao(profile.id());
  }

  @Test
  void metricsAreCalledOnProfileCreation() {
    try (var metricsMock = mockStatic(MetricUtils.class)) {
      var profile = ProfileFixtures.createGcpBillingProfile("ABCD1234");
      metricsMock
          .when(() -> MetricUtils.recordProfileCreation(any(), eq(CloudPlatform.GCP)))
          .thenReturn(profile);
      profileService.createProfile(profile, userRequest);
      metricsMock.verify(() -> MetricUtils.recordProfileCreation(any(), eq(CloudPlatform.GCP)));
    }
  }
}
