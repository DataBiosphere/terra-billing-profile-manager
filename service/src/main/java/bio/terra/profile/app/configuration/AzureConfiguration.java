package bio.terra.profile.app.configuration;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.*;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "profile.azure")
public record AzureConfiguration(
    String azureEnvironment,
    String managedAppClientId,
    String managedAppClientSecret,
    String managedAppTenantId,
    String managedAppWorkloadClientId,
    Boolean controlPlaneEnabled,
    String authTokenScope,
    Set<AzureApplicationOffer> applicationOffers,
    Set<String> requiredProviders) {
  private static final Logger logger = LoggerFactory.getLogger(AzureConfiguration.class);

  /**
   * Represents a pre-configured Terra Microsoft Marketplace Offer
   *
   * <p>Terra marketplace offers are expected to possess an authorized user key. This key will be
   * used during billing profile creation to ensure the creating user has the proper authorization
   * to link a Terra billing with an Azure managed app deployment.
   *
   * @see <a href=https://docs.microsoft.com/en-us/azure/marketplace/determine-your-listing-type/>
   */
  public static class AzureApplicationOffer {
    private String name;
    private String publisher;
    private String authorizedUserKey;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getPublisher() {
      return publisher;
    }

    public void setPublisher(String publisher) {
      this.publisher = publisher;
    }

    /**
     * Key that determines which parameter on the managed application deployment BPM will check when
     * determining whether a user has authorization to provision a billing profile against said MRG.
     *
     * <p>The value of the key will be a comma-separated list of email addresses
     */
    public String getAuthorizedUserKey() {
      return authorizedUserKey;
    }

    public void setAuthorizedUserKey(String authorizedUserKey) {
      this.authorizedUserKey = authorizedUserKey;
    }
  }

  public TokenCredential buildManagedAppCredentials() {

    ManagedIdentityCredential managedIdentityCredential =
        new ManagedIdentityCredentialBuilder().clientId(managedAppWorkloadClientId).build();

    ClientSecretCredential servicePrincipalCredential =
        new ClientSecretCredentialBuilder()
            .authorityHost(getAzureEnvironment().getActiveDirectoryEndpoint())
            .clientId(managedAppClientId)
            .clientSecret(managedAppClientSecret)
            .tenantId(managedAppTenantId)
            .build();

    // when an access token is requested, the chain will try each
    // credential in order, stopping when one provides a token

    // Managed identity can't be used to authenticate locally running applications.
    // BPM must be deployed to an Azure service that supports Managed Identity,
    // otherwise we'll fall through to ServicePrincipal auth
    return new ChainedTokenCredentialBuilder()
        .addLast(managedIdentityCredential)
        .addLast(servicePrincipalCredential)
        .build();
  }

  /**
   * Well-known Microsoft Marketplace offers; only deployments of these offers will be allowed to
   * have billing profiles created against them
   */
  public Set<AzureApplicationOffer> getApplicationOffers() {
    return applicationOffers;
  }

  public void logOffers() {
    for (AzureApplicationOffer offer : this.applicationOffers()) {
      logger.info("Azure application offer {}", offer.name);
    }
  }

  public Set<String> getRequiredProviders() {
    return requiredProviders;
  }

  public Boolean getControlPlaneEnabled() {
    return controlPlaneEnabled;
  }

  public AzureEnvironment getAzureEnvironment() {
    switch (azureEnvironment) {
      case "AZURE":
        return AzureEnvironment.AZURE;
      case "AZURE_GOV":
        return AzureEnvironment.AZURE_US_GOVERNMENT;
      default:
        throw new IllegalArgumentException(
            String.format("Unknown Azure environment: %s", azureEnvironment));
    }
  }
}
