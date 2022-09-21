package bio.terra.profile.service.azure;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.model.AzureManagedAppModel;
import com.azure.resourcemanager.managedapplications.models.Application;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AzureService {
  private static final Logger logger = LoggerFactory.getLogger(AzureService.class);

  private final ApplicationService appService;
  private final Set<AzureConfiguration.AzureApplicationOffer> azureAppOffers;

  @Autowired
  public AzureService(ApplicationService appService, AzureConfiguration azureConfiguration) {
    this.appService = appService;
    this.azureAppOffers = azureConfiguration.getApplicationOffers();
  }

  /**
   * Gets the Azure managed applications the user has access to in the given subscription.
   *
   * @param subscriptionId Azure subscription ID that will be checked for managed applications
   * @param userRequest Authorized user request
   * @return List of Terra Azure managed applications the user has access to
   */
  public List<AzureManagedAppModel> getAuthorizedManagedAppDeployments(
      UUID subscriptionId, AuthenticatedUserRequest userRequest) {
    var tenantId = appService.getTenantForSubscription(subscriptionId);

    Stream<Application> applications = appService.getApplicationsForSubscription(subscriptionId);

    return applications
        .filter(app -> isAuthedTerraManagedApp(userRequest, app))
        .map(
            app ->
                new AzureManagedAppModel()
                    .applicationDeploymentName(app.name())
                    .subscriptionId(subscriptionId)
                    .managedResourceGroupId(
                        normalizeManagedResourceGroupId(app.managedResourceGroupId()))
                    .tenantId(tenantId))
        .distinct()
        .toList();
  }

  private String normalizeManagedResourceGroupId(String mrgId) {
    var tokens = mrgId.split("/");
    return tokens[tokens.length - 1];
  }

  private boolean isAuthedTerraManagedApp(AuthenticatedUserRequest userRequest, Application app) {
    if (app.plan() == null) {
      logger.debug(
          "App deployment has no plan, ignoring [mrg_id={}]", app.managedResourceGroupId());
      return false;
    }

    var maybeOffer =
        azureAppOffers.stream().filter(o -> o.getName().equals(app.plan().product())).findFirst();
    if (maybeOffer.isEmpty()) {
      logger.debug(
          "App deployment is not a deployment of a well-known Terra offer, ignoring [mrg_id={}]",
          app.managedResourceGroupId());
      return false;
    }

    var offer = maybeOffer.get();
    var authedUserKey = offer.getAuthorizedUserKey();
    if (app.parameters() != null && app.parameters() instanceof Map rawParams) {
      if (!rawParams.containsKey(authedUserKey)) {
        logger.warn(
            "Terra app deployment with no authorized user key {} [mrg_id={}]",
            authedUserKey,
            app.managedResourceGroupId());
        return false;
      }
      var paramValues = (Map) rawParams.get(authedUserKey);
      var authedUsers = ((String) paramValues.get("value")).split(",");

      return Arrays.stream(authedUsers).anyMatch(user -> user.equals(userRequest.getEmail()));
    }

    return false;
  }
}
