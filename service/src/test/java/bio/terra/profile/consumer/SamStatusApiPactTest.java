package bio.terra.profile.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import bio.terra.profile.common.BaseUnitTest;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("pact-test")
@ExtendWith(PactConsumerTestExt.class)
public class SamStatusApiPactTest extends BaseUnitTest {

  @Pact(consumer = "bpm-consumer", provider = "sam-provider")
  public RequestResponsePact createPact(PactDslWithProvider builder) {
    return builder
        .given("Sam is ok")
        .uponReceiving("a status request")
        .path("/status").method("GET")
        .willRespondWith().status(200)
        .body("{\"ok\": true}")
        .toPact();
  }

  @Test
  public void testStatusApi(MockServer mockServer) throws Exception {
    var statusApi = new StatusApi();
    statusApi.setCustomBaseUrl(mockServer.getUrl());
    var system = statusApi.getSystemStatus();
    assertTrue(system.getOk());
    // we could also assert that any subsystems we care about here are present
    // system.getSystems()
  }
}
