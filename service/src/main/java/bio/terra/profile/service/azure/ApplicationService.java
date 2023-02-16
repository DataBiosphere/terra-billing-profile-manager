package bio.terra.profile.service.azure;

import bio.terra.profile.service.azure.exception.InaccessibleSubscriptionException;
import bio.terra.profile.service.crl.CrlService;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.managedapplications.models.Application;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ApplicationService {
  public static final String AZURE_SUB_NOT_FOUND = "SubscriptionNotFound";
  public static final String AZURE_AUTH_FAILED = "AuthorizationFailed";
  public static final String INVALID_AUTH_TOKEN = "InvalidAuthenticationTokenTenant";

  private static final Set<String> INACCCESSIBLE_SUB_CODES =
      Set.of(AZURE_SUB_NOT_FOUND, AZURE_AUTH_FAILED, INVALID_AUTH_TOKEN);

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
   * @throws InaccessibleSubscriptionException when the azure subscription is not accessible by BPM
   */
  public UUID getTenantForSubscription(UUID subscriptionId) {
    try {
      return crlService.getTenantForSubscription(subscriptionId);
    } catch (ManagementException e) {
      if (INACCCESSIBLE_SUB_CODES.contains(e.getValue().getCode())) {
        throw new InaccessibleSubscriptionException("Subscription not accessible", e);
      }
      throw e;
    }
  }
}
