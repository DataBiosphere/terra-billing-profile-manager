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

  public Stream<Application> getApplicationsForSubscription(UUID subscriptionId) {
    var appMgr = crlService.getApplicationManager(subscriptionId);
    return appMgr.applications().list().stream();
  }

  public UUID getTenantForSubscription(UUID subscriptionId) {
    return crlService.getTenantForSubscription(subscriptionId);
  }
}
