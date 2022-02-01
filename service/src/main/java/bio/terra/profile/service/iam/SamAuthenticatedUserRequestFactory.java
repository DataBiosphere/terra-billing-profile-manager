package bio.terra.profile.service.iam;

import bio.terra.common.exception.UnauthorizedException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.iam.AuthenticatedUserRequestFactory;
import bio.terra.common.iam.BearerTokenParser;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An {@link AuthenticatedUserRequestFactory} which always resolves the user email and subjectId
 * from Sam given a request token.
 *
 * <p>This is important for calls made by pet service accounts, which will have a pet email in the
 * request header, but Sam will return the owner's email.
 */
@Component
public class SamAuthenticatedUserRequestFactory implements AuthenticatedUserRequestFactory {
  private static final String OAUTH2_ACCESS_TOKEN = "OAUTH2_CLAIM_access_token";
  private static final String AUTHORIZATION = "Authorization";
  private final SamService samService;

  @Autowired
  public SamAuthenticatedUserRequestFactory(SamService samService) {
    this.samService = samService;
  }

  @Override
  public AuthenticatedUserRequest from(HttpServletRequest servletRequest) {
    final var token = getRequiredToken(servletRequest);

    // Fetch the user status from Sam
    var userStatusInfo =
        SamRethrow.onInterrupted(() -> samService.getUserStatusInfo(token), "getUserStatusInfo");

    return AuthenticatedUserRequest.builder()
        .setToken(token)
        .setEmail(userStatusInfo.getUserEmail())
        .setSubjectId(userStatusInfo.getUserSubjectId())
        .build();
  }

  /**
   * Gets the user token from OAuth2 claim or Authorization header. Throws UnauthorizedException if
   * the token could not be found.
   */
  private String getRequiredToken(HttpServletRequest servletRequest) {
    String oauth2Header = servletRequest.getHeader(OAUTH2_ACCESS_TOKEN);
    if (oauth2Header != null) {
      return oauth2Header;
    } else {
      String authHeader = servletRequest.getHeader(AUTHORIZATION);
      if (authHeader != null) {
        return BearerTokenParser.parse(authHeader);
      }
    }
    throw new UnauthorizedException("Unable to retrieve access token");
  }
}
