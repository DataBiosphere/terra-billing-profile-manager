package bio.terra.profile.service.crl;

import bio.terra.cloudres.common.ClientConfig;
import bio.terra.cloudres.google.billing.CloudBillingClientCow;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.service.crl.exception.CrlInternalException;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Provides GCP Cloud Library resources for the application */
@Component
public class GcpCrlService {

  private final ClientConfig clientConfig;

  @Autowired
  public GcpCrlService(ClientConfig clientConfig) {
    this.clientConfig = clientConfig;
  }

  /** Returns a GCP {@link CloudBillingClientCow} which wraps Google Billing API. */
  public CloudBillingClientCow getBillingClientCow(AuthenticatedUserRequest user) {
    try {
      return new CloudBillingClientCow(clientConfig, getUserCredentials(user.getToken()));
    } catch (IOException e) {
      throw new CrlInternalException("Error creating billing client wrapper", e);
    }
  }

  private GoogleCredentials getUserCredentials(String token) {
    return GoogleCredentials.newBuilder().setAccessToken(new AccessToken(token, null)).build();
  }
}