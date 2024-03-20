package bio.terra.profile.service.crl;

import bio.terra.cloudres.common.ClientConfig;
import bio.terra.cloudres.google.billing.CloudBillingClientCow;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.service.crl.exception.CrlInternalException;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.text.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Provides GCP Cloud Library resources for the application */
@Component
public class GcpCrlService {

  private final ClientConfig clientConfig;
  private static final String GOOGLE_ACCESS_TOKEN_CLAIM = "idp_access_token";

  @Autowired
  public GcpCrlService(ClientConfig clientConfig) {
    this.clientConfig = clientConfig;
  }

  /** Returns a GCP {@link CloudBillingClientCow} which wraps Google Billing API. */
  public CloudBillingClientCow getBillingClientCow(AuthenticatedUserRequest user) {
    try {
      return new CloudBillingClientCow(
          clientConfig, getUserCredentials(getGoogleAccessToken(user.getToken())));
    } catch (IOException e) {
      throw new CrlInternalException("Error creating billing client wrapper", e);
    }
  }

  private GoogleCredentials getUserCredentials(String token) {
    return GoogleCredentials.newBuilder().setAccessToken(new AccessToken(token, null)).build();
  }

  private String getGoogleAccessToken(String userToken) {
    try {
      var jwt = SignedJWT.parse(userToken);
      return (String) jwt.getJWTClaimsSet().getClaim(GOOGLE_ACCESS_TOKEN_CLAIM);
    } catch (ParseException e) {
      // even if the token is not a valid JWT, it may still be a valid Google access token
      return userToken;
    }
  }
}
