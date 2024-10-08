package bio.terra.profile.service.profile;

import static org.hamcrest.MatcherAssert.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bio.terra.cloudres.google.billing.CloudBillingClientCow;
import bio.terra.common.exception.BadRequestException;
import bio.terra.common.exception.ForbiddenException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.stairway.StairwayComponent;
import bio.terra.policy.model.TpsComponent;
import bio.terra.policy.model.TpsObjectType;
import bio.terra.policy.model.TpsPaoGetResult;
import bio.terra.policy.model.TpsPolicyInput;
import bio.terra.policy.model.TpsPolicyInputs;
import bio.terra.profile.app.configuration.EnterpriseConfiguration;
import bio.terra.profile.app.configuration.LimitsConfiguration;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.common.ProfileFixtures;
import bio.terra.profile.db.ProfileChangeLogDao;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.model.Organization;
import bio.terra.profile.model.SamPolicyModel;
import bio.terra.profile.model.UpdateProfileRequest;
import bio.terra.profile.service.gcp.GcpService;
import bio.terra.profile.service.gcp.exception.InaccessibleBillingAccountException;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.iam.model.SamAction;
import bio.terra.profile.service.iam.model.SamResourceType;
import bio.terra.profile.service.job.JobBuilder;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.profile.service.job.JobService;
import bio.terra.profile.service.policy.TpsApiDispatch;
import bio.terra.profile.service.profile.exception.ProfileNotFoundException;
import bio.terra.profile.service.profile.flight.ProfileMapKeys;
import bio.terra.profile.service.profile.flight.create.CreateProfileFlight;
import bio.terra.profile.service.profile.flight.delete.DeleteProfileFlight;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.profile.service.profile.model.ProfileDescription;
import com.google.iam.v1.TestIamPermissionsResponse;
import io.opentelemetry.api.OpenTelemetry;
import java.time.Instant;
import java.util.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class ProfileServiceUnitTest extends BaseUnitTest {

  @Mock private ProfileDao profileDao;
  @Mock private SamService samService;
  @Mock private JobService jobService;
  @Mock private TpsApiDispatch tpsApiDispatch;
  @Mock private GcpService gcpService;
  @Mock private EnterpriseConfiguration enterpriseConfiguration;
  @Mock private LimitsConfiguration limitsConfiguration;
  @Mock private ProfileChangeLogDao changeLogDao;

  private ProfileService profileService;
  private AuthenticatedUserRequest user;
  private BillingProfile profile;
  private ProfileDescription profileDescription;
  private List<SamPolicyModel> profilePolicies;
  private SamPolicyModel userPolicy;
  private SamPolicyModel ownerPolicy;

  AuthenticatedUserRequest userRequest =
      AuthenticatedUserRequest.builder()
          .setToken("fake-token")
          .setSubjectId("fake-sub")
          .setEmail("example@example.com")
          .build();

  @BeforeEach
  void before() {
    profileService =
        new ProfileService(
            profileDao,
            changeLogDao,
            samService,
            jobService,
            tpsApiDispatch,
            gcpService,
            enterpriseConfiguration,
            limitsConfiguration);
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
    profileDescription =
        new ProfileDescription(
            profile,
            Optional.empty(),
            Optional.of(new Organization().enterprise(false).limits(Map.of())));

    userPolicy = new SamPolicyModel().name("user").members(List.of("user@unit.com"));
    ownerPolicy = new SamPolicyModel().name("owner").members(List.of("owner@unit.com"));
    profilePolicies = List.of(userPolicy, ownerPolicy);
  }

  @Test
  void createProfile() {
    var jobBuilder = mock(JobBuilder.class);

    when(jobService.newJob()).thenReturn(jobBuilder);
    when(jobBuilder.description(anyString())).thenReturn(jobBuilder);
    when(jobBuilder.flightClass(CreateProfileFlight.class)).thenReturn(jobBuilder);
    when(jobBuilder.request(profileDescription)).thenReturn(jobBuilder);
    when(jobBuilder.userRequest(user)).thenReturn(jobBuilder);
    when(jobBuilder.addParameter(eq(ProfileMapKeys.ORGANIZATION), any())).thenReturn(jobBuilder);
    when(jobBuilder.addParameter(eq(JobMapKeys.INITIATING_USER.getKeyName()), any()))
        .thenReturn(jobBuilder);
    when(jobBuilder.submitAndWait(ProfileDescription.class)).thenReturn(profileDescription);

    ProfileDescription result = profileService.createProfile(profileDescription, user, null);

    verify(jobBuilder).submitAndWait(any());
    assertEquals(profileDescription, result);
  }

  @Test
  void createProfileSpecifyingUserThrowsForNonAdmin() throws InterruptedException {
    var specifiedUser = UUID.randomUUID().toString();
    doThrow(new ForbiddenException(""))
        .when(samService)
        .verifyResourceAdmin(user, SamResourceType.PROFILE, SamAction.SPECIFY_ACTING_USER);

    assertThrows(
        ForbiddenException.class,
        () -> profileService.createProfile(profileDescription, user, specifiedUser));

    verify(samService)
        .verifyResourceAdmin(user, SamResourceType.PROFILE, SamAction.SPECIFY_ACTING_USER);
    verifyNoInteractions(profileDao);
    verifyNoInteractions(tpsApiDispatch);
    verifyNoInteractions(gcpService);
    verifyNoInteractions(changeLogDao);
  }

  @Test
  void createProfileUsesSpecifiedUserWhenAdmin() throws Exception {
    var specifiedUser = UUID.randomUUID().toString();
    var jobBuilder = mock(JobBuilder.class);
    when(jobService.newJob()).thenReturn(jobBuilder);
    when(jobBuilder.description(anyString())).thenReturn(jobBuilder);
    when(jobBuilder.flightClass(CreateProfileFlight.class)).thenReturn(jobBuilder);
    when(jobBuilder.request(profileDescription)).thenReturn(jobBuilder);
    when(jobBuilder.userRequest(user)).thenReturn(jobBuilder);
    when(jobBuilder.addParameter(eq(ProfileMapKeys.ORGANIZATION), any())).thenReturn(jobBuilder);
    when(jobBuilder.addParameter(eq(JobMapKeys.INITIATING_USER.getKeyName()), any()))
        .thenReturn(jobBuilder);
    when(jobBuilder.submitAndWait(ProfileDescription.class)).thenReturn(profileDescription);

    profileService.createProfile(profileDescription, user, specifiedUser);

    verify(jobBuilder).addParameter(JobMapKeys.INITIATING_USER.getKeyName(), specifiedUser);
    verify(samService)
        .verifyResourceAdmin(user, SamResourceType.PROFILE, SamAction.SPECIFY_ACTING_USER);
  }

  @Test
  void deleteProfile() throws InterruptedException {
    var jobBuilder = mock(JobBuilder.class);
    String jobId = "jobId";
    when(jobService.newJob()).thenReturn(jobBuilder);
    when(jobBuilder.submit()).thenReturn(jobId);
    when(jobBuilder.description(anyString())).thenReturn(jobBuilder);
    when(jobBuilder.flightClass(DeleteProfileFlight.class)).thenReturn(jobBuilder);
    when(jobBuilder.userRequest(user)).thenReturn(jobBuilder);
    when(jobBuilder.addParameter(ProfileMapKeys.PROFILE, profile)).thenReturn(jobBuilder);
    when(jobBuilder.addParameter(eq(JobMapKeys.INITIATING_USER.getKeyName()), any()))
        .thenReturn(jobBuilder);
    when(jobBuilder.addParameter(
            eq(JobMapKeys.CLOUD_PLATFORM.getKeyName()), eq(CloudPlatform.GCP.name())))
        .thenReturn(jobBuilder);
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);

    profileService.deleteProfile(profile.id(), user, null);

    verify(samService)
        .verifyAuthorization(user, SamResourceType.PROFILE, profile.id(), SamAction.DELETE.DELETE);
    verify(jobBuilder).submitAndWait(any());
  }

  @Test
  void deleteProfileNoAccess() throws InterruptedException {
    doThrow(ForbiddenException.class)
        .when(samService)
        .verifyAuthorization(eq(user), eq(SamResourceType.PROFILE), any(), eq(SamAction.DELETE));
    assertThrows(
        ForbiddenException.class,
        () -> profileService.deleteProfile(UUID.randomUUID(), user, null));
  }

  @Test
  void getProfile() throws InterruptedException {
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);
    when(tpsApiDispatch.getOrCreatePao(any(), any(), any())).thenReturn(new TpsPaoGetResult());
    var result = profileService.getProfile(profile.id(), user);
    assertEquals(profileDescription, result);
    verify(samService)
        .verifyAuthorization(user, SamResourceType.PROFILE, profile.id(), SamAction.READ_PROFILE);
  }

  @Test
  void getProfileNoAccess() throws InterruptedException {
    doThrow(new ForbiddenException("forbidden"))
        .when(samService)
        .verifyAuthorization(user, SamResourceType.PROFILE, profile.id(), SamAction.READ_PROFILE);
    assertThrows(ForbiddenException.class, () -> profileService.getProfile(profile.id(), user));
  }

  @Test
  void getProfileWithPolicies() throws InterruptedException {
    var policies =
        new TpsPolicyInputs()
            .addInputsItem(new TpsPolicyInput().namespace("terra").name("protected-data"));
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);
    when(tpsApiDispatch.getOrCreatePao(
            profile.id(), TpsComponent.BPM, TpsObjectType.BILLING_PROFILE))
        .thenReturn(new TpsPaoGetResult().effectiveAttributes(policies));
    var result = profileService.getProfile(profile.id(), user);
    assertEquals(
        new ProfileDescription(
            profile,
            Optional.of(policies),
            Optional.of(new Organization().enterprise(false).limits(Map.of()))),
        result);
    verify(samService)
        .verifyAuthorization(user, SamResourceType.PROFILE, profile.id(), SamAction.READ_PROFILE);
  }

  @Test
  void getProfileWithEnterpriseOrganization() throws InterruptedException {
    var enterpriseSubscription = UUID.randomUUID();
    when(enterpriseConfiguration.subscriptions()).thenReturn(Set.of(enterpriseSubscription));
    var enterpriseProfile =
        ProfileFixtures.createAzureBillingProfile(
            UUID.randomUUID(), enterpriseSubscription, "enterpriseMRG");
    var nonEnterpriseProfile =
        ProfileFixtures.createAzureBillingProfile(
            UUID.randomUUID(), UUID.randomUUID(), "nonEnterpriseMRG");

    when(profileDao.getBillingProfileById(enterpriseProfile.id())).thenReturn(enterpriseProfile);
    when(profileDao.getBillingProfileById(nonEnterpriseProfile.id()))
        .thenReturn(nonEnterpriseProfile);
    when(tpsApiDispatch.getOrCreatePao(any(), any(), any())).thenReturn(new TpsPaoGetResult());

    var enterpriseResult = profileService.getProfile(enterpriseProfile.id(), user);
    var nonEnterpriseResult = profileService.getProfile(nonEnterpriseProfile.id(), user);

    assertTrue(enterpriseResult.organization().get().isEnterprise());
    assertFalse(nonEnterpriseResult.organization().get().isEnterprise());
  }

  @Test
  void getProfileWithLimits() throws InterruptedException {
    var organizationSubscription = UUID.randomUUID();
    var limitedProfile =
        ProfileFixtures.createAzureBillingProfile(
            UUID.randomUUID(), organizationSubscription, "limitedMRG");
    var nonLimitedProfile =
        ProfileFixtures.createAzureBillingProfile(
            UUID.randomUUID(), organizationSubscription, "nonLimitedMRG");
    Map<String, String> limitMap = Map.of("vcpus", "4");
    when(limitsConfiguration.getLimitsForProfile(limitedProfile.id())).thenReturn(limitMap);
    when(limitsConfiguration.getLimitsForProfile(nonLimitedProfile.id()))
        .thenReturn(Collections.emptyMap());

    when(profileDao.getBillingProfileById(limitedProfile.id())).thenReturn(limitedProfile);
    when(profileDao.getBillingProfileById(nonLimitedProfile.id())).thenReturn(nonLimitedProfile);
    when(tpsApiDispatch.getOrCreatePao(any(), any(), any())).thenReturn(new TpsPaoGetResult());

    var limitedResult = profileService.getProfile(limitedProfile.id(), user);
    var nonLimitedResult = profileService.getProfile(nonLimitedProfile.id(), user);

    var limits = limitedResult.organization().get().getLimits();
    assertTrue(limits.containsKey("vcpus"));
    var noLimits = nonLimitedResult.organization().get().getLimits();
    assertTrue(noLimits.isEmpty());
  }

  @Test
  void listProfiles() throws InterruptedException {
    when(samService.listProfileIds(user)).thenReturn(List.of(profile.id()));
    when(profileDao.listBillingProfiles(anyInt(), anyInt(), eq(List.of(profile.id()))))
        .thenReturn(List.of(profile));
    when(tpsApiDispatch.getOrCreatePao(any(), any(), any())).thenReturn(new TpsPaoGetResult());

    var result = profileService.listProfiles(user, 0, 0);

    assertEquals(List.of(profileDescription), result);
  }

  @Test
  void listProfilesWithPolicies() throws InterruptedException {
    var policies =
        new TpsPolicyInputs()
            .addInputsItem(new TpsPolicyInput().namespace("terra").name("protected-data"));
    var protectedProfile =
        new BillingProfile(
            UUID.randomUUID(),
            "protected_name",
            "",
            "direct",
            CloudPlatform.AZURE,
            Optional.empty(),
            Optional.of(UUID.randomUUID()),
            Optional.of(UUID.randomUUID()),
            Optional.of("protectedMrgName"),
            Instant.now(),
            Instant.now(),
            "creator");
    var enterpriseSubscription = UUID.randomUUID();
    var enterpriseProfile =
        ProfileFixtures.createAzureBillingProfile(
            UUID.randomUUID(), enterpriseSubscription, "enterpriseMRG");
    when(enterpriseConfiguration.subscriptions()).thenReturn(Set.of(enterpriseSubscription));
    when(samService.listProfileIds(user))
        .thenReturn(List.of(profile.id(), protectedProfile.id(), enterpriseProfile.id()));
    when(profileDao.listBillingProfiles(
            anyInt(),
            anyInt(),
            eq(List.of(profile.id(), protectedProfile.id(), enterpriseProfile.id()))))
        .thenReturn(List.of(profile, protectedProfile, enterpriseProfile));
    when(tpsApiDispatch.listPaos(argThat(l -> l.contains(profile.id()))))
        .thenReturn(
            List.of(
                new TpsPaoGetResult()
                    .effectiveAttributes(policies)
                    .objectId(protectedProfile.id())));

    var result = profileService.listProfiles(user, 0, 0);

    assertThat(
        result,
        Matchers.containsInAnyOrder(
            profileDescription,
            new ProfileDescription(
                protectedProfile,
                Optional.of(policies),
                Optional.of(new Organization().enterprise(false).limits(Map.of()))),
            new ProfileDescription(
                enterpriseProfile,
                Optional.empty(),
                Optional.of(new Organization().enterprise(true).limits(Map.of())))));
  }

  @Test
  void updateProfileSuccess() throws InterruptedException {
    var newBillingAccount = "newBillingAccount";
    var newDescription = "newDescription";
    var updateRequest =
        new UpdateProfileRequest().description(newDescription).billingAccountId(newBillingAccount);
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
    when(profileDao.updateProfile(profile.id(), newDescription, newBillingAccount))
        .thenReturn(true);
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(updatedProfile);
    when(tpsApiDispatch.getOrCreatePao(any(), any(), any())).thenReturn(new TpsPaoGetResult());
    var expectedChanges =
        Map.of("description", newDescription, "billing_account_id", newBillingAccount);
    when(changeLogDao.recordProfileUpdate(profile.id(), user.getSubjectId(), expectedChanges))
        .thenReturn(Optional.of(UUID.randomUUID()));

    var res = profileService.updateProfile(profile.id(), updateRequest, user);

    assertEquals(newBillingAccount, res.billingProfile().billingAccountId().get());
    assertEquals(newDescription, res.billingProfile().description());
    verify(samService)
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_METADATA);
    verify(samService)
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_BILLING_ACCOUNT);
    verify(gcpService).verifyUserBillingAccountAccess(Optional.of(newBillingAccount), user);
    verify(changeLogDao).recordProfileUpdate(profile.id(), user.getSubjectId(), expectedChanges);
  }

  @Test
  void updateProfileBillingAccountOnly() throws InterruptedException {
    var newBillingAccount = "newBillingAccount";
    var updateRequest = new UpdateProfileRequest().billingAccountId(newBillingAccount);
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

    when(profileDao.updateProfile(profile.id(), null, newBillingAccount)).thenReturn(true);
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(updatedProfile);
    when(tpsApiDispatch.getOrCreatePao(any(), any(), any())).thenReturn(new TpsPaoGetResult());
    var expectedChanges = Map.of("billing_account_id", newBillingAccount);
    when(changeLogDao.recordProfileUpdate(profile.id(), user.getSubjectId(), expectedChanges))
        .thenReturn(Optional.of(UUID.randomUUID()));

    var res = profileService.updateProfile(profile.id(), updateRequest, user);

    assertEquals(newBillingAccount, res.billingProfile().billingAccountId().get());
    assertEquals(profile.description(), res.billingProfile().description());
    verify(samService, times(0))
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_METADATA);
    verify(samService)
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_BILLING_ACCOUNT);
    verify(gcpService).verifyUserBillingAccountAccess(Optional.of(newBillingAccount), user);
    verify(changeLogDao).recordProfileUpdate(profile.id(), user.getSubjectId(), expectedChanges);
  }

  @Test
  void updateProfileDescriptionOnly() throws InterruptedException {
    var newDescription = "newDescription";
    var updateRequest = new UpdateProfileRequest().description(newDescription);
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

    when(profileDao.updateProfile(profile.id(), newDescription, null)).thenReturn(true);
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(updatedProfile);
    when(tpsApiDispatch.getOrCreatePao(any(), any(), any())).thenReturn(new TpsPaoGetResult());
    var expectedChanges = Map.of("description", newDescription);
    when(changeLogDao.recordProfileUpdate(profile.id(), user.getSubjectId(), expectedChanges))
        .thenReturn(Optional.of(UUID.randomUUID()));

    var res = profileService.updateProfile(profile.id(), updateRequest, user);

    assertEquals(
        profile.getRequiredBillingAccountId(), res.billingProfile().billingAccountId().get());
    assertEquals(newDescription, res.billingProfile().description());
    verify(samService)
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_METADATA);
    verify(samService, times(0))
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_BILLING_ACCOUNT);
    verifyNoInteractions(gcpService);
    verify(changeLogDao).recordProfileUpdate(profile.id(), user.getSubjectId(), expectedChanges);
  }

  @Test
  void updateProfileRecordsChangesAsInitiatingUserWhenSpecified() throws InterruptedException {
    var newDescription = "newDescription";
    var specifiedUser = UUID.randomUUID().toString();
    var updateRequest =
        new UpdateProfileRequest().description(newDescription).initiatingUser(specifiedUser);
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
    when(profileDao.updateProfile(profile.id(), newDescription, null)).thenReturn(true);
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(updatedProfile);
    when(tpsApiDispatch.getOrCreatePao(any(), any(), any())).thenReturn(new TpsPaoGetResult());
    var expectedChanges = Map.of("description", newDescription);
    when(changeLogDao.recordProfileUpdate(profile.id(), specifiedUser, expectedChanges))
        .thenReturn(Optional.of(UUID.randomUUID()));

    var res = profileService.updateProfile(profile.id(), updateRequest, user);

    assertEquals(
        profile.getRequiredBillingAccountId(), res.billingProfile().billingAccountId().get());
    assertEquals(newDescription, res.billingProfile().description());
    verify(samService)
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_METADATA);
    verify(samService, times(0))
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_BILLING_ACCOUNT);
    verifyNoInteractions(gcpService);
    verify(samService)
        .verifyResourceAdmin(user, SamResourceType.PROFILE, SamAction.SPECIFY_ACTING_USER);
    verify(changeLogDao).recordProfileUpdate(profile.id(), specifiedUser, expectedChanges);
  }

  @Test
  void updateProfileSpecifyingUserThrowsForNonAdmin() throws InterruptedException {
    var newDescription = "newDescription";
    var specifiedUser = UUID.randomUUID().toString();
    var updateRequest =
        new UpdateProfileRequest().description(newDescription).initiatingUser(specifiedUser);
    doThrow(new ForbiddenException(""))
        .when(samService)
        .verifyResourceAdmin(user, SamResourceType.PROFILE, SamAction.SPECIFY_ACTING_USER);

    assertThrows(
        ForbiddenException.class,
        () -> profileService.updateProfile(profile.id(), updateRequest, user));

    verifyNoInteractions(profileDao);
    verifyNoInteractions(tpsApiDispatch);
    verifyNoInteractions(gcpService);
    verifyNoInteractions(changeLogDao);
    verify(samService)
        .verifyResourceAdmin(user, SamResourceType.PROFILE, SamAction.SPECIFY_ACTING_USER);
  }

  @Test
  void updateProfileDescriptionNoSamAccess() throws InterruptedException {
    var newDescription = "newDescription";
    var updateRequest = new UpdateProfileRequest().description(newDescription);

    doThrow(new ForbiddenException("forbidden"))
        .when(samService)
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_METADATA);

    assertThrows(
        ForbiddenException.class,
        () -> profileService.updateProfile(profile.id(), updateRequest, user));
    verify(samService)
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_METADATA);
    verify(samService, times(0))
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_BILLING_ACCOUNT);
    verifyNoInteractions(gcpService);
    verifyNoInteractions(profileDao);
  }

  @Test
  void updateProfileBillingAccountNoSamAccess() throws InterruptedException {
    var newBillingAccount = "newBillingAccount";
    var updateRequest = new UpdateProfileRequest().billingAccountId(newBillingAccount);

    doThrow(new ForbiddenException("forbidden"))
        .when(samService)
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_BILLING_ACCOUNT);

    assertThrows(
        ForbiddenException.class,
        () -> profileService.updateProfile(profile.id(), updateRequest, user));
    verify(samService, times(0))
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_METADATA);
    verify(samService)
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_BILLING_ACCOUNT);
    verifyNoInteractions(gcpService);
    verifyNoInteractions(profileDao);
  }

  @Test
  void updateProfileBillingAccountNoGcpAccess() throws InterruptedException {
    var newBillingAccount = "newBillingAccount";
    var updateRequest = new UpdateProfileRequest().billingAccountId(newBillingAccount);

    doThrow(new InaccessibleBillingAccountException("forbidden"))
        .when(gcpService)
        .verifyUserBillingAccountAccess(Optional.of(newBillingAccount), user);

    assertThrows(
        InaccessibleBillingAccountException.class,
        () -> profileService.updateProfile(profile.id(), updateRequest, user));
    verify(samService, times(0))
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_METADATA);
    verify(samService)
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_BILLING_ACCOUNT);
    verify(gcpService).verifyUserBillingAccountAccess(Optional.of(newBillingAccount), user);
    verifyNoInteractions(profileDao);
  }

  @Test
  void updateProfileNotFound() {
    var newBillingAccount = "newBillingAccount";
    var newDescription = "newDescription";
    var updateRequest =
        new UpdateProfileRequest().description(newDescription).billingAccountId(newBillingAccount);

    when(profileDao.updateProfile(profile.id(), newDescription, newBillingAccount))
        .thenReturn(false);

    assertThrows(
        ProfileNotFoundException.class,
        () -> profileService.updateProfile(profile.id(), updateRequest, user));
  }

  @Test
  void removeBillingAccount() {
    var expectedChanges = new HashMap<String, String>();
    expectedChanges.put("billing_account_id", null);
    when(changeLogDao.recordProfileUpdate(profile.id(), user.getSubjectId(), expectedChanges))
        .thenReturn(Optional.of(UUID.randomUUID()));

    when(profileDao.removeBillingAccount(profile.id())).thenReturn(true);
    profileService.removeBillingAccount(profile.id(), user, null);

    verify(profileDao).removeBillingAccount(profile.id());
    verify(changeLogDao).recordProfileUpdate(profile.id(), user.getSubjectId(), expectedChanges);
  }

  @Test
  void removeBillingAccountUsesRequestingUserIdWhenSpecified() throws Exception {
    var expectedChanges = new HashMap<String, String>();
    expectedChanges.put("billing_account_id", null);
    var specifiedUserId = UUID.randomUUID().toString();
    when(changeLogDao.recordProfileUpdate(profile.id(), specifiedUserId, expectedChanges))
        .thenReturn(Optional.of(UUID.randomUUID()));

    when(profileDao.removeBillingAccount(profile.id())).thenReturn(true);
    profileService.removeBillingAccount(profile.id(), user, specifiedUserId);

    verify(profileDao).removeBillingAccount(profile.id());
    verify(changeLogDao).recordProfileUpdate(profile.id(), specifiedUserId, expectedChanges);
    verify(samService)
        .verifyResourceAdmin(user, SamResourceType.PROFILE, SamAction.SPECIFY_ACTING_USER);
  }

  @Test
  void removeBillingAccountNoAccess() throws InterruptedException {
    doThrow(new ForbiddenException("forbidden"))
        .when(samService)
        .verifyAuthorization(
            user, SamResourceType.PROFILE, profile.id(), SamAction.UPDATE_BILLING_ACCOUNT);
    when(profileDao.removeBillingAccount(profile.id())).thenReturn(true);
    assertThrows(
        ForbiddenException.class,
        () -> profileService.removeBillingAccount(profile.id(), user, null));

    verifyNoInteractions(profileDao);
  }

  @Test
  void leaveProfile() throws InterruptedException {
    assertDoesNotThrow(() -> profileService.leaveProfile(profile.id(), user));
    verify(samService)
        .leaveResource(user, SamResourceType.PROFILE.getSamResourceName(), profile.id());
  }

  @Test
  void getProfilePolicies() throws InterruptedException {
    when(samService.retrieveProfilePolicies(user, profile.id())).thenReturn(profilePolicies);
    var result = profileService.getProfilePolicies(profile.id(), user);
    assertEquals(profilePolicies, result);
  }

  @Test
  void getProfilePolicies403() throws InterruptedException {
    doThrow(ForbiddenException.class).when(samService).retrieveProfilePolicies(user, profile.id());
    assertThrows(
        ForbiddenException.class, () -> profileService.getProfilePolicies(profile.id(), user));
  }

  @Test
  void getProfilePolicies404() throws InterruptedException {
    doThrow(NotFoundException.class).when(samService).retrieveProfilePolicies(user, profile.id());
    assertThrows(
        NotFoundException.class, () -> profileService.getProfilePolicies(profile.id(), user));
  }

  @Test
  void addProfilePolicyMember() throws InterruptedException {
    when(samService.addProfilePolicyMember(user, profile.id(), "user", "user@unit.com"))
        .thenReturn(userPolicy);
    var result = profileService.addProfilePolicyMember(profile.id(), "user", "user@unit.com", user);
    assertEquals(userPolicy, result);
  }

  @Test
  void addProfilePolicyMember403() throws InterruptedException {
    doThrow(ForbiddenException.class)
        .when(samService)
        .addProfilePolicyMember(user, profile.id(), "user", "user@unit.com");
    assertThrows(
        ForbiddenException.class,
        () -> profileService.addProfilePolicyMember(profile.id(), "user", "user@unit.com", user));
  }

  @Test
  void addProfilePolicyMember404() throws InterruptedException {
    doThrow(NotFoundException.class)
        .when(samService)
        .addProfilePolicyMember(user, profile.id(), "user", "user@unit.com");
    assertThrows(
        NotFoundException.class,
        () -> profileService.addProfilePolicyMember(profile.id(), "user", "user@unit.com", user));
  }

  @Test
  void deleteProfilePolicyMember() throws InterruptedException {
    when(samService.deleteProfilePolicyMember(user, profile.id(), "owner", "leaving@unit.com"))
        .thenReturn(ownerPolicy);
    var result =
        profileService.deleteProfilePolicyMember(profile.id(), "owner", "leaving@unit.com", user);
    assertEquals(ownerPolicy, result);
  }

  @Test
  void deleteProfilePolicyMember400() throws InterruptedException {
    assertThrows(
        BadRequestException.class,
        () ->
            profileService.deleteProfilePolicyMember(profile.id(), "user", user.getEmail(), user));
  }

  @Test
  void deleteProfilePolicyMember403() throws InterruptedException {
    doThrow(ForbiddenException.class)
        .when(samService)
        .deleteProfilePolicyMember(user, profile.id(), "user", "user@unit.com");
    assertThrows(
        ForbiddenException.class,
        () ->
            profileService.deleteProfilePolicyMember(profile.id(), "user", "user@unit.com", user));
  }

  @Test
  void deleteProfilePolicyMember404() throws InterruptedException {
    doThrow(NotFoundException.class)
        .when(samService)
        .deleteProfilePolicyMember(user, profile.id(), "user", "user@unit.com");
    assertThrows(
        NotFoundException.class,
        () ->
            profileService.deleteProfilePolicyMember(profile.id(), "user", "user@unit.com", user));
  }

  @Test
  void createProfileFlightSetup() {
    var billingCow = mock(CloudBillingClientCow.class);
    var iamPermissionsResponse =
        TestIamPermissionsResponse.newBuilder()
            .addAllPermissions(GcpService.BILLING_ACCOUNT_PERMISSIONS_TO_TEST)
            .build();
    when(billingCow.testIamPermissions(any())).thenReturn(iamPermissionsResponse);
    StairwayComponent stairwayComponent = mock(StairwayComponent.class);

    var builder = spy(new JobBuilder(jobService, stairwayComponent, OpenTelemetry.noop()));
    when(jobService.newJob()).thenReturn(builder);

    var profileDescription = ProfileFixtures.createGcpBillingProfileDescription("ABCD1234");
    var profile = profileDescription.billingProfile();
    doReturn(profileDescription).when(builder).submitAndWait(eq(ProfileDescription.class));

    var createdProfileDescription =
        profileService.createProfile(profileDescription, userRequest, null);
    var createdProfile = createdProfileDescription.billingProfile();

    assertEquals(createdProfile.id(), profile.id());
    assertEquals(createdProfile.biller(), profile.biller());
    assertEquals(createdProfile.cloudPlatform(), profile.cloudPlatform());
    assertEquals(createdProfile.billingAccountId().get(), profile.billingAccountId().get());
    assertEquals(createdProfile.displayName(), profile.displayName());
    verify(jobService).newJob();
    verify(builder).description(anyString());
    verify(builder).flightClass(CreateProfileFlight.class);
    verify(builder).request(profileDescription);
    verify(builder).userRequest(userRequest);
  }
}
