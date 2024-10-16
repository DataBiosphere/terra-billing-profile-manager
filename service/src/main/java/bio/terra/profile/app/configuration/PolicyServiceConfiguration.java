package bio.terra.profile.app.configuration;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Configuration for managing connection to Terra Policy Service. * */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "profile.policy")
public class PolicyServiceConfiguration {

  private String basePath;
  private String clientCredentialFilePath;
  private final AzureConfiguration azureConfiguration;

  private static final List<String> POLICY_SERVICE_ACCOUNT_SCOPES =
      List.of("openid", "email", "profile");

  public String getBasePath() {
    return basePath;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public void setClientCredentialFilePath(String clientCredentialFilePath) {
    this.clientCredentialFilePath = clientCredentialFilePath;
  }

  public String getClientCredentialFilePath() {
    return clientCredentialFilePath;
  }

  @Autowired
  PolicyServiceConfiguration(AzureConfiguration azureConfiguration) {
    this.azureConfiguration = azureConfiguration;
  }

  public String getAccessToken() throws IOException {
    if (azureConfiguration.controlPlaneEnabled()) {
      TokenCredential credential = new DefaultAzureCredentialBuilder()
              .authorityHost(azureConfiguration.getAzureEnvironment().getActiveDirectoryEndpoint())
              .build();
      // The Microsoft Authentication Library (MSAL) currently specifies offline_access, openid,
      // profile, and email by default in authorization and token requests.
      com.azure.core.credential.AccessToken token =
          credential
              .getToken(new TokenRequestContext().addScopes(azureConfiguration.authTokenScope()))
              .block();
      return token.getToken();
    } else {
      try (FileInputStream fileInputStream = new FileInputStream(clientCredentialFilePath)) {
        GoogleCredentials credentials =
            ServiceAccountCredentials.fromStream(fileInputStream)
                .createScoped(POLICY_SERVICE_ACCOUNT_SCOPES);
        AccessToken token = credentials.refreshAccessToken();
        return token.getTokenValue();
      }
    }
  }
}
