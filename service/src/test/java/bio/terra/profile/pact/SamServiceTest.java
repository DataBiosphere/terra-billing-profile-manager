package bio.terra.profile.pact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import bio.terra.profile.app.configuration.SamConfiguration;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.service.iam.SamService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Tag("pact-test")
@ExtendWith(PactConsumerTestExt.class)
public class SamServiceTest extends BaseUnitTest {

  @Pact(consumer = "bpm-consumer", provider = "sam-provider")
  public RequestResponsePact statusApiPact(PactDslWithProvider builder) {
    return builder
        .given("Sam is ok")
        .uponReceiving("a status request")
        .path("/status")
        .method("GET")
        .willRespondWith()
        .status(200)
        .body("{\"ok\": true}")
        .toPact();
  }

  @Pact(consumer = "bpm-consumer", provider = "sam-provider")
  public RequestResponsePact userStatusPact(PactDslWithProvider builder) {
    return builder
        .given("user exists")
        .uponReceiving("a request for the user's status")
        .path("/register/user/v2/self/info")
        .method("GET")
        .headers("Authorization", "Bearer accessToken")
        .willRespondWith()
        .status(200)
        .body(
            "{\n"
                + "  \"userSubjectId\": \"userSubjectId\",\n"
                + "  \"userEmail\": \"userEmail\",\n"
                + "  \"enabled\": true\n"
                + "}")
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "statusApiPact")
  public void testSamServiceStatusCheck(MockServer mockServer) {
    SamConfiguration config = new SamConfiguration(mockServer.getUrl(), "test@test.com");
    var samService = new SamService(config);
    var system = samService.status();
    assertTrue(system.isOk());

    // we could also assert that any subsystems we care about here are present
    // but then we'd need to add them to the pact above
    // system.getSystems()
  }

  @Test
  @PactTestFor(pactMethod = "userStatusPact")
  public void testSamServiceUserStatusInfo(MockServer mockServer) throws Exception {
    SamConfiguration config = new SamConfiguration(mockServer.getUrl(), "test@test.com");
    var samService = new SamService(config);
    var userStatus = samService.getUserStatusInfo("accessToken");
    assertEquals("userEmail", userStatus.getUserEmail());
    assertEquals("userSubjectId", userStatus.getUserSubjectId());
    assertTrue(userStatus.getEnabled());
  }
}
