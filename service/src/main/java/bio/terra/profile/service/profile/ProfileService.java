package bio.terra.profile.service.profile;

import bio.terra.common.exception.ForbiddenException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.policy.model.TpsComponent;
import bio.terra.policy.model.TpsObjectType;
import bio.terra.policy.model.TpsPaoGetResult;
import bio.terra.policy.model.TpsPolicyInputs;
import bio.terra.profile.app.common.MetricUtils;
import bio.terra.profile.app.configuration.EnterpriseConfiguration;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.Organization;
import bio.terra.profile.model.SamPolicyModel;
import bio.terra.profile.model.UpdateProfileRequest;
import bio.terra.profile.service.gcp.GcpService;
import bio.terra.profile.service.iam.SamRethrow;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.iam.model.SamAction;
import bio.terra.profile.service.iam.model.SamResourceType;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.profile.service.job.JobService;
import bio.terra.profile.service.policy.TpsApiDispatch;
import bio.terra.profile.service.policy.exception.PolicyServiceAPIException;
import bio.terra.profile.service.profile.exception.ProfileNotFoundException;
import bio.terra.profile.service.profile.flight.ProfileMapKeys;
import bio.terra.profile.service.profile.flight.create.CreateProfileFlight;
import bio.terra.profile.service.profile.flight.delete.DeleteProfileFlight;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.profile.service.profile.model.ProfileDescription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
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
  private final TpsApiDispatch tpsApiDispatch;
  private final EnterpriseConfiguration enterpriseConfiguration;
  private final GcpService gcpService;

  @Autowired
  public ProfileService(
      ProfileDao profileDao,
      SamService samService,
      JobService jobService,
      TpsApiDispatch tpsApiDispatch,
      GcpService gcpService,
      EnterpriseConfiguration enterpriseConfiguration) {
    this.profileDao = profileDao;
    this.samService = samService;
    this.jobService = jobService;
    this.tpsApiDispatch = tpsApiDispatch;
    this.enterpriseConfiguration = enterpriseConfiguration;
    this.gcpService = gcpService;
  }

  /**
   * Create a new billing profileDescription.
   *
   * @param profileDescription billing profileDescription to be created
   * @param user authenticated user
   * @return jobId of the submitted Stairway job
   */
  public ProfileDescription createProfile(
      ProfileDescription profileDescription, AuthenticatedUserRequest user) {
    String description =
        String.format(
            "Create billing profile id [%s] on cloud platform [%s]",
            profileDescription.billingProfile().id(),
            profileDescription.billingProfile().cloudPlatform());
    logger.info(description);
    var createJob =
        jobService
            .newJob()
            .description(description)
            .flightClass(CreateProfileFlight.class)
            .request(profileDescription)
            .userRequest(user)
            .addParameter(
                ProfileMapKeys.ORGANIZATION,
                getProfileOrganization(profileDescription.billingProfile()));
    Callable<ProfileDescription> executeProfileCreation =
        () -> createJob.submitAndWait(ProfileDescription.class);
    return MetricUtils.recordProfileCreation(
        executeProfileCreation, profileDescription.billingProfile().cloudPlatform());
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
  public ProfileDescription getProfile(UUID id, AuthenticatedUserRequest user) {
    // Check Sam permissions before checking in database
    var hasActions =
        SamRethrow.onInterrupted(
            () -> samService.hasActions(user, SamResourceType.PROFILE, id), "hasActions");
    if (Boolean.FALSE.equals(hasActions)) {
      throw new ForbiddenException("forbidden");
    }
    // Throws 404 if not found
    BillingProfile profile = profileDao.getBillingProfileById(id);

    return profileDescription(profile);
  }

  public List<ProfileDescription> listProfiles(
      AuthenticatedUserRequest user, int offset, int limit) {
    List<UUID> samProfileIds =
        SamRethrow.onInterrupted(() -> samService.listProfileIds(user), "listProfileIds");
    var profiles = profileDao.listBillingProfiles(offset, limit, samProfileIds);

    try {
      var policyById =
          tpsApiDispatch.listPaos(profiles.stream().map(BillingProfile::id).toList()).stream()
              .collect(
                  Collectors.toMap(
                      TpsPaoGetResult::getObjectId, TpsPaoGetResult::getEffectiveAttributes));

      return profiles.stream()
          .map(
              profile ->
                  new ProfileDescription(
                      profile,
                      Optional.ofNullable(policyById.get(profile.id())),
                      Optional.of(getProfileOrganization(profile))))
          .toList();
    } catch (InterruptedException e) {
      throw new PolicyServiceAPIException("Interrupted during TPS listPaos operation.", e);
    }
  }

  public ProfileDescription updateProfile(
      UUID id, UpdateProfileRequest requestBody, AuthenticatedUserRequest user) {
    if (requestBody.getDescription() != null) {
      SamRethrow.onInterrupted(
          () ->
              samService.verifyAuthorization(
                  user, SamResourceType.PROFILE, id, SamAction.UPDATE_METADATA),
          "verifyMetadataUpdateAuthz");
    }

    if (requestBody.getBillingAccountId() != null) {
      SamRethrow.onInterrupted(
          () ->
              samService.verifyAuthorization(
                  user, SamResourceType.PROFILE, id, SamAction.UPDATE_BILLING_ACCOUNT),
          "verifyBillingAccountUpdateAuthz");
      gcpService.verifyUserBillingAccountAccess(
          Optional.of(requestBody.getBillingAccountId()), user);
    }

    if (!profileDao.updateProfile(
        id, requestBody.getDescription(), requestBody.getBillingAccountId())) {
      throw new ProfileNotFoundException(String.format("Profile %s not found, update failed.", id));
    }
    return profileDescription(profileDao.getBillingProfileById(id));
  }

  public void removeBillingAccount(UUID id, AuthenticatedUserRequest user) {
    SamRethrow.onInterrupted(
        () ->
            samService.verifyAuthorization(
                user, SamResourceType.PROFILE, id, SamAction.UPDATE_BILLING_ACCOUNT),
        "verifyRemoveBillingAccountAuthz");

    profileDao.removeBillingAccount(id);
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

  private Organization getProfileOrganization(BillingProfile profile) {
    return new Organization()
        .enterprise(
            profile
                .subscriptionId()
                .map(enterpriseConfiguration.subscriptions()::contains)
                .orElse(false));
  }

  private ProfileDescription profileDescription(BillingProfile profile) {
    Optional<TpsPolicyInputs> policies;
    try {
      policies =
          Optional.ofNullable(
              tpsApiDispatch
                  .getOrCreatePao(profile.id(), TpsComponent.BPM, TpsObjectType.BILLING_PROFILE)
                  .getEffectiveAttributes());
    } catch (InterruptedException e) {
      throw new PolicyServiceAPIException("Interrupted during TPS getPao operation.", e);
    }

    return new ProfileDescription(profile, policies, Optional.of(getProfileOrganization(profile)));
  }
}
