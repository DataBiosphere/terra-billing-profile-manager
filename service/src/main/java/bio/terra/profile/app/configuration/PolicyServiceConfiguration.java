package bio.terra.profile.app.configuration;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/** Configuration for managing connection to Terra Policy Service. * */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "profile.policy")
public class PolicyServiceConfiguration {

  private String basePath;
  private String clientCredentialFilePath;

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

  public String getAccessToken() throws IOException {
    try (FileInputStream fileInputStream = new FileInputStream(clientCredentialFilePath)) {
      GoogleCredentials credentials =
          ServiceAccountCredentials.fromStream(fileInputStream)
              .createScoped(POLICY_SERVICE_ACCOUNT_SCOPES);
      AccessToken token = credentials.refreshAccessToken();
      return token.getTokenValue();
    }
  }
}
