package bio.terra.profile.service.profile.model;

import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.model.CreateProfileRequest;
import bio.terra.profile.model.ProfileModel;
import bio.terra.profile.service.profile.exception.InvalidFieldException;
import bio.terra.profile.service.profile.exception.MissingRequiredFieldsException;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    Optional<String> managedResourceGroupId,
    Instant createdTime,
    Instant lastModified,
    String createdBy) {

  private static final int MAX_BILLING_ACCT_LENGTH = 32;

  /**
   * Converts an {@link CreateProfileRequest} to a BillingProfile and performs validation.
   *
   * @param request the API request to covert
   * @return billing profile
   */
  public static BillingProfile fromApiCreateProfileRequest(CreateProfileRequest request) {
    // id, display name, biller, cloud platform are required
    if (request.getId() == null
        || request.getDisplayName() == null
        || request.getBiller() == null
        || request.getCloudPlatform() == null) {
      throw new MissingRequiredFieldsException(
          "Billing profile requires id, displayName, biller, and cloudPlatform");
    }

    // check cloud-specific fields
    if (request.getCloudPlatform().equals(CloudPlatform.GCP)) {
      if (request.getBillingAccountId() == null) {
        throw new MissingRequiredFieldsException("GCP billing profile requires billingAccount");
      }
      if (request.getTenantId() != null
          || request.getSubscriptionId() != null
          || request.getManagedResourceGroupId() != null) {
        throw new MissingRequiredFieldsException("GCP billing profile must not contain Azure data");
      }
      if (request.getBillingAccountId().length() > MAX_BILLING_ACCT_LENGTH) {
        throw new InvalidFieldException(
            "GCP billing account ID must be less than " + MAX_BILLING_ACCT_LENGTH + " characters");
      }
    } else if (request.getCloudPlatform().equals(CloudPlatform.AZURE)) {
      if (request.getTenantId() == null
          || request.getSubscriptionId() == null
          || request.getManagedResourceGroupId() == null) {
        throw new MissingRequiredFieldsException(
            "Azure billing profile requires tenantId, subscriptionId, managedResourceGroupId");
      }
      if (request.getBillingAccountId() != null) {
        throw new MissingRequiredFieldsException("Azure billing profile must not contain GCP data");
      }
    }

    return new BillingProfile(
        request.getId(),
        request.getDisplayName(),
        request.getDisplayName(),
        request.getBiller(),
        request.getCloudPlatform(),
        Optional.ofNullable(request.getBillingAccountId()),
        Optional.ofNullable(request.getTenantId()),
        Optional.ofNullable(request.getSubscriptionId()),
        Optional.ofNullable(request.getManagedResourceGroupId()),
        null,
        null,
        null);
  }

  /**
   * Converts this billing profile to an {@link ProfileModel}.
   *
   * @return ApiProfileModel
   */
  public ProfileModel toApiProfileModel() {
    return new ProfileModel()
        .id(id)
        .displayName(displayName)
        .description(description)
        .biller(biller)
        .cloudPlatform(cloudPlatform)
        .billingAccountId(billingAccountId.orElse(null))
        .tenantId(tenantId.orElse(null))
        .subscriptionId(subscriptionId.orElse(null))
        .managedResourceGroupId(managedResourceGroupId.orElse(null))
        .createdDate(createdTime.toString())
        .lastModified(lastModified.toString())
        .createdBy(createdBy);
  }

  @JsonIgnore
  public String getRequiredBillingAccountId() {
    return billingAccountId.orElseThrow(
        () -> new MissingRequiredFieldsException("Missing billing account ID"));
  }

  @JsonIgnore
  public UUID getRequiredTenantId() {
    return tenantId.orElseThrow(
        () -> new MissingRequiredFieldsException("Missing azure tenant ID"));
  }

  @JsonIgnore
  public UUID getRequiredSubscriptionId() {
    return subscriptionId.orElseThrow(
        () -> new MissingRequiredFieldsException("Missing azure subscription ID"));
  }

  @JsonIgnore
  public String getRequiredManagedResourceGroupId() {
    return managedResourceGroupId.orElseThrow(
        () -> new MissingRequiredFieldsException("Missing azure managed resource group ID"));
  }
}
