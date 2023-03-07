package bio.terra.profile.common;

import bio.terra.common.iam.AuthenticatedUserRequest;

public class AuthRequestFixtures {
  private static final String AUTH_USER_EMAIL = "user@app.terra.bio";
  private static final String AUTH_SUBJECT_ID = "subjectId";
  private static final String AUTH_TOKEN = "token";

  private AuthRequestFixtures() {}

  public static AuthenticatedUserRequest buildAuthRequest() {
    return AuthenticatedUserRequest.builder()
        .setEmail(AUTH_USER_EMAIL)
        .setSubjectId(AUTH_SUBJECT_ID)
        .setToken(AUTH_TOKEN)
        .build();
  }
}
