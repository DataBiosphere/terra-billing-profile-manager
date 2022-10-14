package bio.terra.profile.app.controller;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.iam.AuthenticatedUserRequestFactory;
import bio.terra.profile.api.AzureApi;
import bio.terra.profile.model.AzureManagedAppsResponseModel;
import bio.terra.profile.service.azure.AzureService;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class AzureApiController implements AzureApi {

  private final HttpServletRequest request;
  private final AzureService azureService;
  private final AuthenticatedUserRequestFactory authenticatedUserRequestFactory;

  @Autowired
  public AzureApiController(
      HttpServletRequest request,
      AzureService azureService,
      AuthenticatedUserRequestFactory authenticatedUserRequestFactory) {
    this.request = request;
    this.azureService = azureService;
    this.authenticatedUserRequestFactory = authenticatedUserRequestFactory;
  }

  @Override
  public ResponseEntity<AzureManagedAppsResponseModel> getManagedAppDeployments(
      UUID azureSubscriptionId, Boolean includeAssignedApplications) {
    final AuthenticatedUserRequest userRequest = authenticatedUserRequestFactory.from(request);
    var managedApps =
        this.azureService.getAuthorizedManagedAppDeployments(
            azureSubscriptionId, includeAssignedApplications, userRequest);

    return new ResponseEntity<>(
        new AzureManagedAppsResponseModel().managedApps(managedApps), HttpStatus.OK);
  }
}
