package bio.terra.profile.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.PactTestExecutionContext;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.ConsumerPactTest;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.junit.Assert;

import java.io.IOException;


/**
 * This is the one that actually 'works' (is generating a pact)
 */
public class SamStatusApiPactTest extends ConsumerPactTest {


  @Pact(consumer = "bpm-consumer", provider = "sam-provider")
  @Override
  protected RequestResponsePact createPact(PactDslWithProvider builder) {
    return builder
        .given("Sam is ok")
        .uponReceiving("a status request")
        .path("/status").method("GET")
        .willRespondWith().status(200)
        .body("{\"ok\": true}")
        .toPact();
  }

  @Override
  protected String providerName() {
    return "sam-provider";
  }

  @Override
  protected String consumerName() {
    return "bpm-consumer";
  }


  /**
   * This needs to have something that calls the endpoint defined in the pact above.
   */
  @Override
  protected void runTest(MockServer mockServer, PactTestExecutionContext context) throws IOException {
    var statusApi = new StatusApi();
    statusApi.setCustomBaseUrl(mockServer.getUrl());
    try {
      var system = statusApi.getSystemStatus();
      Assert.assertTrue(system.getOk());
      // we could also assert that any subsystems we care about here are present
      // system.getSystems()
    } catch (ApiException e) {
      throw new RuntimeException(e);
    }
  }
}
