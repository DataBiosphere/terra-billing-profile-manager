package bio.terra.profile.app.controller;

import bio.terra.common.exception.ValidationException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.iam.AuthenticatedUserRequestFactory;
import bio.terra.profile.api.ProfileApi;
import bio.terra.profile.model.*;
import bio.terra.profile.service.job.JobService;
import bio.terra.profile.service.profile.ProfileService;
import bio.terra.profile.service.profile.model.ProfileDescription;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class ProfileApiController implements ProfileApi {
  private final HttpServletRequest request;
  private final ProfileService profileService;
  private final AuthenticatedUserRequestFactory authenticatedUserRequestFactory;

  private static final Logger logger = LoggerFactory.getLogger(ProfileApiController.class);


  @Autowired
  public ProfileApiController(
      HttpServletRequest request,
      ProfileService profileService,
      JobService jobService,
      AuthenticatedUserRequestFactory authenticatedUserRequestFactory) {
    this.request = request;
    this.profileService = profileService;
    this.authenticatedUserRequestFactory = authenticatedUserRequestFactory;
  }

  @Override
  public ResponseEntity<ProfileModel> createProfile(CreateProfileRequest body) {
    AuthenticatedUserRequest user = authenticatedUserRequestFactory.from(request);
    ProfileDescription profile = ProfileDescription.fromApiCreateProfileRequest(body);
    ProfileDescription result =
        profileService.createProfile(profile, user, body.getInitiatingUser());
    return new ResponseEntity<>(result.toApiProfileModel(), HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<ProfileModel> getProfile(UUID profileId) {
    AuthenticatedUserRequest user = authenticatedUserRequestFactory.from(request);
    ProfileDescription profile = profileService.getProfile(profileId, user);
    return new ResponseEntity<>(profile.toApiProfileModel(), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ProfileModelList> listProfiles(Integer offset, Integer limit) {
    validatePaginationParams(offset, limit);
    logger.info("Get token to register: {}",request.getHeader("Authorization"));
    AuthenticatedUserRequest user = authenticatedUserRequestFactory.from(request);
    List<ProfileDescription> profiles = profileService.listProfiles(user, offset, limit);
    var response =
        new ProfileModelList()
            .items(profiles.stream().map(ProfileDescription::toApiProfileModel).toList());
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ProfileModel> updateProfile(UUID id, UpdateProfileRequest body) {
    AuthenticatedUserRequest user = authenticatedUserRequestFactory.from(request);
    ProfileDescription updatedProfile = profileService.updateProfile(id, body, user);
    return new ResponseEntity<>(updatedProfile.toApiProfileModel(), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> deleteProfile(UUID id, String initiatingUser) {
    AuthenticatedUserRequest user = authenticatedUserRequestFactory.from(request);
    profileService.deleteProfile(id, user, initiatingUser);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> removeBillingAccount(UUID id, String initiatingUser) {
    AuthenticatedUserRequest user = authenticatedUserRequestFactory.from(request);
    profileService.removeBillingAccount(id, user, initiatingUser);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> leaveProfile(UUID id) {
    AuthenticatedUserRequest user = authenticatedUserRequestFactory.from(request);
    profileService.leaveProfile(id, user);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<SamPolicyModelList> getProfilePolicies(UUID id) {
    AuthenticatedUserRequest user = authenticatedUserRequestFactory.from(request);
    List<SamPolicyModel> policies = profileService.getProfilePolicies(id, user);
    var response = new SamPolicyModelList().items(policies);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<SamPolicyModel> addProfilePolicyMember(
      UUID id, String policyName, PolicyMemberRequest requestBody) {
    AuthenticatedUserRequest user = authenticatedUserRequestFactory.from(request);
    SamPolicyModel policy =
        profileService.addProfilePolicyMember(id, policyName, requestBody.getEmail(), user);
    return new ResponseEntity<>(policy, HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<SamPolicyModel> deleteProfilePolicyMember(
      UUID id, String policyName, String memberEmail) {
    AuthenticatedUserRequest user = authenticatedUserRequestFactory.from(request);
    SamPolicyModel policy =
        profileService.deleteProfilePolicyMember(id, policyName, memberEmail, user);
    return new ResponseEntity<>(policy, HttpStatus.OK);
  }

  /**
   * Utility to validate limit/offset parameters used in pagination.
   *
   * <p>This throws ValidationExceptions if invalid offset or limit values are provided. This only
   * asserts that offset is at least 0 and limit is at least 1. More specific validation can be
   * added for individual endpoints.
   */
  private static void validatePaginationParams(int offset, int limit) {
    List<String> errors = new ArrayList<>();
    if (offset < 0) {
      errors.add("offset must be greater than or equal to 0.");
    }
    if (limit < 1) {
      errors.add("limit must be greater than or equal to 1.");
    }
    if (!errors.isEmpty()) {
      throw new ValidationException("Invalid pagination parameters.", errors);
    }
  }
}
