package bio.terra.profile.service.iam;

import bio.terra.common.exception.ForbiddenException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.sam.SamRetry;
import bio.terra.common.sam.exception.SamExceptionFactory;
import bio.terra.profile.app.configuration.SamConfiguration;
import bio.terra.profile.service.iam.model.SamAction;
import bio.terra.profile.service.iam.model.SamResourceType;
import bio.terra.profile.service.iam.model.SamRole;
import com.google.common.annotations.VisibleForTesting;
import io.opencensus.contrib.spring.aop.Traced;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import okhttp3.OkHttpClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.AccessPolicyMembershipV2;
import org.broadinstitute.dsde.workbench.client.sam.model.CreateResourceRequestV2;
import org.broadinstitute.dsde.workbench.client.sam.model.ResourceAndAccessPolicy;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SamService {
  private static final Logger logger = LoggerFactory.getLogger(SamService.class);
  private final SamConfiguration samConfig;
  private final OkHttpClient commonHttpClient;

  @Autowired
  public SamService(SamConfiguration samConfig) {
    this.samConfig = samConfig;
    this.commonHttpClient = new ApiClient().getHttpClient();
  }

  /**
   * Wrapper around isAuthorized which throws an appropriate exception if a user does not have
   * access to a resource.
   */
  @Traced
  public void verifyAuthorization(
      AuthenticatedUserRequest userRequest,
      SamResourceType resourceType,
      UUID resourceId,
      SamAction action)
      throws InterruptedException {
    final boolean isAuthorized = isAuthorized(userRequest, resourceType, resourceId, action);
    final String userEmail = userRequest.getEmail();
    if (!isAuthorized)
      throw new ForbiddenException(
          String.format(
              "User %s is not authorized to %s resource %s of type %s",
              userEmail, action, resourceId, resourceType));
    else {
      logger.info(
          "User {} is authorized to {} resource {} of type {}",
          userEmail,
          action,
          resourceId,
          resourceType);
    }
  }

  /**
   * Checks if a user authorized to do an action on a resource.
   *
   * @param userRequest authenticated user
   * @param resourceType resource type
   * @param resourceId resource in question
   * @param action action to check
   * @return true if authorized; false otherwise
   * @throws InterruptedException
   */
  public boolean isAuthorized(
      AuthenticatedUserRequest userRequest,
      SamResourceType resourceType,
      UUID resourceId,
      SamAction action)
      throws InterruptedException {
    String accessToken = userRequest.getToken();
    ResourcesApi resourceApi = samResourcesApi(accessToken);
    try {
      return SamRetry.retry(
          () ->
              resourceApi.resourcePermissionV2(
                  resourceType.getSamResourceName(),
                  resourceId.toString(),
                  action.getSamActionName()));
    } catch (ApiException e) {
      throw SamExceptionFactory.create("Error checking resource permission in Sam", e);
    }
  }

  /**
   * Checks if a user has any action on a resource.
   *
   * <p>If user has any action on a resource than we allow that user to list the resource, rather
   * than have a specific action for listing. That is the Sam convention.
   *
   * @param userRequest authenticated user
   * @param resourceType resource type
   * @param resourceId resource in question
   * @return true if the user has any actions on that resource; false otherwise.
   */
  public boolean hasActions(
      AuthenticatedUserRequest userRequest, SamResourceType resourceType, UUID resourceId)
      throws InterruptedException {
    String accessToken = userRequest.getToken();
    ResourcesApi resourceApi = samResourcesApi(accessToken);
    try {
      return SamRetry.retry(
          () ->
              resourceApi
                      .resourceActions(resourceType.getSamResourceName(), resourceId.toString())
                      .size()
                  > 0);
    } catch (ApiException e) {
      throw SamExceptionFactory.create("Error checking resource permission in Sam", e);
    }
  }

  /**
   * List all profile IDs in Sam this user has access to.
   *
   * @param userRequest authenticated user
   * @return list of profiles
   * @throws InterruptedException
   */
  @Traced
  public List<UUID> listProfileIds(AuthenticatedUserRequest userRequest)
      throws InterruptedException {
    ResourcesApi resourceApi = samResourcesApi(userRequest.getToken());
    try {
      List<ResourceAndAccessPolicy> resourceAndPolicies =
          SamRetry.retry(
              () ->
                  resourceApi.listResourcesAndPolicies(
                      SamResourceType.PROFILE.getSamResourceName()));
      return resourceAndPolicies.stream()
          .flatMap(
              p -> {
                // BPM always uses UUIDs for profile IDs, but this is not enforced in Sam.
                // Ignore any profiles with a non-UUID id.
                try {
                  return Stream.of(UUID.fromString(p.getResourceId()));
                } catch (IllegalArgumentException e) {
                  return Stream.empty();
                }
              })
          .collect(Collectors.toList());
    } catch (ApiException e) {
      throw SamExceptionFactory.create("Error listing profile ids in Sam", e);
    }
  }

  /**
   * Fetch the user status (email and subjectId) from Sam.
   *
   * @param userToken user token
   * @return {@link UserStatusInfo}
   */
  public UserStatusInfo getUserStatusInfo(String userToken) throws InterruptedException {
    UsersApi usersApi = samUsersApi(userToken);
    try {
      return SamRetry.retry(() -> usersApi.getUserStatusInfo());
    } catch (ApiException e) {
      throw SamExceptionFactory.create("Error getting user email from Sam", e);
    }
  }

  /**
   * Creates a profile resource in Sam.
   *
   * @param userRequest authenticated user
   * @param profileId id of the profile to create
   * @throws InterruptedException
   */
  public void createProfileResource(AuthenticatedUserRequest userRequest, UUID profileId)
      throws InterruptedException {
    ResourcesApi resourcesApi = samResourcesApi(userRequest.getToken());

    Map<String, AccessPolicyMembershipV2> policyMap = new HashMap<>();

    // BPM-configured group has Admin role
    // TODO: configure admin group
    //    policyMap.put(
    //        SamRole.ADMIN.getSamRoleName(),
    //        new AccessPolicyMembershipV2()
    //            .addRolesItem(SamRole.ADMIN.getSamRoleName())
    //            .addMemberEmailsItem(samConfig.adminsGroupEmail()));

    // Calling user has Owner role
    policyMap.put(
        SamRole.OWNER.getSamRoleName(),
        new AccessPolicyMembershipV2()
            .addRolesItem(SamRole.OWNER.getSamRoleName())
            .addMemberEmailsItem(userRequest.getEmail()));

    // Create empty User policy which can be modified later
    policyMap.put(
        SamRole.USER.getSamRoleName(),
        new AccessPolicyMembershipV2().addRolesItem(SamRole.USER.getSamRoleName()));

    CreateResourceRequestV2 profileRequest =
        new CreateResourceRequestV2().resourceId(profileId.toString()).policies(policyMap);

    try {
      SamRetry.retry(
          () ->
              resourcesApi.createResourceV2(
                  SamResourceType.PROFILE.getSamResourceName(), profileRequest));
      logger.info("Created Sam resource for profile {}", profileId);
    } catch (ApiException e) {
      throw SamExceptionFactory.create("Error creating a profile resource in Sam", e);
    }
  }

  /**
   * Deletes a profile resource from Sam.
   *
   * @param userRequest authenticated user
   * @param profileId profile ID to delete
   * @throws InterruptedException
   */
  public void deleteProfileResource(AuthenticatedUserRequest userRequest, UUID profileId)
      throws InterruptedException {
    ResourcesApi resourcesApi = samResourcesApi(userRequest.getToken());
    try {
      SamRetry.retry(
          () ->
              resourcesApi.deleteResource(
                  SamResourceType.PROFILE.getSamResourceName(), profileId.toString()));
      logger.info("Deleted Sam resource for profile {}", profileId);
    } catch (ApiException e) {
      logger.info("Sam API error while deleting profile, code is " + e.getCode());
      // Recover if the resource to delete is not found
      if (e.getCode() == HttpStatus.NOT_FOUND.value()) {
        return;
      }
      throw SamExceptionFactory.create("Error deleting a profile in Sam", e);
    }
  }

  @VisibleForTesting
  UsersApi samUsersApi(String accessToken) {
    return new UsersApi(getApiClient(accessToken));
  }

  @VisibleForTesting
  ResourcesApi samResourcesApi(String accessToken) {
    return new ResourcesApi(getApiClient(accessToken));
  }

  private ApiClient getApiClient(String accessToken) {
    // OkHttpClient objects manage their own thread pools, so it's much more performant to share one
    // across requests.
    ApiClient apiClient =
        new ApiClient().setHttpClient(commonHttpClient).setBasePath(samConfig.basePath());
    apiClient.setAccessToken(accessToken);
    return apiClient;
  }
}
