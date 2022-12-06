package bio.terra.profile.service.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import bio.terra.common.exception.ForbiddenException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.common.BaseSpringUnitTest;
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
import bio.terra.profile.service.profile.flight.delete.DeleteProfileFlight;
import bio.terra.profile.service.profile.model.BillingProfile;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class ProfileServiceUnitTest extends BaseSpringUnitTest {

  @Mock private ProfileDao profileDao;
  @Mock private SamService samService;
  @Mock private JobService jobService;

  private ProfileService profileService;
  private AuthenticatedUserRequest user;
  private BillingProfile profile;
  private List<SamPolicyModel> profilePolicies;
  private SamPolicyModel userPolicy;
  private SamPolicyModel ownerPolicy;

  @BeforeEach
  public void before() {
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
  public void createProfile() {
    var jobBuilder = mock(JobBuilder.class);

    when(jobService.newJob()).thenReturn(jobBuilder);
    when(jobBuilder.description(anyString())).thenReturn(jobBuilder);
    when(jobBuilder.flightClass(eq(CreateProfileFlight.class))).thenReturn(jobBuilder);
    when(jobBuilder.request(eq(profile))).thenReturn(jobBuilder);
    when(jobBuilder.userRequest(eq(user))).thenReturn(jobBuilder);
    when(jobBuilder.submitAndWait(BillingProfile.class)).thenReturn(profile);

    BillingProfile result = profileService.createProfile(profile, user);

    verify(jobBuilder).submitAndWait(any());
    assertEquals(profile, result);
  }

  @Test
  public void deleteProfile() throws InterruptedException {
    var jobBuilder = mock(JobBuilder.class);
    String jobId = "jobId";

    when(jobService.newJob()).thenReturn(jobBuilder);
    when(jobBuilder.submit()).thenReturn(jobId);
    when(jobBuilder.description(anyString())).thenReturn(jobBuilder);
    when(jobBuilder.flightClass(eq(DeleteProfileFlight.class))).thenReturn(jobBuilder);
    when(jobBuilder.userRequest(eq(user))).thenReturn(jobBuilder);
    when(jobBuilder.addParameter(eq(ProfileMapKeys.PROFILE), eq(profile))).thenReturn(jobBuilder);
    when(jobBuilder.addParameter(
            eq(JobMapKeys.CLOUD_PLATFORM.getKeyName()), eq(CloudPlatform.GCP.name())))
        .thenReturn(jobBuilder);
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);

    profileService.deleteProfile(profile.id(), user);
    verify(samService)
        .verifyAuthorization(
            eq(user), eq(SamResourceType.PROFILE), eq(profile.id()), eq(SamAction.DELETE.DELETE));
    verify(jobBuilder).submitAndWait(any());
  }

  @Test
  public void deleteProfileNoAccess() throws InterruptedException {
    doThrow(ForbiddenException.class)
        .when(samService)
        .verifyAuthorization(eq(user), eq(SamResourceType.PROFILE), any(), eq(SamAction.DELETE));
    assertThrows(
        ForbiddenException.class, () -> profileService.deleteProfile(UUID.randomUUID(), user));
  }

  @Test
  public void getProfile() throws InterruptedException {
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);
    when(samService.hasActions(eq(user), eq(SamResourceType.PROFILE), eq(profile.id())))
        .thenReturn(true);

    var result = profileService.getProfile(profile.id(), user);
    assertEquals(profile, result);
  }

  @Test
  public void getProfileNoAccess() throws InterruptedException {
    when(samService.hasActions(eq(user), eq(SamResourceType.PROFILE), eq(profile.id())))
        .thenReturn(false);
    assertThrows(ForbiddenException.class, () -> profileService.getProfile(profile.id(), user));
  }

  @Test
  public void listProfiles() throws InterruptedException {
    when(samService.listProfileIds(eq(user))).thenReturn(List.of(profile.id()));
    when(profileDao.listBillingProfiles(anyInt(), anyInt(), eq(List.of(profile.id()))))
        .thenReturn(List.of(profile));
    var result = profileService.listProfiles(user, 0, 0);
    assertEquals(List.of(profile), result);
  }

  @Test
  public void getProfilePolicies() throws InterruptedException {
    when(samService.retrieveProfilePolicies(eq(user), eq(profile.id())))
        .thenReturn(profilePolicies);
    var result = profileService.getProfilePolicies(profile.id(), user);
    assertEquals(profilePolicies, result);
  }

  @Test
  public void getProfilePolicies403() throws InterruptedException {
    doThrow(ForbiddenException.class)
        .when(samService)
        .retrieveProfilePolicies(eq(user), eq(profile.id()));
    assertThrows(
        ForbiddenException.class, () -> profileService.getProfilePolicies(profile.id(), user));
  }

  @Test
  public void getProfilePolicies404() throws InterruptedException {
    doThrow(NotFoundException.class)
        .when(samService)
        .retrieveProfilePolicies(eq(user), eq(profile.id()));
    assertThrows(
        NotFoundException.class, () -> profileService.getProfilePolicies(profile.id(), user));
  }

  @Test
  public void addProfilePolicyMember() throws InterruptedException {
    when(samService.addProfilePolicyMember(
            eq(user), eq(profile.id()), eq("user"), eq("user@unit.com")))
        .thenReturn(userPolicy);
    var result = profileService.addProfilePolicyMember(profile.id(), "user", "user@unit.com", user);
    assertEquals(userPolicy, result);
  }

  @Test
  public void addProfilePolicyMember403() throws InterruptedException {
    doThrow(ForbiddenException.class)
        .when(samService)
        .addProfilePolicyMember(eq(user), eq(profile.id()), eq("user"), eq("user@unit.com"));
    assertThrows(
        ForbiddenException.class,
        () -> profileService.addProfilePolicyMember(profile.id(), "user", "user@unit.com", user));
  }

  @Test
  public void addProfilePolicyMember404() throws InterruptedException {
    doThrow(NotFoundException.class)
        .when(samService)
        .addProfilePolicyMember(eq(user), eq(profile.id()), eq("user"), eq("user@unit.com"));
    assertThrows(
        NotFoundException.class,
        () -> profileService.addProfilePolicyMember(profile.id(), "user", "user@unit.com", user));
  }

  @Test
  public void deleteProfilePolicyMember() throws InterruptedException {
    when(samService.deleteProfilePolicyMember(
            eq(user), eq(profile.id()), eq("owner"), eq("leaving@unit.com")))
        .thenReturn(ownerPolicy);
    var result =
        profileService.deleteProfilePolicyMember(profile.id(), "owner", "leaving@unit.com", user);
    assertEquals(ownerPolicy, result);
  }

  @Test
  public void deleteProfilePolicyMember403() throws InterruptedException {
    doThrow(ForbiddenException.class)
        .when(samService)
        .deleteProfilePolicyMember(eq(user), eq(profile.id()), eq("user"), eq("user@unit.com"));
    assertThrows(
        ForbiddenException.class,
        () ->
            profileService.deleteProfilePolicyMember(profile.id(), "user", "user@unit.com", user));
  }

  @Test
  public void deleteProfilePolicyMember404() throws InterruptedException {
    doThrow(NotFoundException.class)
        .when(samService)
        .deleteProfilePolicyMember(eq(user), eq(profile.id()), eq("user"), eq("user@unit.com"));
    assertThrows(
        NotFoundException.class,
        () ->
            profileService.deleteProfilePolicyMember(profile.id(), "user", "user@unit.com", user));
  }
}
