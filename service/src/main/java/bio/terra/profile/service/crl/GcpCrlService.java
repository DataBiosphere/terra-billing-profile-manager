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
      return new CloudBillingClientCow(clientConfig, getUserCredentials(user.getToken()));
    } catch (IOException e) {
      throw new CrlInternalException("Error creating billing client wrapper", e);
    }
  }

  private GoogleCredentials getUserCredentials(String token) {
    return GoogleCredentials.newBuilder()
        .setAccessToken(new AccessToken(getGoogleAccessToken(token), null))
        .build();
  }

  /**
   * The user's token might be a JWT from B2C or a Google access token. If the user signed in to
   * Google through B2C, their JWT will include their Google access token as a claim. Try to parse
   * the token as if it's a JWT and return the Google access token in the claim. If parsing fails,
   * assume the token is a Google access token and return it as is.
   *
   * <p>Note that we do not fully validate the JWT here as requests to BPM go through its proxy
   * which does validate the JWT.
   */
  private String getGoogleAccessToken(String userToken) {
    try {
      var jwt = SignedJWT.parse(userToken);
      return (String) jwt.getJWTClaimsSet().getClaim(GOOGLE_ACCESS_TOKEN_CLAIM);
    } catch (ParseException e) {
      return userToken;
    }
  }
}
