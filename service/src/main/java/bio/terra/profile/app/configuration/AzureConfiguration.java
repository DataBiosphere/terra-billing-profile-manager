package bio.terra.profile.app.configuration;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "profile.azure")
public record AzureConfiguration(
    String managedAppClientId, String managedAppClientSecret, String managedAppTenantId) {

  public TokenCredential buildManagedAppCredentials() {
    return new ClientSecretCredentialBuilder()
        .clientId(managedAppClientId)
        .clientSecret(managedAppClientSecret)
        .tenantId(managedAppTenantId)
        .build();
  }
}
