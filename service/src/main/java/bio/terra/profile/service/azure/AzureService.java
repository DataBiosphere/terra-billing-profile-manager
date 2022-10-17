package bio.terra.profile.service.azure;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.AzureManagedAppModel;
import bio.terra.profile.service.crl.CrlService;
import com.azure.resourcemanager.managedapplications.models.Application;
import com.azure.resourcemanager.resources.models.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AzureService {

  private static final Logger logger = LoggerFactory.getLogger(AzureService.class);

  private final ApplicationService appService;
  private final CrlService crlService;
  private final Set<AzureConfiguration.AzureApplicationOffer> azureAppOffers;
  private final ProfileDao profileDao;

  @Autowired
  public AzureService(
      CrlService crlService,
      ApplicationService appService,
      AzureConfiguration azureConfiguration,
      ProfileDao profileDao) {
    this.crlService = crlService;
    this.appService = appService;
    this.azureAppOffers = azureConfiguration.getApplicationOffers();
    this.profileDao = profileDao;
  }

  /**
   * Gets the Azure managed applications the user has access to in the given subscription.
   *
   * @param subscriptionId Azure subscription ID that will be checked for managed applications
   * @param userRequest Authorized user request
   * @return List of Terra Azure managed applications the user has access to
   */
  public List<AzureManagedAppModel> getAuthorizedManagedAppDeployments(
      UUID subscriptionId,
      Boolean includeAssignedApplications,
      AuthenticatedUserRequest userRequest) {
    var tenantId = appService.getTenantForSubscription(subscriptionId);

    Stream<Application> applications = appService.getApplicationsForSubscription(subscriptionId);

    List<String> assignedManagedResourceGroups;
    if (includeAssignedApplications) {
      assignedManagedResourceGroups = Collections.emptyList();
    } else {
      assignedManagedResourceGroups =
          profileDao.listManagedResourceGroupsInSubscription(subscriptionId);
    }

    return applications
        .filter(app -> isAuthedTerraManagedApp(userRequest, app))
        .filter(
            app ->
                !assignedManagedResourceGroups.contains(
                    normalizeManagedResourceGroupId(app.managedResourceGroupId())))
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

  /**
   * Gets the resource provider namespaces in a subscription that are in either the "Registered" or
   * "Registering" state.
   *
   * @param tenantId Azure tenant ID associated with the given subscription.
   * @param subscriptionId Azure subscription ID to be checked for providers
   * @return Set of registered or registering resource providers namespaces.
   */
  public Set<String> getRegisteredProviderNamespacesForSubscription(
      UUID tenantId, UUID subscriptionId) {
    var resourceManager = crlService.getResourceManager(tenantId, subscriptionId);
    var providers = resourceManager.providers();
    return providers.list().stream()
        .filter(
            provider ->
                provider.registrationState().equals("Registered")
                    || provider.registrationState().equals("Registering"))
        .map(Provider::namespace)
        .collect(Collectors.toSet());
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
        azureAppOffers.stream()
            .filter(
                o ->
                    o.getName().equals(app.plan().product())
                        && o.getPublisher().equals(app.plan().publisher()))
            .findFirst();
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
