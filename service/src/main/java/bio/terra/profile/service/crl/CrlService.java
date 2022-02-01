package bio.terra.profile.service.crl;

import bio.terra.cloudres.common.ClientConfig;
import bio.terra.cloudres.google.billing.CloudBillingClientCow;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.service.crl.exception.CrlInternalException;
import bio.terra.profile.service.crl.exception.CrlSecurityException;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.resources.ResourceManager;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CrlService {
  /** The client name required by CRL. */
  private static final String CLIENT_NAME = "profile";

  private final AzureConfiguration azureConfiguration;
  private final ClientConfig clientConfig;
  private final CloudBillingClientCow crlBillingClientCow;

  @Autowired
  public CrlService(AzureConfiguration azureConfiguration) {
    this.azureConfiguration = azureConfiguration;
    var creds = getApplicationCredentials();
    this.clientConfig = buildClientConfig();
    try {
      this.crlBillingClientCow = new CloudBillingClientCow(clientConfig, creds);
    } catch (IOException e) {
      throw new CrlInternalException("Error creating billing client wrapper", e);
    }
  }

  /** Returns the CRL {@link CloudBillingClientCow} which wraps Google Billing API. */
  public CloudBillingClientCow getCloudBillingClientCow() {
    return crlBillingClientCow;
  }

  /** Returns an Azure {@link ResourceManager} configured for use with CRL. */
  public ResourceManager getResourceManager(UUID tenantId, UUID subscriptionId) {
    AzureProfile azureProfile =
        new AzureProfile(tenantId.toString(), subscriptionId.toString(), AzureEnvironment.AZURE);

    // We must use FQDN because there are two `Defaults` symbols imported otherwise.
    return bio.terra.cloudres.azure.resourcemanager.common.Defaults.crlConfigure(
            clientConfig, ResourceManager.configure())
        .authenticate(azureConfiguration.buildManagedAppCredentials(), azureProfile)
        .withSubscription(subscriptionId.toString());
  }

  private ClientConfig buildClientConfig() {
    // Billing profile manager does not create any cloud resources, so no need to use Janitor.
    return ClientConfig.Builder.newBuilder().setClient(CLIENT_NAME).build();
  }

  private GoogleCredentials getApplicationCredentials() {
    try {
      return GoogleCredentials.getApplicationDefault();
    } catch (IOException e) {
      throw new CrlSecurityException("Failed to get credentials", e);
    }
  }
}
