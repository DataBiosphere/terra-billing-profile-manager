package bio.terra.profile.app.controller;

import bio.terra.common.exception.ValidationException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.iam.AuthenticatedUserRequestFactory;
import bio.terra.profile.generated.controller.ProfileApi;
import bio.terra.profile.generated.model.ApiCreateProfileRequest;
import bio.terra.profile.generated.model.ApiCreateProfileResult;
import bio.terra.profile.generated.model.ApiJobReport;
import bio.terra.profile.generated.model.ApiProfileModel;
import bio.terra.profile.generated.model.ApiProfileModelList;
import bio.terra.profile.service.job.JobService;
import bio.terra.profile.service.profile.ProfileService;
import bio.terra.profile.service.profile.model.BillingProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class ProfileApiController implements ProfileApi {
  private final HttpServletRequest request;
  private final ProfileService profileService;
  private final AuthenticatedUserRequestFactory authenticatedUserRequestFactory;
  private final JobService jobService;

  @Autowired
  public ProfileApiController(
      HttpServletRequest request,
      ProfileService profileService,
      JobService jobService,
      AuthenticatedUserRequestFactory authenticatedUserRequestFactory) {
    this.request = request;
    this.profileService = profileService;
    this.jobService = jobService;
    this.authenticatedUserRequestFactory = authenticatedUserRequestFactory;
  }

  @Override
  public ResponseEntity<ApiCreateProfileResult> createProfile(
      @RequestBody ApiCreateProfileRequest body) {
    AuthenticatedUserRequest user = authenticatedUserRequestFactory.from(request);
    BillingProfile profile = BillingProfile.fromApiCreateProfileRequest(body);
    String jobId = profileService.createProfile(profile, user);
    final ApiCreateProfileResult result = fetchCreateProfileResult(jobId, user);
    return new ResponseEntity<>(result, getAsyncResponseCode(result.getJobReport()));
  }

  @Override
  public ResponseEntity<ApiCreateProfileResult> getCreateProfileResult(
      @PathVariable("jobId") String jobId) {
    AuthenticatedUserRequest user = authenticatedUserRequestFactory.from(request);
    final ApiCreateProfileResult response = fetchCreateProfileResult(jobId, user);
    return new ResponseEntity<>(response, getAsyncResponseCode(response.getJobReport()));
  }

  @Override
  public ResponseEntity<ApiProfileModel> getProfile(@PathVariable("profileId") UUID profileId) {
    AuthenticatedUserRequest user = authenticatedUserRequestFactory.from(request);
    BillingProfile profile = profileService.getProfile(profileId, user);
    return new ResponseEntity<>(profile.toApiProfileModel(), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ApiProfileModelList> listProfiles(Integer offset, Integer limit) {
    validatePaginationParams(offset, limit);
    AuthenticatedUserRequest user = authenticatedUserRequestFactory.from(request);
    List<BillingProfile> profiles = profileService.listProfiles(user, offset, limit);
    var response =
        new ApiProfileModelList()
            .items(
                profiles.stream()
                    .map(BillingProfile::toApiProfileModel)
                    .collect(Collectors.toList()));
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> deleteProfile(@PathVariable("profileId") UUID id) {
    AuthenticatedUserRequest user = authenticatedUserRequestFactory.from(request);
    profileService.deleteProfile(id, user);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  private ApiCreateProfileResult fetchCreateProfileResult(
      String jobId, AuthenticatedUserRequest userRequest) {
    final JobService.AsyncJobResult<BillingProfile> jobResult =
        jobService.retrieveAsyncJobResult(jobId, BillingProfile.class, userRequest);
    return new ApiCreateProfileResult()
        .jobReport(jobResult.getJobReport())
        .errorReport(jobResult.getApiErrorReport())
        .profileDescription(
            Optional.ofNullable(jobResult.getResult())
                .map(BillingProfile::toApiProfileModel)
                .orElse(null));
  }

  private static HttpStatus getAsyncResponseCode(ApiJobReport jobReport) {
    return jobReport.getStatus() == ApiJobReport.StatusEnum.RUNNING
        ? HttpStatus.ACCEPTED
        : HttpStatus.OK;
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
