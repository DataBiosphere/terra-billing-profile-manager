package bio.terra.profile.service.profile.model;

import bio.terra.profile.generated.model.ApiCloudPlatform;
import bio.terra.profile.generated.model.ApiCreateProfileRequest;
import bio.terra.profile.generated.model.ApiProfileModel;
import bio.terra.profile.service.profile.exception.MissingRequiredFieldsException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Internal representation of a billing profile. */
public record BillingProfile(
    UUID id,
    String displayName,
    String description,
    String biller,
    CloudPlatform cloudPlatform,
    Optional<String> billingAccountId,
    Optional<UUID> tenantId,
    Optional<UUID> subscriptionId,
    Optional<String> resourceGroupName,
    Optional<String> applicationDeploymentName,
    Instant createdTime,
    String createdBy) {

  /**
   * Converts an {@link ApiCreateProfileRequest} to a BillingProfile and performs validation.
   *
   * @param request the API request to covert
   * @return billing profile
   */
  public static BillingProfile fromApiCreateProfileRequest(ApiCreateProfileRequest request) {
    // id, display name, biller, cloud platform are required
    if (request.getId() == null
        || request.getDisplayName() == null
        || request.getBiller() == null
        || request.getCloudPlatform() == null) {
      throw new MissingRequiredFieldsException(
          "Billing profile requires id, displayName, biller, and cloudPlatform");
    }

    // check cloud-specific fields
    if (request.getCloudPlatform().equals(ApiCloudPlatform.GCP)
        && request.getBillingAccountId() == null) {
      throw new MissingRequiredFieldsException("GCP billing profile requires billingAccount");
    }
    if (request.getCloudPlatform().equals(ApiCloudPlatform.AZURE)
        && (request.getTenantId() == null
            || request.getSubscriptionId() == null
            || request.getResourceGroupName() == null
            || request.getApplicationDeploymentName() == null)) {
      throw new MissingRequiredFieldsException(
          "Azure billing profile requires tenantId, subscriptionId, resourceGroupName, and applicationDeploymentName");
    }

    return new BillingProfile(
        request.getId(),
        request.getDisplayName(),
        request.getDisplayName(),
        request.getBiller(),
        CloudPlatform.fromApi(request.getCloudPlatform()),
        Optional.ofNullable(request.getBillingAccountId()),
        Optional.ofNullable(request.getTenantId()),
        Optional.ofNullable(request.getSubscriptionId()),
        Optional.ofNullable(request.getResourceGroupName()),
        Optional.ofNullable(request.getApplicationDeploymentName()),
        null,
        null);
  }

  /**
   * Converts this billing profile to an {@link ApiProfileModel}.
   *
   * @return ApiProfileModel
   */
  public ApiProfileModel toApiProfileModel() {
    return new ApiProfileModel()
        .id(id)
        .displayName(displayName)
        .description(description)
        .biller(biller)
        .cloudPlatform(cloudPlatform.toApi())
        .billingAccountId(billingAccountId.orElse(null))
        .tenantId(tenantId.orElse(null))
        .subscriptionId(subscriptionId.orElse(null))
        .resourceGroupName(resourceGroupName.orElse(null))
        .applicationDeploymentName(applicationDeploymentName.orElse(null))
        .createdDate(createdTime.toString())
        .createdBy(createdBy);
  }
}
