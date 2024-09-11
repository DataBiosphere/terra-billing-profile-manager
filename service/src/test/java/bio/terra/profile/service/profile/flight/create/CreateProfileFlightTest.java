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
import bio.terra.profile.app.configuration.EnterpriseConfiguration;
import bio.terra.profile.app.configuration.GcpConfiguration;
import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.common.ProfileFixtures;
import bio.terra.profile.model.AzureManagedAppModel;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.azure.AzureService;
import bio.terra.profile.service.crl.GcpCrlService;
import bio.terra.profile.service.gcp.GcpService;
import bio.terra.profile.service.gcp.exception.InaccessibleBillingAccountException;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.policy.TpsApiDispatch;
import bio.terra.profile.service.policy.exception.PolicyServiceAPIException;
import bio.terra.profile.service.profile.ProfileService;
import bio.terra.profile.service.profile.exception.InaccessibleApplicationDeploymentException;
import bio.terra.profile.service.profile.exception.MissingRequiredFieldsException;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.profile.service.profile.model.ProfileDescription;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.iam.v1.TestIamPermissionsResponse;
import java.io.IOException;
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
  @MockBean GcpConfiguration gcpConfiguration;

  @MockBean SamService samService;
  @MockBean AzureService azureService;
  @MockBean TpsApiDispatch tpsApiDispatch;
  @MockBean EnterpriseConfiguration enterpriseConfiguration;

  AuthenticatedUserRequest userRequest =
      AuthenticatedUserRequest.builder()
          .setToken("fake-token")
          .setSubjectId("fake-sub")
          .setEmail("example@example.com")
          .build();

  @Test
  void createGcpProfileSuccess() {
    var billingCow = mock(CloudBillingClientCow.class);
    when(crlService.getBillingClientCow((AuthenticatedUserRequest) any())).thenReturn(billingCow);
    when(crlService.getBillingClientCow((GoogleCredentials) any())).thenReturn(billingCow);
    var iamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(GcpService.BILLING_ACCOUNT_PERMISSIONS_TO_TEST)
            .build();
    when(billingCow.testIamPermissions(any())).thenReturn(iamPermissionsResponse);
    var profile = ProfileFixtures.createGcpBillingProfile("ABCD1234");

    var createdProfile =
        profileService
            .createProfile(new ProfileDescription(profile), userRequest, null)
            .billingProfile();

    assertEquals(createdProfile.id(), profile.id());
    assertEquals(createdProfile.biller(), profile.biller());
    assertEquals(createdProfile.cloudPlatform(), profile.cloudPlatform());
    assertEquals(createdProfile.billingAccountId().get(), profile.billingAccountId().get());
    assertEquals(createdProfile.displayName(), profile.displayName());
    assertNotNull(createdProfile.createdTime());
    assertNotNull(createdProfile.lastModified());
    assertEquals(createdProfile.createdBy(), userRequest.getSubjectId());
  }

  @Test
  void createGcpProfileMissingBillingAccount() {
    var billingCow = mock(CloudBillingClientCow.class);
    when(crlService.getBillingClientCow((AuthenticatedUserRequest) any())).thenReturn(billingCow);
    when(crlService.getBillingClientCow((GoogleCredentials) any())).thenReturn(billingCow);

    var iamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(GcpService.BILLING_ACCOUNT_PERMISSIONS_TO_TEST)
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
        () -> profileService.createProfile(new ProfileDescription(profile), userRequest, null));
  }

  @Test
  void createGcpProfileInvalidUserPermissions() {
    var userBillingCow = mock(CloudBillingClientCow.class);
    var saBillingCow = mock(CloudBillingClientCow.class);
    when(crlService.getBillingClientCow((AuthenticatedUserRequest) any()))
        .thenReturn(userBillingCow);
    when(crlService.getBillingClientCow((GoogleCredentials) any())).thenReturn(saBillingCow);
    var badIamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(List.of("billing:wrong", "billing:fake"))
            .build();
    when(userBillingCow.testIamPermissions(any())).thenReturn(badIamPermissionsResponse);
    var iamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(GcpService.BILLING_ACCOUNT_PERMISSIONS_TO_TEST)
            .build();
    when(saBillingCow.testIamPermissions(any())).thenReturn(iamPermissionsResponse);

    var profile = ProfileFixtures.createGcpBillingProfileDescription("ABCDEF-1234");

    assertThrows(
        InaccessibleBillingAccountException.class,
        () -> profileService.createProfile(profile, userRequest, null));
  }

  @Test
  void createGcpProfileInvalidSAPermissions() {
    var userBillingCow = mock(CloudBillingClientCow.class);
    var saBillingCow = mock(CloudBillingClientCow.class);
    when(crlService.getBillingClientCow((AuthenticatedUserRequest) any()))
        .thenReturn(userBillingCow);
    when(crlService.getBillingClientCow((GoogleCredentials) any())).thenReturn(saBillingCow);
    var badIamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(List.of("billing:wrong", "billing:fake"))
            .build();
    when(saBillingCow.testIamPermissions(any())).thenReturn(badIamPermissionsResponse);
    var iamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(GcpService.BILLING_ACCOUNT_PERMISSIONS_TO_TEST)
            .build();
    when(userBillingCow.testIamPermissions(any())).thenReturn(iamPermissionsResponse);

    var profile = ProfileFixtures.createGcpBillingProfileDescription("ABCDEF-1234");

    assertThrows(
        InaccessibleBillingAccountException.class,
        () -> profileService.createProfile(profile, userRequest, null));
  }

  @Test
  void createGcpProfileSACredentialsMissing() throws IOException {
    var billingCow = mock(CloudBillingClientCow.class);
    when(crlService.getBillingClientCow((AuthenticatedUserRequest) any())).thenReturn(billingCow);
    var iamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(GcpService.BILLING_ACCOUNT_PERMISSIONS_TO_TEST)
            .build();
    when(billingCow.testIamPermissions(any())).thenReturn(iamPermissionsResponse);

    when(gcpConfiguration.getSaCredentials()).thenThrow(new IOException("test exception"));

    var profile = ProfileFixtures.createGcpBillingProfileDescription("ABCDEF-1234");

    var exception =
        assertThrows(
            RuntimeException.class, () -> profileService.createProfile(profile, userRequest, null));
    assertEquals("Failed to get service account credentials", exception.getMessage());
  }

  @Test
  void createGcpProfile_withPolicy() throws InterruptedException {
    var billingCow = mock(CloudBillingClientCow.class);
    when(crlService.getBillingClientCow((AuthenticatedUserRequest) any())).thenReturn(billingCow);
    when(crlService.getBillingClientCow((GoogleCredentials) any())).thenReturn(billingCow);
    var iamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(GcpService.BILLING_ACCOUNT_PERMISSIONS_TO_TEST)
            .build();
    when(billingCow.testIamPermissions(any())).thenReturn(iamPermissionsResponse);
    var policies =
        new TpsPolicyInputs()
            .addInputsItem(new TpsPolicyInput().namespace("terra").name("protected-data"));
    var profile = ProfileFixtures.createGcpBillingProfileDescription("ABCD1234", policies);

    profileService.createProfile(profile, userRequest, null);

    verify(tpsApiDispatch)
        .createPao(
            profile.billingProfile().id(),
            policies,
            TpsComponent.BPM,
            TpsObjectType.BILLING_PROFILE);
  }

  @Test
  void createGcpProfile_deletePolicyOnFailure() throws InterruptedException {
    var billingCow = mock(CloudBillingClientCow.class);
    when(crlService.getBillingClientCow((AuthenticatedUserRequest) any())).thenReturn(billingCow);
    when(crlService.getBillingClientCow((GoogleCredentials) any())).thenReturn(billingCow);
    var iamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(GcpService.BILLING_ACCOUNT_PERMISSIONS_TO_TEST)
            .build();
    when(billingCow.testIamPermissions(any())).thenReturn(iamPermissionsResponse);
    doThrow(new PolicyServiceAPIException("foo"))
        .when(tpsApiDispatch)
        .createPao(any(), any(), any(), any());
    var policies =
        new TpsPolicyInputs()
            .addInputsItem(new TpsPolicyInput().namespace("terra").name("protected-data"));
    var profile = ProfileFixtures.createGcpBillingProfileDescription("ABCD1234", policies);

    assertThrows(
        PolicyServiceAPIException.class,
        () -> profileService.createProfile(profile, userRequest, null));

    verify(tpsApiDispatch)
        .createPao(
            profile.billingProfile().id(),
            policies,
            TpsComponent.BPM,
            TpsObjectType.BILLING_PROFILE);
    verify(tpsApiDispatch).deletePao(profile.billingProfile().id());
  }

  @Test
  void createAzureProfileInaccessibleAppDeployment() {
    when(azureService.getAuthorizedManagedAppDeployments(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    var profile =
        ProfileFixtures.createAzureBillingProfileDescription(
            UUID.randomUUID(), UUID.randomUUID(), "fake");

    assertThrows(
        InaccessibleApplicationDeploymentException.class,
        () -> profileService.createProfile(profile, userRequest, null));
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
    var profile = ProfileFixtures.createAzureBillingProfileDescription(tenantId, subId, mrgId);

    profileService.createProfile(profile, userRequest, null);

    verify(samService).createManagedResourceGroup(profile.billingProfile(), userRequest);
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
    var profile = ProfileFixtures.createAzureBillingProfileDescription(tenantId, subId, mrgId);

    assertThrows(
        SamInterruptedException.class,
        () -> profileService.createProfile(profile, userRequest, null));

    verify(samService).deleteManagedResourceGroup(profile.billingProfile().id(), userRequest);
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
        ProfileFixtures.createAzureBillingProfileDescription(tenantId, subId, mrgId, policies);

    profileService.createProfile(profile, userRequest, null);

    verify(tpsApiDispatch)
        .createPao(
            profile.billingProfile().id(),
            policies,
            TpsComponent.BPM,
            TpsObjectType.BILLING_PROFILE);
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
        ProfileFixtures.createAzureBillingProfileDescription(tenantId, subId, mrgId, policies);

    assertThrows(
        PolicyServiceAPIException.class,
        () -> profileService.createProfile(profile, userRequest, null));

    verify(tpsApiDispatch)
        .createPao(
            profile.billingProfile().id(),
            policies,
            TpsComponent.BPM,
            TpsObjectType.BILLING_PROFILE);
    verify(tpsApiDispatch).deletePao(profile.billingProfile().id());
  }

  @Test
  void metricsAreCalledOnProfileCreation() {
    try (var metricsMock = mockStatic(MetricUtils.class)) {
      var profile = ProfileFixtures.createGcpBillingProfileDescription("ABCD1234");
      metricsMock
          .when(() -> MetricUtils.recordProfileCreation(any(), eq(CloudPlatform.GCP)))
          .thenReturn(profile);
      profileService.createProfile(profile, userRequest, null);
      metricsMock.verify(() -> MetricUtils.recordProfileCreation(any(), eq(CloudPlatform.GCP)));
    }
  }
}
