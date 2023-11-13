package bio.terra.profile.service.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import bio.terra.cloudres.google.billing.CloudBillingClientCow;
import bio.terra.common.exception.ForbiddenException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.stairway.StairwayComponent;
import bio.terra.profile.app.common.MdcHook;
import bio.terra.profile.common.*;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.model.SamPolicyModel;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.iam.model.SamAction;
import bio.terra.profile.service.iam.model.SamResourceType;
import bio.terra.profile.service.job.JobBuilder;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.profile.service.job.JobService;
import bio.terra.profile.service.profile.flight.ProfileMapKeys;
import bio.terra.profile.service.profile.flight.create.CreateProfileFlight;
import bio.terra.profile.service.profile.flight.create.CreateProfileVerifyAccountStep;
import bio.terra.profile.service.profile.flight.delete.DeleteProfileFlight;
import bio.terra.profile.service.profile.model.BillingProfile;
import com.google.iam.v1.TestIamPermissionsResponse;
import io.opentelemetry.api.OpenTelemetry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class ProfileServiceUnitTest extends BaseUnitTest {

  @Mock private ProfileDao profileDao;
  @Mock private SamService samService;
  @Mock private JobService jobService;

  private ProfileService profileService;
  private AuthenticatedUserRequest user;
  private BillingProfile profile;
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
    profileService = new ProfileService(profileDao, samService, jobService);
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
    when(jobBuilder.request(profile)).thenReturn(jobBuilder);
    when(jobBuilder.userRequest(user)).thenReturn(jobBuilder);
    when(jobBuilder.submitAndWait(BillingProfile.class)).thenReturn(profile);

    BillingProfile result = profileService.createProfile(profile, user);

    verify(jobBuilder).submitAndWait(any());
    assertEquals(profile, result);
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
    when(jobBuilder.addParameter(
            eq(JobMapKeys.CLOUD_PLATFORM.getKeyName()), eq(CloudPlatform.GCP.name())))
        .thenReturn(jobBuilder);
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);

    profileService.deleteProfile(profile.id(), user);
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
        ForbiddenException.class, () -> profileService.deleteProfile(UUID.randomUUID(), user));
  }

  @Test
  void getProfile() throws InterruptedException {
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);
    when(samService.hasActions(eq(user), eq(SamResourceType.PROFILE), eq(profile.id())))
        .thenReturn(true);

    var result = profileService.getProfile(profile.id(), user);
    assertEquals(profile, result);
  }

  @Test
  void getProfileNoAccess() throws InterruptedException {
    when(samService.hasActions(user, SamResourceType.PROFILE, profile.id())).thenReturn(false);
    assertThrows(ForbiddenException.class, () -> profileService.getProfile(profile.id(), user));
  }

  @Test
  void listProfiles() throws InterruptedException {
    when(samService.listProfileIds(user)).thenReturn(List.of(profile.id()));
    when(profileDao.listBillingProfiles(anyInt(), anyInt(), eq(List.of(profile.id()))))
        .thenReturn(List.of(profile));
    var result = profileService.listProfiles(user, 0, 0);
    assertEquals(List.of(profile), result);
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
            .addAllPermissions(CreateProfileVerifyAccountStep.PERMISSIONS_TO_TEST)
            .build();
    when(billingCow.testIamPermissions(any())).thenReturn(iamPermissionsResponse);
    StairwayComponent stairwayComponent = mock(StairwayComponent.class);

    var builder =
        spy(
            new JobBuilder(
                jobService, stairwayComponent, mock(MdcHook.class), OpenTelemetry.noop()));
    when(jobService.newJob()).thenReturn(builder);

    var profile = ProfileFixtures.createGcpBillingProfile("ABCD1234");
    doReturn(profile).when(builder).submitAndWait(eq(BillingProfile.class));

    var createdProfile = profileService.createProfile(profile, userRequest);

    assertEquals(createdProfile.id(), profile.id());
    assertEquals(createdProfile.biller(), profile.biller());
    assertEquals(createdProfile.cloudPlatform(), profile.cloudPlatform());
    assertEquals(createdProfile.billingAccountId().get(), profile.billingAccountId().get());
    assertEquals(createdProfile.displayName(), profile.displayName());
    verify(jobService).newJob();
    verify(builder).description(anyString());
    verify(builder).flightClass(CreateProfileFlight.class);
    verify(builder).request(profile);
    verify(builder).userRequest(userRequest);
  }
}
