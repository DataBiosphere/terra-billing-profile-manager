package bio.terra.profile.service.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.common.exception.ForbiddenException;
import bio.terra.common.exception.UnauthorizedException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.CloudPlatform;
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

public class ProfileServiceUnitTest extends BaseUnitTest {

  @Mock private ProfileDao profileDao;
  @Mock private SamService samService;
  @Mock private JobService jobService;

  private ProfileService profileService;
  private AuthenticatedUserRequest user;
  private BillingProfile profile;

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
            Optional.empty(),
            Instant.now(),
            Instant.now(),
            "creator");
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
    when(jobBuilder.addParameter(eq(ProfileMapKeys.PROFILE_ID), eq(profile.id())))
        .thenReturn(jobBuilder);
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
    assertThrows(UnauthorizedException.class, () -> profileService.getProfile(profile.id(), user));
  }

  @Test
  public void listProfiles() throws InterruptedException {
    when(samService.listProfileIds(eq(user))).thenReturn(List.of(profile.id()));
    when(profileDao.listBillingProfiles(anyInt(), anyInt(), eq(List.of(profile.id()))))
        .thenReturn(List.of(profile));
    var result = profileService.listProfiles(user, 0, 0);
    assertEquals(List.of(profile), result);
  }
}
