package bio.terra.profile.service.profile;

import bio.terra.common.exception.ForbiddenException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.common.MetricUtils;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.SamPolicyModel;
import bio.terra.profile.service.iam.SamRethrow;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.iam.model.SamAction;
import bio.terra.profile.service.iam.model.SamResourceType;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.profile.service.job.JobService;
import bio.terra.profile.service.profile.exception.ProfileNotFoundException;
import bio.terra.profile.service.profile.flight.ProfileMapKeys;
import bio.terra.profile.service.profile.flight.create.CreateProfileFlight;
import bio.terra.profile.service.profile.flight.delete.DeleteProfileFlight;
import bio.terra.profile.service.profile.model.BillingProfile;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
  private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

  private final ProfileDao profileDao;
  private final SamService samService;
  private final JobService jobService;

  @Autowired
  public ProfileService(ProfileDao profileDao, SamService samService, JobService jobService) {
    this.profileDao = profileDao;
    this.samService = samService;
    this.jobService = jobService;
  }

  /**
   * Create a new billing profile.
   *
   * @param profile billing profile to be created
   * @param user authenticated user
   * @return jobId of the submitted Stairway job
   */
  public BillingProfile createProfile(BillingProfile profile, AuthenticatedUserRequest user) {
    String description =
        String.format(
            "Create billing profile id [%s] on cloud platform [%s]",
            profile.id(), profile.cloudPlatform());
    logger.info(description);
    var createJob =
        jobService
            .newJob()
            .description(description)
            .flightClass(CreateProfileFlight.class)
            .request(profile)
            .userRequest(user);
    Callable<BillingProfile> executeProfileCreation =
        () -> createJob.submitAndWait(BillingProfile.class);
    return MetricUtils.recordProfileCreation(executeProfileCreation, profile.cloudPlatform());
  }

  /**
   * Delete billing profile. Blocks until the deletion is complete.
   *
   * @param id unique ID of the billing profile to delete
   * @param user authenticated user
   */
  public void deleteProfile(UUID id, AuthenticatedUserRequest user) {
    SamRethrow.onInterrupted(
        () -> samService.verifyAuthorization(user, SamResourceType.PROFILE, id, SamAction.DELETE),
        "verifyAuthorization");
    var billingProfile = profileDao.getBillingProfileById(id);
    var platform = billingProfile.cloudPlatform();
    var description =
        String.format("Delete billing profile id [%s] on cloud platform [%s]", id, platform);
    logger.info(description);
    var deleteJob =
        jobService
            .newJob()
            .description(description)
            .flightClass(DeleteProfileFlight.class)
            .userRequest(user)
            .addParameter(ProfileMapKeys.PROFILE, billingProfile)
            .addParameter(JobMapKeys.CLOUD_PLATFORM.getKeyName(), platform.name());
    deleteJob.submitAndWait(null);
    MetricUtils.incrementProfileDeletion(platform);
  }

  /**
   * Retrieves a billing profile by ID.
   *
   * @param id unique ID of the billing profile to retrieve
   * @param user authenticated user
   * @return billing profile model
   * @throws ProfileNotFoundException when the profile is not found
   */
  public BillingProfile getProfile(UUID id, AuthenticatedUserRequest user) {
    // Throws 404 if not found
    var profile = profileDao.getBillingProfileById(id);
    // If profile was found, check permissions
    var hasActions =
        SamRethrow.onInterrupted(
            () -> samService.hasActions(user, SamResourceType.PROFILE, id), "hasActions");
    if (Boolean.FALSE.equals(hasActions)) {
      throw new ForbiddenException("forbidden");
    }
    return profile;
  }

  public List<BillingProfile> listProfiles(AuthenticatedUserRequest user, int offset, int limit) {
    List<UUID> samProfileIds =
        SamRethrow.onInterrupted(() -> samService.listProfileIds(user), "listProfileIds");
    return profileDao.listBillingProfiles(offset, limit, samProfileIds);
  }

  public List<SamPolicyModel> getProfilePolicies(UUID profileId, AuthenticatedUserRequest user) {
    return SamRethrow.onInterrupted(
        () -> samService.retrieveProfilePolicies(user, profileId), "retrieveProfilePolicies");
  }

  public SamPolicyModel addProfilePolicyMember(
      UUID profileId, String policyName, String memberEmail, AuthenticatedUserRequest user) {
    return SamRethrow.onInterrupted(
        () -> samService.addProfilePolicyMember(user, profileId, policyName, memberEmail),
        "addProfilePolicyMember");
  }

  public SamPolicyModel deleteProfilePolicyMember(
      UUID profileId, String policyName, String memberEmail, AuthenticatedUserRequest user) {
    return SamRethrow.onInterrupted(
        () -> samService.deleteProfilePolicyMember(user, profileId, policyName, memberEmail),
        "deletePolicyMember");
  }
}
