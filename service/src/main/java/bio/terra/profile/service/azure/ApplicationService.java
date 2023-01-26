package bio.terra.profile.service.azure;

import bio.terra.profile.service.crl.CrlService;
import com.azure.resourcemanager.managedapplications.models.Application;
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
}
