package bio.terra.profile.app.configuration;

import com.google.api.services.compute.ComputeScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "profile.gcp")
public class GcpConfiguration {
  private String saCredentialFilePath;

  public void setSaCredentialFilePath(String clientCredentialFilePath) {
    this.saCredentialFilePath = clientCredentialFilePath;
  }

  public String getSaCredentialFilePath() {
    return saCredentialFilePath;
  }

  public GoogleCredentials getSaCredentials() throws IOException {
    try (FileInputStream fileInputStream = new FileInputStream(saCredentialFilePath)) {
      // need this broad scope to create/manage projects (same one that is used in Rawls)
      final String manageProjectScope = ComputeScopes.CLOUD_PLATFORM;
      return ServiceAccountCredentials.fromStream(fileInputStream).createScoped(manageProjectScope);
    }
  }
}
