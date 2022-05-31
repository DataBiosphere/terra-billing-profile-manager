package bio.terra.profile.service.azure;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.model.AzureManagedAppModel;
import bio.terra.profile.service.crl.CrlService;
import com.azure.resourcemanager.managedapplications.models.Application;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AzureService {
  private static final Logger logger = LoggerFactory.getLogger(AzureService.class);
  private static final String AUTHORIZED_USER_KEY = "authorizedTerraUser";
  // TODO externalize this
  private static final String TERRA_APP_PRDDUCT = "aherbst-202202024-preview";

  private final CrlService crlService;

  @Autowired
  public AzureService(CrlService crlService) {
    this.crlService = crlService;
  }

  /**
   * Gets the Azure managed applications the user has access to in the given subscription.
   *
   * @param subscriptionId Azure subscription ID that will be checked for managed applications
   * @param userRequest Authorized user request
   * @return List of Terra Azure managed applications the user has access to
   */
  public List<AzureManagedAppModel> getManagedAppDeployments(
      UUID subscriptionId, AuthenticatedUserRequest userRequest) {
    var appMgr = this.crlService.getApplicationManager(subscriptionId);
    return appMgr.applications().list().stream()
        .filter(app -> isAuthedTerraManagedApp(userRequest, app))
        .map(app -> new AzureManagedAppModel().name(app.name()))
        .toList();
  }

  private boolean isAuthedTerraManagedApp(AuthenticatedUserRequest userRequest, Application app) {
    if (app.plan() == null) {
      return false;
    }
    if (!app.plan().product().equals(TERRA_APP_PRDDUCT)) {
      return false;
    }

    if (app.parameters() != null && app.parameters() instanceof HashMap rawParams) {
      var paramValues = (HashMap) rawParams.get(AUTHORIZED_USER_KEY);
      var authedUser = paramValues.get("value");
      return authedUser.equals(userRequest.getEmail());
    }

    return false;
  }
}
