package bio.terra.profile.app.configuration;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "profile.azure")
public record AzureConfiguration(
    String managedAppClientId,
    String managedAppClientSecret,
    String managedAppTenantId,
    Map<String, AzureApplicationOffer> applicationOffers) {
  private static final Logger logger = LoggerFactory.getLogger(AzureConfiguration.class);

  public static class AzureApplicationOffer {
    private String name;
    private String authorizedUserKey;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getAuthorizedUserKey() {
      return authorizedUserKey;
    }

    public void setAuthorizedUserKey(String authorizedUserKey) {
      this.authorizedUserKey = authorizedUserKey;
    }
  }

  public TokenCredential buildManagedAppCredentials() {
    return new ClientSecretCredentialBuilder()
        .clientId(managedAppClientId)
        .clientSecret(managedAppClientSecret)
        .tenantId(managedAppTenantId)
        .build();
  }

  /**
   * Well-known Microsoft Marketplace offers; only deployments of these offers will be allowed
   * to have billing profiles created against them
   */
  public Map<String, AzureApplicationOffer> getApplicationOffers() {
    return applicationOffers;
  }

  public void logOffers() {
    for (String offer : this.applicationOffers.keySet()) {
      logger.info("Azure application offer {}", offer);
    }
  }
}
