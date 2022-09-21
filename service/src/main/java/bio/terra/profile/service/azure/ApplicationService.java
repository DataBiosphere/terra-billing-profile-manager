package bio.terra.profile.service.azure;

import bio.terra.profile.service.crl.CrlService;
import bio.terra.profile.service.profile.exception.DuplicateTagException;
import com.azure.resourcemanager.managedapplications.models.Application;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ApplicationService {
  private final CrlService crlService;

  @Autowired
  public ApplicationService(CrlService crlService) {
    this.crlService = crlService;
  }

  /**
   * Retrieves the managed applications for a given Azure subscription
   *
   * @param subscriptionId Azure subscription ID to be queried
   * @return List of managed applications present in the subscription
   */
  public Stream<Application> getApplicationsForSubscription(UUID subscriptionId) {
    var appMgr = crlService.getApplicationManager(subscriptionId);
    return appMgr.applications().list().stream();
  }

  /**
   * Retrieves the tenant associated with a given Azure subscription ID
   *
   * @param subscriptionId Azure subscription ID to be queried
   * @return UUID for the associated tenant
   */
  public UUID getTenantForSubscription(UUID subscriptionId) {
    return crlService.getTenantForSubscription(subscriptionId);
  }

  /**
   * Adds a tag to the MRG associated with a given profile. If the tag is already present, * throws
   * a DuplicateTagException.
   *
   * @param tenantId Tenant ID associated with the MRG
   * @param subscriptionId Subscription ID associated with the MRG
   * @param managedResourceGroupId ID for the MRG
   * @param tag Tag name
   * @param value Tag value
   */
  public void addTagToMrg(
      UUID tenantId, UUID subscriptionId, String managedResourceGroupId, String tag, String value) {

    var mrgResource = crlService.getResourceGroup(tenantId, subscriptionId, managedResourceGroupId);
    var existingTags = mrgResource.tags();
    var existing = existingTags.get(tag);
    if (existing != null) {
      throw new DuplicateTagException(
          String.format(
              "MRG has existing tag [mrg_id = %s, existing tag = %s, value = %s]",
              managedResourceGroupId, tag, existing));
    }

    // dupe existing tags to a mutable hashmap
    HashMap<String, String> tags = new HashMap<>(mrgResource.tags());
    tags.put(tag, value);
    crlService.updateTagsForResource(tenantId, subscriptionId, managedResourceGroupId, tags);
  }
}
