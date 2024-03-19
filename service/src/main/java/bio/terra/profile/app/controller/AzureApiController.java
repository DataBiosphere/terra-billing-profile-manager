package bio.terra.profile.app.controller;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.iam.AuthenticatedUserRequestFactory;
import bio.terra.profile.api.AzureApi;
import bio.terra.profile.app.configuration.PolicyServiceConfiguration;
import bio.terra.profile.model.AzureManagedAppModel;
import bio.terra.profile.model.AzureManagedAppsResponseModel;
import bio.terra.profile.service.azure.AzureService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class AzureApiController implements AzureApi {

  private final HttpServletRequest request;
  private final AzureService azureService;
  private final AuthenticatedUserRequestFactory authenticatedUserRequestFactory;
  private final PolicyServiceConfiguration policyServiceConfiguration;

  @Autowired
  public AzureApiController(
      HttpServletRequest request,
      AzureService azureService,
      AuthenticatedUserRequestFactory authenticatedUserRequestFactory,
      PolicyServiceConfiguration policyServiceConfiguration) {
    this.request = request;
    this.azureService = azureService;
    this.authenticatedUserRequestFactory = authenticatedUserRequestFactory;
    this.policyServiceConfiguration = policyServiceConfiguration;
  }

  @Override
  public ResponseEntity<AzureManagedAppsResponseModel> getManagedAppDeployments(
      UUID azureSubscriptionId, Boolean includeAssignedApplications) {
    final AuthenticatedUserRequest userRequest = authenticatedUserRequestFactory.from(request);
    List<AzureManagedAppModel> managedApps;

    // check azure control plane feature flag
    if (this.policyServiceConfiguration.getAzureControlPlaneEnabled()) {
      // get ServiceCatalog deployed managed apps
      managedApps =
          this.azureService.getServiceCatalogManagedAppDeployments(
              azureSubscriptionId, includeAssignedApplications, userRequest);
    } else {
      // get Marketplace deployed managed apps
      managedApps =
          this.azureService.getAuthorizedManagedAppDeployments(
              azureSubscriptionId, includeAssignedApplications, userRequest);
    }

    return new ResponseEntity<>(
        new AzureManagedAppsResponseModel().managedApps(managedApps), HttpStatus.OK);
  }
}
