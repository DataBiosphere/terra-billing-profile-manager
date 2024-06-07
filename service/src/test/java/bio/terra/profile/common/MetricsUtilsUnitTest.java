package bio.terra.profile.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.profile.app.common.MetricUtils;
import bio.terra.profile.model.CloudPlatform;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// IGNORE
class MetricsUtilsUnitTest extends BaseUnitTest {

  private SimpleMeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    Metrics.globalRegistry.add(meterRegistry);
  }

  @AfterEach
  void tearDown() {
    meterRegistry.clear();
    Metrics.globalRegistry.clear();
  }

  @Test
  void createGcpProfileMetrics() {
    var profile = ProfileFixtures.createGcpBillingProfileDescription("ABCD1234");

    MetricUtils.recordProfileCreation(() -> profile, profile.billingProfile().cloudPlatform());

    var timer = meterRegistry.find("bpm.profile.creation.time").timer();
    assertNotNull(timer);
    assertEquals(1, timer.count());
    assertEquals(CloudPlatform.GCP.toString(), timer.getId().getTag("cloudPlatform"));
  }

  @Test
  void createAzureProfileMetrics() {
    var profile =
        ProfileFixtures.createAzureBillingProfileDescription(
            UUID.randomUUID(), UUID.randomUUID(), "fake-mrg");

    MetricUtils.recordProfileCreation(() -> profile, profile.billingProfile().cloudPlatform());

    var timer = meterRegistry.find("bpm.profile.creation.time").timer();
    assertNotNull(timer);
    assertEquals(1, timer.count());
    assertEquals(CloudPlatform.AZURE.toString(), timer.getId().getTag("cloudPlatform"));
  }
}
