package bio.terra.profile.service.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import bio.terra.common.exception.UnauthorizedException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.common.BaseSpringUnitTest;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;

class SamAuthenticatedUserRequestFactoryTest extends BaseSpringUnitTest {
  private static final String EMAIL = "billing@unit.com";
  private static final String SUBJECT = "12345";
  private static final String TOKEN = "not-a-real-token";

  private static final UserStatusInfo SAM_ENABLED_USER =
      new UserStatusInfo().userEmail(EMAIL).userSubjectId(SUBJECT).enabled(true);
  private static final UserStatusInfo SAM_DISABLED_USER =
      new UserStatusInfo().userEmail(EMAIL).userSubjectId(SUBJECT).enabled(false);

  @Mock private SamService samService;

  private SamAuthenticatedUserRequestFactory factory;

  @BeforeEach
  public void before() throws InterruptedException {
    factory = new SamAuthenticatedUserRequestFactory(samService);
    when(samService.getUserStatusInfo(eq(TOKEN))).thenReturn(SAM_ENABLED_USER);
  }

  @Test
  void oauth2Token() {
    var request = new MockHttpServletRequest();
    request.addHeader("OAUTH2_CLAIM_access_token", TOKEN);

    var result = factory.from(request);
    assertEquals(
        AuthenticatedUserRequest.builder()
            .setEmail(EMAIL)
            .setSubjectId(SUBJECT)
            .setToken(TOKEN)
            .build(),
        result);
  }

  @Test
  void bearerToken() {
    var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + TOKEN);

    var result = factory.from(request);
    assertEquals(
        AuthenticatedUserRequest.builder()
            .setEmail(EMAIL)
            .setSubjectId(SUBJECT)
            .setToken(TOKEN)
            .build(),
        result);
  }

  @Test
  void noToken() {
    assertThrows(UnauthorizedException.class, () -> factory.from(new MockHttpServletRequest()));
  }

  @Test
  void userDisabled() throws InterruptedException {
    when(samService.getUserStatusInfo(eq(TOKEN))).thenReturn(SAM_DISABLED_USER);

    var request = new MockHttpServletRequest();
    request.addHeader("OAUTH2_CLAIM_access_token", TOKEN);

    assertThrows(UnauthorizedException.class, () -> factory.from(new MockHttpServletRequest()));
  }
}
