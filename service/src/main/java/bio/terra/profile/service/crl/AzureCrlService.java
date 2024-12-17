package bio.terra.profile.service.crl;

import bio.terra.cloudres.common.ClientConfig;
import bio.terra.profile.app.configuration.AzureConfiguration;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.containerservice.ContainerServiceManager;
import com.azure.resourcemanager.costmanagement.CostManagementManager;
import com.azure.resourcemanager.managedapplications.ApplicationManager;
import com.azure.resourcemanager.resources.ResourceManager;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Azure Cloud Resources Library Service */
@Component
public class AzureCrlService {

  private final AzureConfiguration azureConfiguration;
  private final ClientConfig clientConfig;

  @Autowired
  public AzureCrlService(AzureConfiguration azureConfiguration, ClientConfig clientConfig) {
    this.azureConfiguration = azureConfiguration;
    this.clientConfig = clientConfig;
  }

  public ResourceManager getResourceManager(UUID subscriptionId) {
    return getResourceManager(
        UUID.fromString(azureConfiguration.managedAppTenantId()), subscriptionId);
  }

  /** Returns an Azure {@link ResourceManager} configured for use with CRL. */
  public ResourceManager getResourceManager(UUID tenantId, UUID subscriptionId) {
    AzureProfile azureProfile =
        new AzureProfile(tenantId.toString(), subscriptionId.toString(), azureConfiguration.getAzureEnvironment());

    // We must use FQDN because there are two `Defaults` symbols imported otherwise.
    return bio.terra.cloudres.azure.resourcemanager.common.Defaults.crlConfigure(
            clientConfig, ResourceManager.configure())
        .authenticate(azureConfiguration.buildManagedAppCredentials(), azureProfile)
        .withSubscription(subscriptionId.toString());
  }

  public ApplicationManager getApplicationManager(UUID subscriptionId) {
    AzureProfile azureProfile =
        new AzureProfile(null, subscriptionId.toString(), azureConfiguration.getAzureEnvironment());

    return ApplicationManager.authenticate(
        azureConfiguration.buildManagedAppCredentials(), azureProfile);
  }

  public CostManagementManager getCostManagementManager(UUID subscriptionId) {
    AzureProfile azureProfile =
        new AzureProfile(null, subscriptionId.toString(), azureConfiguration.getAzureEnvironment());

    return CostManagementManager.authenticate(
        azureConfiguration.buildManagedAppCredentials(), azureProfile);
  }

  public ContainerServiceManager getContainerServiceManager(UUID subscriptionId) {
    AzureProfile azureProfile =
        new AzureProfile(null, subscriptionId.toString(), azureConfiguration.getAzureEnvironment());

    return ContainerServiceManager.authenticate(
        azureConfiguration.buildManagedAppCredentials(), azureProfile);
  }
}
