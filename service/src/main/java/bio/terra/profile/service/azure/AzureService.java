package bio.terra.profile.service.azure;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.model.AzureManagedAppModel;
import bio.terra.profile.service.crl.CrlService;
import com.azure.resourcemanager.managedapplications.models.Application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.azure.resourcemanager.resources.fluent.SubscriptionsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AzureService {
  private static final Logger logger = LoggerFactory.getLogger(AzureService.class);
  private final CrlService crlService;
  private final Map<String, AzureConfiguration.AzureApplicationOffer> azureAppOffers;

  @Autowired
  public AzureService(CrlService crlService, AzureConfiguration azureConfiguration) {
    this.crlService = crlService;
    this.azureAppOffers = azureConfiguration.getApplicationOffers();
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
        .map(app -> new AzureManagedAppModel()
                .deploymentName(app.name())
                .subscriptionId(subscriptionId)
                .managedResourceGroupId(app.managedResourceGroupId())

        )
        .toList();
  }

  private boolean isAuthedTerraManagedApp(AuthenticatedUserRequest userRequest, Application app) {
    if (app.plan() == null) {
      return false;
    }

    var offer = azureAppOffers.get(app.plan().product());
    if (offer == null) {
      return false;
    }

    var authedUserKey = offer.getAuthorizedUserKey();
    if (app.parameters() != null && app.parameters() instanceof HashMap rawParams) {
      if (!rawParams.containsKey(authedUserKey)) {
        logger.warn("Terra app deployment with no authorized user key {} [mrg_id={}]", authedUserKey, app.managedResourceGroupId());
        return false;
      }
      var paramValues = (HashMap) rawParams.get(authedUserKey);
      var authedUser = paramValues.get("value");
      return authedUser.equals(userRequest.getEmail());
    }

    return false;
  }
}
