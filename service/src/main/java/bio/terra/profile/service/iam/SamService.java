package bio.terra.profile.service.iam;

import bio.terra.common.exception.ForbiddenException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.sam.SamRetry;
import bio.terra.common.sam.exception.SamExceptionFactory;
import bio.terra.common.tracing.OkHttpClientTracingInterceptor;
import bio.terra.profile.app.configuration.SamConfiguration;
import bio.terra.profile.model.SamPolicyModel;
import bio.terra.profile.model.SystemStatusSystems;
import bio.terra.profile.service.iam.model.SamAction;
import bio.terra.profile.service.iam.model.SamResourceType;
import bio.terra.profile.service.iam.model.SamRole;
import bio.terra.profile.service.profile.model.BillingProfile;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import okhttp3.OkHttpClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.AzureApi;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.*;
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
  public SamService(SamConfiguration samConfig, OpenTelemetry openTelemetry) {
    this.samConfig = samConfig;
    this.commonHttpClient =
        new ApiClient()
            .getHttpClient()
            .newBuilder()
            .addInterceptor(new OkHttpClientTracingInterceptor(openTelemetry))
            .build();
  }

  /**
   * Wrapper around isAuthorized which throws an appropriate exception if a user does not have
   * access to a resource.
   */
  @WithSpan
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
   * List all profile IDs in Sam this user has access to.
   *
   * @param userRequest authenticated user
   * @return list of profiles
   * @throws InterruptedException
   */
  @WithSpan
  public List<UUID> listProfileIds(AuthenticatedUserRequest userRequest)
      throws InterruptedException {
    ResourcesApi resourceApi = samResourcesApi(userRequest.getToken());
    try {
      List<UserResourcesResponse> resourceAndPolicies =
          SamRetry.retry(
              () ->
                  resourceApi.listResourcesAndPoliciesV2(
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
          .toList();
    } catch (ApiException e) {
      throw SamExceptionFactory.create("Error listing profile ids in Sam", e);
    }
  }

  /**
   * Retrieves the Sam policies for the specified profile.
   *
   * @param userRequest authenticated user
   * @param resourceId resourceId profile in question
   * @return list of policies
   * @throws InterruptedException
   */
  public List<SamPolicyModel> retrieveProfilePolicies(
      AuthenticatedUserRequest userRequest, UUID resourceId) throws InterruptedException {
    ResourcesApi resourceApi = samResourcesApi(userRequest.getToken());
    try {
      List<AccessPolicyResponseEntryV2> policies =
          SamRetry.retry(
              () ->
                  resourceApi.listResourcePoliciesV2(
                      SamResourceType.PROFILE.getSamResourceName(), resourceId.toString()));
      return policies.stream()
          .map(
              entry ->
                  new SamPolicyModel()
                      .name(entry.getPolicyName())
                      .members(entry.getPolicy().getMemberEmails()))
          .toList();
    } catch (ApiException e) {
      throw SamExceptionFactory.create("Error retrieving profile policies from Sam", e);
    }
  }

  /**
   * Adds a user via their email to the specified profile policy.
   *
   * @param userRequest authenticated user
   * @param resourceId resourceId profile in question
   * @param policyName the name of the Sam policy
   * @param userEmail the email of the user to add
   * @return the policy details after the update
   * @throws InterruptedException
   */
  public SamPolicyModel addProfilePolicyMember(
      AuthenticatedUserRequest userRequest, UUID resourceId, String policyName, String userEmail)
      throws InterruptedException {
    try {
      return SamRetry.retry(
          () -> addProfilePolicyMemberInner(userRequest, resourceId, policyName, userEmail));
    } catch (ApiException e) {
      throw SamExceptionFactory.create("Error adding profile policy member in Sam", e);
    }
  }

  private SamPolicyModel addProfilePolicyMemberInner(
      AuthenticatedUserRequest userRequest, UUID resourceId, String policyName, String userEmail)
      throws ApiException {
    ResourcesApi samResourceApi = samResourcesApi(userRequest.getToken());
    String samResourceName = SamResourceType.PROFILE.getSamResourceName();
    samResourceApi.addUserToPolicyV2(
        samResourceName, resourceId.toString(), policyName, userEmail, null);
    AccessPolicyMembershipV2 result =
        samResourceApi.getPolicyV2(samResourceName, resourceId.toString(), policyName);
    return new SamPolicyModel().name(policyName).members(result.getMemberEmails());
  }

  /**
   * Removes a user via their email from the specified profile policy.
   *
   * @param userRequest authenticated user
   * @param resourceId resourceId profile in question
   * @param policyName the name of the Sam policy
   * @param userEmail the email of the user to remove
   * @return the policy details after the update
   * @throws InterruptedException
   */
  public SamPolicyModel deleteProfilePolicyMember(
      AuthenticatedUserRequest userRequest, UUID resourceId, String policyName, String userEmail)
      throws InterruptedException {
    try {
      return SamRetry.retry(
          () -> deleteProfilePolicyMemberInner(userRequest, resourceId, policyName, userEmail));
    } catch (ApiException e) {
      throw SamExceptionFactory.create("Error deleting profile policy member in Sam", e);
    }
  }

  private SamPolicyModel deleteProfilePolicyMemberInner(
      AuthenticatedUserRequest userRequest, UUID resourceId, String policyName, String userEmail)
      throws ApiException {
    ResourcesApi samResourceApi = samResourcesApi(userRequest.getToken());
    String samResourceName = SamResourceType.PROFILE.getSamResourceName();
    samResourceApi.removeUserFromPolicyV2(
        samResourceName, resourceId.toString(), policyName, userEmail);
    AccessPolicyMembershipV2 result =
        samResourceApi.getPolicyV2(samResourceName, resourceId.toString(), policyName);
    return new SamPolicyModel().name(policyName).members(result.getMemberEmails());
  }

  /**
   * Removes the authenticated user from the specified resource.
   *
   * @param userRequest authenticated user
   * @param samResourceTypeName the type of resource being left
   * @param resourceId resourceId of the profile in question
   * @throws InterruptedException
   */
  public void leaveResource(
      AuthenticatedUserRequest userRequest, String samResourceTypeName, UUID resourceId)
      throws InterruptedException {
    try {
      SamRetry.retry(() -> leaveResourceInner(userRequest, samResourceTypeName, resourceId));
    } catch (ApiException e) {
      throw SamExceptionFactory.create("Error leaving resource in Sam", e);
    }
  }

  private void leaveResourceInner(
      AuthenticatedUserRequest userRequest, String samResourceTypeName, UUID resourceId)
      throws ApiException {
    ResourcesApi samResourceApi = samResourcesApi(userRequest.getToken());
    samResourceApi.leaveResourceV2(samResourceTypeName, resourceId.toString());
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

    Map<String, AccessPolicyMembershipRequest> policyMap = new HashMap<>();

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
        new AccessPolicyMembershipRequest()
            .addRolesItem(SamRole.OWNER.getSamRoleName())
            .addMemberEmailsItem(userRequest.getEmail()));

    // Create empty User policy which can be modified later
    policyMap.put(
        SamRole.USER.getSamRoleName(),
        new AccessPolicyMembershipRequest().addRolesItem(SamRole.USER.getSamRoleName()));

    policyMap.put(
        SamRole.PET_CREATOR.getSamRoleName(),
        new AccessPolicyMembershipRequest().addRolesItem(SamRole.PET_CREATOR.getSamRoleName()));

    CreateResourceRequestV2 profileRequest =
        new CreateResourceRequestV2()
            .resourceId(profileId.toString())
            .policies(policyMap)
            .authDomain(Collections.emptyList());

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
              resourcesApi.deleteResourceV2(
                  SamResourceType.PROFILE.getSamResourceName(), profileId.toString()));
      logger.info("Deleted Sam resource for profile {}", profileId);
    } catch (ApiException e) {
      logger.info("Sam API error while deleting profile, code is {}", e.getCode());
      // Recover if the resource to delete is not found
      if (e.getCode() == HttpStatus.NOT_FOUND.value()) {
        return;
      }
      throw SamExceptionFactory.create("Error deleting a profile in Sam", e);
    }
  }

  /**
   * Deletes the managed resource group associated with the given billing profile in Sam.
   *
   * @param profileId profile ID whose MRG entry in Sam should be deleted
   * @param userRequest authenticated user
   * @throws InterruptedException
   */
  public void deleteManagedResourceGroup(UUID profileId, AuthenticatedUserRequest userRequest)
      throws InterruptedException {
    AzureApi azureApi = samAzureApi(userRequest.getToken());
    try {
      SamRetry.retry(() -> azureApi.deleteManagedResourceGroup(profileId.toString()));
      logger.info("Deleted mrg in Sam for profile {}", profileId);
    } catch (ApiException e) {
      if (e.getCode() == HttpStatus.NOT_FOUND.value()) {
        // MRG may already be deleted or not present
        return;
      }
      throw SamExceptionFactory.create(
          "Error deleting Sam managed resource group record for billing profile", e);
    }
  }

  /**
   * Creates a record in Sam that links a billing profile to an Azure MRG
   *
   * @param profile Billing profile that will be linked
   * @param userRequest authenticated user
   * @throws InterruptedException
   */
  public void createManagedResourceGroup(
      BillingProfile profile, AuthenticatedUserRequest userRequest) throws InterruptedException {
    AzureApi azureApi = samAzureApi(userRequest.getToken());
    try {
      SamRetry.retry(
          () ->
              azureApi.createManagedResourceGroup(
                  profile.id().toString(),
                  new ManagedResourceGroupCoordinates()
                      .tenantId(profile.getRequiredTenantId().toString())
                      .subscriptionId(profile.getRequiredSubscriptionId().toString())
                      .managedResourceGroupName(profile.getRequiredManagedResourceGroupId())));

      logger.info(
          "Created mrg in Sam for profile {}, mrg id = {}, tenant = {}, subscription = {}",
          profile.id(),
          profile.managedResourceGroupId(),
          profile.tenantId(),
          profile.subscriptionId());
    } catch (ApiException e) {
      throw SamExceptionFactory.create(
          "Error creating managed resource group record for billing profile", e);
    }
  }

  public SystemStatusSystems status() {
    // No access token needed since this is an unauthenticated API.
    StatusApi statusApi = new StatusApi(getApiClient(null));
    try {
      // Don't retry status check
      SystemStatus samStatus = statusApi.getSystemStatus();
      var result = new SystemStatusSystems().ok(samStatus.getOk());
      var samSystems = samStatus.getSystems();
      // Populate error message if Sam status is non-ok
      if (!samStatus.getOk()) {
        String errorMsg = "Sam status check failed. Messages = " + samSystems;
        logger.error(errorMsg);
        result.addMessagesItem(errorMsg);
      }
      return result;
    } catch (Exception e) {
      String errorMsg = "Sam status check failed";
      logger.error(errorMsg, e);
      return new SystemStatusSystems().ok(false).messages(List.of(errorMsg));
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

  AzureApi samAzureApi(String accessToken) {
    return new AzureApi(getApiClient(accessToken));
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
